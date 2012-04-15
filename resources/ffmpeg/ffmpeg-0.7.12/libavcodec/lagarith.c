/*
 * Lagarith lossless decoder
 * Copyright (c) 2009 Nathan Caldwell <saintdev (at) gmail.com>
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

/**
 * @file
 * Lagarith lossless decoder
 * @author Nathan Caldwell
 */

#include "avcodec.h"
#include "get_bits.h"
#include "mathops.h"
#include "dsputil.h"
#include "lagarithrac.h"

enum LagarithFrameType {
    FRAME_RAW           = 1,    /*!< uncompressed */
    FRAME_U_RGB24       = 2,    /*!< unaligned RGB24 */
    FRAME_ARITH_YUY2    = 3,    /*!< arithmetic coded YUY2 */
    FRAME_ARITH_RGB24   = 4,    /*!< arithmetic coded RGB24 */
    FRAME_SOLID_GRAY    = 5,    /*!< solid grayscale color frame */
    FRAME_SOLID_COLOR   = 6,    /*!< solid non-grayscale color frame */
    FRAME_OLD_ARITH_RGB = 7,    /*!< obsolete arithmetic coded RGB (no longer encoded by upstream since version 1.1.0) */
    FRAME_ARITH_RGBA    = 8,    /*!< arithmetic coded RGBA */
    FRAME_SOLID_RGBA    = 9,    /*!< solid RGBA color frame */
    FRAME_ARITH_YV12    = 10,   /*!< arithmetic coded YV12 */
    FRAME_REDUCED_RES   = 11,   /*!< reduced resolution YV12 frame */
};

typedef struct LagarithContext {
    AVCodecContext *avctx;
    AVFrame picture;
    DSPContext dsp;
    int zeros;                  /*!< number of consecutive zero bytes encountered */
    int zeros_rem;              /*!< number of zero bytes remaining to output */
} LagarithContext;

/**
 * Compute the 52bit mantissa of 1/(double)denom.
 * This crazy format uses floats in an entropy coder and we have to match x86
 * rounding exactly, thus ordinary floats aren't portable enough.
 * @param denom denominator
 * @return 52bit mantissa
 * @see softfloat_mul
 */
static uint64_t softfloat_reciprocal(uint32_t denom)
{
    int shift = av_log2(denom - 1) + 1;
    uint64_t ret = (1ULL << 52) / denom;
    uint64_t err = (1ULL << 52) - ret * denom;
    ret <<= shift;
    err <<= shift;
    err +=  denom / 2;
    return ret + err / denom;
}

/**
 * (uint32_t)(x*f), where f has the given mantissa, and exponent 0
 * Used in combination with softfloat_reciprocal computes x/(double)denom.
 * @param x 32bit integer factor
 * @param mantissa mantissa of f with exponent 0
 * @return 32bit integer value (x*f)
 * @see softfloat_reciprocal
 */
static uint32_t softfloat_mul(uint32_t x, uint64_t mantissa)
{
    uint64_t l = x * (mantissa & 0xffffffff);
    uint64_t h = x * (mantissa >> 32);
    h += l >> 32;
    l &= 0xffffffff;
    l += 1 << av_log2(h >> 21);
    h += l >> 32;
    return h >> 20;
}

static uint8_t lag_calc_zero_run(int8_t x)
{
    return (x << 1) ^ (x >> 7);
}

static int lag_decode_prob(GetBitContext *gb, uint32_t *value)
{
    static const uint8_t series[] = { 1, 2, 3, 5, 8, 13, 21 };
    int i;
    int bit     = 0;
    int bits    = 0;
    int prevbit = 0;
    unsigned val;

    for (i = 0; i < 7; i++) {
        if (prevbit && bit)
            break;
        prevbit = bit;
        bit = get_bits1(gb);
        if (bit && !prevbit)
            bits += series[i];
    }
    bits--;
    if (bits < 0 || bits > 31) {
        *value = 0;
        return -1;
    } else if (bits == 0) {
        *value = 0;
        return 0;
    }

    val  = get_bits_long(gb, bits);
    val |= 1 << bits;

    *value = val - 1;

    return 0;
}

static int lag_read_prob_header(lag_rac *rac, GetBitContext *gb)
{
    int i, j, scale_factor;
    unsigned prob, cumulative_target;
    unsigned cumul_prob = 0;
    unsigned scaled_cumul_prob = 0;

    rac->prob[0] = 0;
    rac->prob[257] = UINT_MAX;
    /* Read probabilities from bitstream */
    for (i = 1; i < 257; i++) {
        if (lag_decode_prob(gb, &rac->prob[i]) < 0) {
            av_log(rac->avctx, AV_LOG_ERROR, "Invalid probability encountered.\n");
            return -1;
        }
        if ((uint64_t)cumul_prob + rac->prob[i] > UINT_MAX) {
            av_log(rac->avctx, AV_LOG_ERROR, "Integer overflow encountered in cumulative probability calculation.\n");
            return -1;
        }
        cumul_prob += rac->prob[i];
        if (!rac->prob[i]) {
            if (lag_decode_prob(gb, &prob)) {
                av_log(rac->avctx, AV_LOG_ERROR, "Invalid probability run encountered.\n");
                return -1;
            }
            if (prob > 257 - i)
                prob = 257 - i;
            for (j = 0; j < prob; j++)
                rac->prob[++i] = 0;
        }
    }

    if (!cumul_prob) {
        av_log(rac->avctx, AV_LOG_ERROR, "All probabilities are 0!\n");
        return -1;
    }

    /* Scale probabilities so cumulative probability is an even power of 2. */
    scale_factor = av_log2(cumul_prob);

    if (cumul_prob & (cumul_prob - 1)) {
        uint64_t mul = softfloat_reciprocal(cumul_prob);
        for (i = 1; i < 257; i++) {
            rac->prob[i] = softfloat_mul(rac->prob[i], mul);
            scaled_cumul_prob += rac->prob[i];
        }

        scale_factor++;
        cumulative_target = 1 << scale_factor;

        if (scaled_cumul_prob > cumulative_target) {
            av_log(rac->avctx, AV_LOG_ERROR,
                   "Scaled probabilities are larger than target!\n");
            return -1;
        }

        scaled_cumul_prob = cumulative_target - scaled_cumul_prob;

        for (i = 1; scaled_cumul_prob; i = (i & 0x7f) + 1) {
            if (rac->prob[i]) {
                rac->prob[i]++;
                scaled_cumul_prob--;
            }
            /* Comment from reference source:
             * if (b & 0x80 == 0) {     // order of operations is 'wrong'; it has been left this way
             *                          // since the compression change is negligable and fixing it
             *                          // breaks backwards compatibilty
             *      b =- (signed int)b;
             *      b &= 0xFF;
             * } else {
             *      b++;
             *      b &= 0x7f;
             * }
             */
        }
    }

    rac->scale = scale_factor;

    /* Fill probability array with cumulative probability for each symbol. */
    for (i = 1; i < 257; i++)
        rac->prob[i] += rac->prob[i - 1];

    return 0;
}

static void add_lag_median_prediction(uint8_t *dst, uint8_t *src1,
                                      uint8_t *diff, int w, int *left,
                                      int *left_top)
{
    /* This is almost identical to add_hfyu_median_prediction in dsputil.h.
     * However the &0xFF on the gradient predictor yealds incorrect output
     * for lagarith.
     */
    int i;
    uint8_t l, lt;

    l  = *left;
    lt = *left_top;

    for (i = 0; i < w; i++) {
        l = mid_pred(l, src1[i], l + src1[i] - lt) + diff[i];
        lt = src1[i];
        dst[i] = l;
    }

    *left     = l;
    *left_top = lt;
}

static void lag_pred_line(LagarithContext *l, uint8_t *buf,
                          int width, int stride, int line)
{
    int L, TL;

    if (!line) {
        /* Left prediction only for first line */
        L = l->dsp.add_hfyu_left_prediction(buf + 1, buf + 1,
                                            width - 1, buf[0]);
        return;
    } else if (line == 1) {
        /* Second line, left predict first pixel, the rest of the line is median predicted */
        /* FIXME: In the case of RGB this pixel is top predicted */
        TL = buf[-stride];
    } else {
        /* Top left is 2 rows back, last pixel */
        TL = buf[width - (2 * stride) - 1];
    }
    /* Left pixel is actually prev_row[width] */
    L = buf[width - stride - 1];

    add_lag_median_prediction(buf, buf - stride, buf,
                              width, &L, &TL);
}

static int lag_decode_line(LagarithContext *l, lag_rac *rac,
                           uint8_t *dst, int width, int stride,
                           int esc_count)
{
    int i = 0;
    int ret = 0;

    if (!esc_count)
        esc_count = -1;

    /* Output any zeros remaining from the previous run */
handle_zeros:
    if (l->zeros_rem) {
        int count = FFMIN(l->zeros_rem, width - i);
        memset(dst + i, 0, count);
        i += count;
        l->zeros_rem -= count;
    }

    while (i < width) {
        dst[i] = lag_get_rac(rac);
        ret++;

        if (dst[i])
            l->zeros = 0;
        else
            l->zeros++;

        i++;
        if (l->zeros == esc_count) {
            int index = lag_get_rac(rac);
            ret++;

            l->zeros = 0;

            l->zeros_rem = lag_calc_zero_run(index);
            goto handle_zeros;
        }
    }
    return ret;
}

static int lag_decode_zero_run_line(LagarithContext *l, uint8_t *dst,
                                    const uint8_t *src, int width,
                                    int esc_count)
{
    int i = 0;
    int count;
    uint8_t zero_run = 0;
    const uint8_t *start = src;
    uint8_t mask1 = -(esc_count < 2);
    uint8_t mask2 = -(esc_count < 3);
    uint8_t *end = dst + (width - 2);

output_zeros:
    if (l->zeros_rem) {
        count = FFMIN(l->zeros_rem, width - i);
        memset(dst, 0, count);
        l->zeros_rem -= count;
        dst += count;
    }

    while (dst < end) {
        i = 0;
        while (!zero_run && dst + i < end) {
            i++;
            zero_run =
                !(src[i] | (src[i + 1] & mask1) | (src[i + 2] & mask2));
        }
        if (zero_run) {
            zero_run = 0;
            i += esc_count;
            memcpy(dst, src, i);
            dst += i;
            l->zeros_rem = lag_calc_zero_run(src[i]);

            src += i + 1;
            goto output_zeros;
        } else {
            memcpy(dst, src, i);
            src += i;
        }
    }
    return start - src;
}



static int lag_decode_arith_plane(LagarithContext *l, uint8_t *dst,
                                  int width, int height, int stride,
                                  const uint8_t *src, int src_size)
{
    int i = 0;
    int read = 0;
    uint32_t length;
    uint32_t offset = 1;
    int esc_count = src[0];
    GetBitContext gb;
    lag_rac rac;

    rac.avctx = l->avctx;
    l->zeros = 0;

    if (esc_count < 4) {
        length = width * height;
        if (esc_count && AV_RL32(src + 1) < length) {
            length = AV_RL32(src + 1);
            offset += 4;
        }

        init_get_bits(&gb, src + offset, src_size * 8);

        if (lag_read_prob_header(&rac, &gb) < 0)
            return -1;

        lag_rac_init(&rac, &gb, length - stride);

        for (i = 0; i < height; i++)
            read += lag_decode_line(l, &rac, dst + (i * stride), width,
                                    stride, esc_count);

        if (read > length)
            av_log(l->avctx, AV_LOG_WARNING,
                   "Output more bytes than length (%d of %d)\n", read,
                   length);
    } else if (esc_count < 8) {
        esc_count -= 4;
        if (esc_count > 0) {
            /* Zero run coding only, no range coding. */
            for (i = 0; i < height; i++)
                src += lag_decode_zero_run_line(l, dst + (i * stride), src,
                                                width, esc_count);
        } else {
            /* Plane is stored uncompressed */
            for (i = 0; i < height; i++) {
                memcpy(dst + (i * stride), src, width);
                src += width;
            }
        }
    } else if (esc_count == 0xff) {
        /* Plane is a solid run of given value */
        for (i = 0; i < height; i++)
            memset(dst + i * stride, src[1], width);
        /* Do not apply prediction.
           Note: memset to 0 above, setting first value to src[1]
           and applying prediction gives the same result. */
        return 0;
    } else {
        av_log(l->avctx, AV_LOG_ERROR,
               "Invalid zero run escape code! (%#x)\n", esc_count);
        return -1;
    }

    for (i = 0; i < height; i++) {
        lag_pred_line(l, dst, width, stride, i);
        dst += stride;
    }

    return 0;
}

/**
 * Decode a frame.
 * @param avctx codec context
 * @param data output AVFrame
 * @param data_size size of output data or 0 if no picture is returned
 * @param avpkt input packet
 * @return number of consumed bytes on success or negative if decode fails
 */
static int lag_decode_frame(AVCodecContext *avctx,
                            void *data, int *data_size, AVPacket *avpkt)
{
    const uint8_t *buf = avpkt->data;
    int buf_size = avpkt->size;
    LagarithContext *l = avctx->priv_data;
    AVFrame *const p = &l->picture;
    uint8_t frametype = 0;
    uint32_t offset_gu = 0, offset_bv = 0, offset_ry = 9;

    AVFrame *picture = data;

    if (p->data[0])
        avctx->release_buffer(avctx, p);

    p->reference = 0;
    p->key_frame = 1;

    frametype = buf[0];

    offset_gu = AV_RL32(buf + 1);
    offset_bv = AV_RL32(buf + 5);

    switch (frametype) {
    case FRAME_ARITH_YV12:
        avctx->pix_fmt = PIX_FMT_YUV420P;

        if (avctx->get_buffer(avctx, p) < 0) {
            av_log(avctx, AV_LOG_ERROR, "get_buffer() failed\n");
            return -1;
        }

        lag_decode_arith_plane(l, p->data[0], avctx->width, avctx->height,
                               p->linesize[0], buf + offset_ry,
                               buf_size);
        lag_decode_arith_plane(l, p->data[2], avctx->width / 2,
                               avctx->height / 2, p->linesize[2],
                               buf + offset_gu, buf_size);
        lag_decode_arith_plane(l, p->data[1], avctx->width / 2,
                               avctx->height / 2, p->linesize[1],
                               buf + offset_bv, buf_size);
        break;
    default:
        av_log(avctx, AV_LOG_ERROR,
               "Unsupported Lagarith frame type: %#x\n", frametype);
        return -1;
    }

    *picture = *p;
    *data_size = sizeof(AVFrame);

    return buf_size;
}

static av_cold int lag_decode_init(AVCodecContext *avctx)
{
    LagarithContext *l = avctx->priv_data;
    l->avctx = avctx;

    dsputil_init(&l->dsp, avctx);

    return 0;
}

static av_cold int lag_decode_end(AVCodecContext *avctx)
{
    LagarithContext *l = avctx->priv_data;

    if (l->picture.data[0])
        avctx->release_buffer(avctx, &l->picture);

    return 0;
}

AVCodec ff_lagarith_decoder = {
    "lagarith",
    AVMEDIA_TYPE_VIDEO,
    CODEC_ID_LAGARITH,
    sizeof(LagarithContext),
    lag_decode_init,
    NULL,
    lag_decode_end,
    lag_decode_frame,
    CODEC_CAP_DR1,
    .long_name = NULL_IF_CONFIG_SMALL("Lagarith lossless"),
};
