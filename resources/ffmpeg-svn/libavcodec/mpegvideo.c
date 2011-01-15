/*
 * The simplest mpeg encoder (well, it was the simplest!)
 * Copyright (c) 2000,2001 Fabrice Bellard.
 * Copyright (c) 2002-2004 Michael Niedermayer <michaelni@gmx.at>
 *
 * 4MV & hq & B-frame encoding stuff by Michael Niedermayer <michaelni@gmx.at>
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
 * @file mpegvideo.c
 * The simplest mpeg encoder (well, it was the simplest!).
 */

#include "avcodec.h"
#include "dsputil.h"
#include "mpegvideo.h"
#include "mpegvideo_common.h"
#include "mjpegenc.h"
#include "msmpeg4.h"
#include "faandct.h"
#include <limits.h>

//#undef NDEBUG
//#include <assert.h>

static void dct_unquantize_mpeg1_intra_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale);
static void dct_unquantize_mpeg1_inter_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale);
static void dct_unquantize_mpeg2_intra_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale);
static void dct_unquantize_mpeg2_intra_bitexact(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale);
static void dct_unquantize_mpeg2_inter_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale);
static void dct_unquantize_h263_intra_c(MpegEncContext *s,
                                  DCTELEM *block, int n, int qscale);
static void dct_unquantize_h263_inter_c(MpegEncContext *s,
                                  DCTELEM *block, int n, int qscale);

int  XVMC_field_start(MpegEncContext*s, AVCodecContext *avctx);
void XVMC_field_end(MpegEncContext *s);
void XVMC_decode_mb(MpegEncContext *s);


/* enable all paranoid tests for rounding, overflows, etc... */
//#define PARANOID

//#define DEBUG


static const uint8_t ff_default_chroma_qscale_table[32]={
//  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
};

const uint8_t ff_mpeg1_dc_scale_table[128]={
//  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
};


const uint8_t *ff_find_start_code(const uint8_t * restrict p, const uint8_t *end, uint32_t * restrict state){
    int i;

    assert(p<=end);
    if(p>=end)
        return end;

    for(i=0; i<3; i++){
        uint32_t tmp= *state << 8;
        *state= tmp + *(p++);
        if(tmp == 0x100 || p==end)
            return p;
    }

    while(p<end){
        if     (p[-1] > 1      ) p+= 3;
        else if(p[-2]          ) p+= 2;
        else if(p[-3]|(p[-1]-1)) p++;
        else{
            p++;
            break;
        }
    }

    p= FFMIN(p, end)-4;
    *state= AV_RB32(p);

    return p+4;
}

/* init common dct for both encoder and decoder */
int ff_dct_common_init(MpegEncContext *s)
{
    s->dct_unquantize_h263_intra = dct_unquantize_h263_intra_c;
    s->dct_unquantize_h263_inter = dct_unquantize_h263_inter_c;
    s->dct_unquantize_mpeg1_intra = dct_unquantize_mpeg1_intra_c;
    s->dct_unquantize_mpeg1_inter = dct_unquantize_mpeg1_inter_c;
    s->dct_unquantize_mpeg2_intra = dct_unquantize_mpeg2_intra_c;
    if(s->flags & CODEC_FLAG_BITEXACT)
        s->dct_unquantize_mpeg2_intra = dct_unquantize_mpeg2_intra_bitexact;
    s->dct_unquantize_mpeg2_inter = dct_unquantize_mpeg2_inter_c;

#if defined(HAVE_MMX)
    MPV_common_init_mmx(s);
#elif defined(ARCH_ALPHA)
    MPV_common_init_axp(s);
#elif defined(CONFIG_MLIB)
    MPV_common_init_mlib(s);
#elif defined(HAVE_MMI)
    MPV_common_init_mmi(s);
#elif defined(ARCH_ARMV4L)
    MPV_common_init_armv4l(s);
#elif defined(HAVE_ALTIVEC)
    MPV_common_init_altivec(s);
#elif defined(ARCH_BFIN)
    MPV_common_init_bfin(s);
#endif

    /* load & permutate scantables
       note: only wmv uses different ones
    */
    if(s->alternate_scan){
        ff_init_scantable(s->dsp.idct_permutation, &s->inter_scantable  , ff_alternate_vertical_scan);
        ff_init_scantable(s->dsp.idct_permutation, &s->intra_scantable  , ff_alternate_vertical_scan);
    }else{
        ff_init_scantable(s->dsp.idct_permutation, &s->inter_scantable  , ff_zigzag_direct);
        ff_init_scantable(s->dsp.idct_permutation, &s->intra_scantable  , ff_zigzag_direct);
    }
    ff_init_scantable(s->dsp.idct_permutation, &s->intra_h_scantable, ff_alternate_horizontal_scan);
    ff_init_scantable(s->dsp.idct_permutation, &s->intra_v_scantable, ff_alternate_vertical_scan);

    return 0;
}

void ff_copy_picture(Picture *dst, Picture *src){
    *dst = *src;
    dst->type= FF_BUFFER_TYPE_COPY;
}

/**
 * allocates a Picture
 * The pixels are allocated/set by calling get_buffer() if shared=0
 */
int alloc_picture(MpegEncContext *s, Picture *pic, int shared){
    const int big_mb_num= s->mb_stride*(s->mb_height+1) + 1; //the +1 is needed so memset(,,stride*height) does not sig11
    const int mb_array_size= s->mb_stride*s->mb_height;
    const int b8_array_size= s->b8_stride*s->mb_height*2;
    const int b4_array_size= s->b4_stride*s->mb_height*4;
    int i;
    int r= -1;

    if(shared){
        assert(pic->data[0]);
        assert(pic->type == 0 || pic->type == FF_BUFFER_TYPE_SHARED);
        pic->type= FF_BUFFER_TYPE_SHARED;
    }else{
        assert(!pic->data[0]);

        r= s->avctx->get_buffer(s->avctx, (AVFrame*)pic);

        if(r<0 || !pic->age || !pic->type || !pic->data[0]){
            av_log(s->avctx, AV_LOG_ERROR, "get_buffer() failed (%d %d %d %p)\n", r, pic->age, pic->type, pic->data[0]);
            return -1;
        }

        if(s->linesize && (s->linesize != pic->linesize[0] || s->uvlinesize != pic->linesize[1])){
            av_log(s->avctx, AV_LOG_ERROR, "get_buffer() failed (stride changed)\n");
            s->avctx->release_buffer(s->avctx, (AVFrame*)pic);
            return -1;
        }

        if(pic->linesize[1] != pic->linesize[2]){
            av_log(s->avctx, AV_LOG_ERROR, "get_buffer() failed (uv stride mismatch)\n");
            s->avctx->release_buffer(s->avctx, (AVFrame*)pic);
            return -1;
        }

        s->linesize  = pic->linesize[0];
        s->uvlinesize= pic->linesize[1];
    }

    if(pic->qscale_table==NULL){
        if (s->encoding) {
            CHECKED_ALLOCZ(pic->mb_var   , mb_array_size * sizeof(int16_t))
            CHECKED_ALLOCZ(pic->mc_mb_var, mb_array_size * sizeof(int16_t))
            CHECKED_ALLOCZ(pic->mb_mean  , mb_array_size * sizeof(int8_t))
        }

        CHECKED_ALLOCZ(pic->mbskip_table , mb_array_size * sizeof(uint8_t)+2) //the +2 is for the slice end check
        CHECKED_ALLOCZ(pic->qscale_table , mb_array_size * sizeof(uint8_t))
        CHECKED_ALLOCZ(pic->mb_type_base , (big_mb_num + s->mb_stride) * sizeof(uint32_t))
        pic->mb_type= pic->mb_type_base + 2*s->mb_stride+1;
        if(s->out_format == FMT_H264){
            for(i=0; i<2; i++){
                CHECKED_ALLOCZ(pic->motion_val_base[i], 2 * (b4_array_size+4)  * sizeof(int16_t))
                pic->motion_val[i]= pic->motion_val_base[i]+4;
                CHECKED_ALLOCZ(pic->ref_index[i], b8_array_size * sizeof(uint8_t))
            }
            pic->motion_subsample_log2= 2;
        }else if(s->out_format == FMT_H263 || s->encoding || (s->avctx->debug&FF_DEBUG_MV) || (s->avctx->debug_mv)){
            for(i=0; i<2; i++){
                CHECKED_ALLOCZ(pic->motion_val_base[i], 2 * (b8_array_size+4) * sizeof(int16_t))
                pic->motion_val[i]= pic->motion_val_base[i]+4;
                CHECKED_ALLOCZ(pic->ref_index[i], b8_array_size * sizeof(uint8_t))
            }
            pic->motion_subsample_log2= 3;
        }
        if(s->avctx->debug&FF_DEBUG_DCT_COEFF) {
            CHECKED_ALLOCZ(pic->dct_coeff, 64 * mb_array_size * sizeof(DCTELEM)*6)
        }
        pic->qstride= s->mb_stride;
        CHECKED_ALLOCZ(pic->pan_scan , 1 * sizeof(AVPanScan))
    }

    /* It might be nicer if the application would keep track of these
     * but it would require an API change. */
    memmove(s->prev_pict_types+1, s->prev_pict_types, PREV_PICT_TYPES_BUFFER_SIZE-1);
    s->prev_pict_types[0]= s->dropable ? FF_B_TYPE : s->pict_type;
    if(pic->age < PREV_PICT_TYPES_BUFFER_SIZE && s->prev_pict_types[pic->age] == FF_B_TYPE)
        pic->age= INT_MAX; // Skipped MBs in B-frames are quite rare in MPEG-1/2 and it is a bit tricky to skip them anyway.

    return 0;
fail: //for the CHECKED_ALLOCZ macro
    if(r>=0)
        s->avctx->release_buffer(s->avctx, (AVFrame*)pic);
    return -1;
}

/**
 * deallocates a picture
 */
static void free_picture(MpegEncContext *s, Picture *pic){
    int i;

    if(pic->data[0] && pic->type!=FF_BUFFER_TYPE_SHARED){
        s->avctx->release_buffer(s->avctx, (AVFrame*)pic);
    }

    av_freep(&pic->mb_var);
    av_freep(&pic->mc_mb_var);
    av_freep(&pic->mb_mean);
    av_freep(&pic->mbskip_table);
    av_freep(&pic->qscale_table);
    av_freep(&pic->mb_type_base);
    av_freep(&pic->dct_coeff);
    av_freep(&pic->pan_scan);
    pic->mb_type= NULL;
    for(i=0; i<2; i++){
        av_freep(&pic->motion_val_base[i]);
        av_freep(&pic->ref_index[i]);
    }

    if(pic->type == FF_BUFFER_TYPE_SHARED){
        for(i=0; i<4; i++){
            pic->base[i]=
            pic->data[i]= NULL;
        }
        pic->type= 0;
    }
}

static int init_duplicate_context(MpegEncContext *s, MpegEncContext *base){
    int i;

    // edge emu needs blocksize + filter length - 1 (=17x17 for halfpel / 21x21 for h264)
    CHECKED_ALLOCZ(s->allocated_edge_emu_buffer, (s->width+64)*2*21*2); //(width + edge + align)*interlaced*MBsize*tolerance
    s->edge_emu_buffer= s->allocated_edge_emu_buffer + (s->width+64)*2*21;

     //FIXME should be linesize instead of s->width*2 but that is not known before get_buffer()
    CHECKED_ALLOCZ(s->me.scratchpad,  (s->width+64)*4*16*2*sizeof(uint8_t))
    s->me.temp=         s->me.scratchpad;
    s->rd_scratchpad=   s->me.scratchpad;
    s->b_scratchpad=    s->me.scratchpad;
    s->obmc_scratchpad= s->me.scratchpad + 16;
    if (s->encoding) {
        CHECKED_ALLOCZ(s->me.map      , ME_MAP_SIZE*sizeof(uint32_t))
        CHECKED_ALLOCZ(s->me.score_map, ME_MAP_SIZE*sizeof(uint32_t))
        if(s->avctx->noise_reduction){
            CHECKED_ALLOCZ(s->dct_error_sum, 2 * 64 * sizeof(int))
        }
    }
    CHECKED_ALLOCZ(s->blocks, 64*12*2 * sizeof(DCTELEM))
    s->block= s->blocks[0];

    for(i=0;i<12;i++){
        s->pblocks[i] = (short *)(&s->block[i]);
    }
    return 0;
fail:
    return -1; //free() through MPV_common_end()
}

static void free_duplicate_context(MpegEncContext *s){
    if(s==NULL) return;

    av_freep(&s->allocated_edge_emu_buffer); s->edge_emu_buffer= NULL;
    av_freep(&s->me.scratchpad);
    s->me.temp=
    s->rd_scratchpad=
    s->b_scratchpad=
    s->obmc_scratchpad= NULL;

    av_freep(&s->dct_error_sum);
    av_freep(&s->me.map);
    av_freep(&s->me.score_map);
    av_freep(&s->blocks);
    s->block= NULL;
}

static void backup_duplicate_context(MpegEncContext *bak, MpegEncContext *src){
#define COPY(a) bak->a= src->a
    COPY(allocated_edge_emu_buffer);
    COPY(edge_emu_buffer);
    COPY(me.scratchpad);
    COPY(me.temp);
    COPY(rd_scratchpad);
    COPY(b_scratchpad);
    COPY(obmc_scratchpad);
    COPY(me.map);
    COPY(me.score_map);
    COPY(blocks);
    COPY(block);
    COPY(start_mb_y);
    COPY(end_mb_y);
    COPY(me.map_generation);
    COPY(pb);
    COPY(dct_error_sum);
    COPY(dct_count[0]);
    COPY(dct_count[1]);
#undef COPY
}

void ff_update_duplicate_context(MpegEncContext *dst, MpegEncContext *src){
    MpegEncContext bak;
    int i;
    //FIXME copy only needed parts
//START_TIMER
    backup_duplicate_context(&bak, dst);
    memcpy(dst, src, sizeof(MpegEncContext));
    backup_duplicate_context(dst, &bak);
    for(i=0;i<12;i++){
        dst->pblocks[i] = (short *)(&dst->block[i]);
    }
//STOP_TIMER("update_duplicate_context") //about 10k cycles / 0.01 sec for 1000frames on 1ghz with 2 threads
}

/**
 * sets the given MpegEncContext to common defaults (same for encoding and decoding).
 * the changed fields will not depend upon the prior state of the MpegEncContext.
 */
void MPV_common_defaults(MpegEncContext *s){
    s->y_dc_scale_table=
    s->c_dc_scale_table= ff_mpeg1_dc_scale_table;
    s->chroma_qscale_table= ff_default_chroma_qscale_table;
    s->progressive_frame= 1;
    s->progressive_sequence= 1;
    s->picture_structure= PICT_FRAME;

    s->coded_picture_number = 0;
    s->picture_number = 0;
    s->input_picture_number = 0;

    s->picture_in_gop_number = 0;

    s->f_code = 1;
    s->b_code = 1;
}

/**
 * sets the given MpegEncContext to defaults for decoding.
 * the changed fields will not depend upon the prior state of the MpegEncContext.
 */
void MPV_decode_defaults(MpegEncContext *s){
    MPV_common_defaults(s);
}

/**
 * init common structure for both encoder and decoder.
 * this assumes that some variables like width/height are already set
 */
int MPV_common_init(MpegEncContext *s)
{
    int y_size, c_size, yc_size, i, mb_array_size, mv_table_size, x, y, threads;

    s->mb_height = (s->height + 15) / 16;

    if(s->avctx->thread_count > MAX_THREADS || (s->avctx->thread_count > s->mb_height && s->mb_height)){
        av_log(s->avctx, AV_LOG_ERROR, "too many threads\n");
        return -1;
    }

    if((s->width || s->height) && avcodec_check_dimensions(s->avctx, s->width, s->height))
        return -1;

    dsputil_init(&s->dsp, s->avctx);
    ff_dct_common_init(s);

    s->flags= s->avctx->flags;
    s->flags2= s->avctx->flags2;

    s->mb_width  = (s->width  + 15) / 16;
    s->mb_stride = s->mb_width + 1;
    s->b8_stride = s->mb_width*2 + 1;
    s->b4_stride = s->mb_width*4 + 1;
    mb_array_size= s->mb_height * s->mb_stride;
    mv_table_size= (s->mb_height+2) * s->mb_stride + 1;

    /* set chroma shifts */
    avcodec_get_chroma_sub_sample(s->avctx->pix_fmt,&(s->chroma_x_shift),
                                                    &(s->chroma_y_shift) );

    /* set default edge pos, will be overriden in decode_header if needed */
    s->h_edge_pos= s->mb_width*16;
    s->v_edge_pos= s->mb_height*16;

    s->mb_num = s->mb_width * s->mb_height;

    s->block_wrap[0]=
    s->block_wrap[1]=
    s->block_wrap[2]=
    s->block_wrap[3]= s->b8_stride;
    s->block_wrap[4]=
    s->block_wrap[5]= s->mb_stride;

    y_size = s->b8_stride * (2 * s->mb_height + 1);
    c_size = s->mb_stride * (s->mb_height + 1);
    yc_size = y_size + 2 * c_size;

    /* convert fourcc to upper case */
    s->codec_tag=          toupper( s->avctx->codec_tag     &0xFF)
                        + (toupper((s->avctx->codec_tag>>8 )&0xFF)<<8 )
                        + (toupper((s->avctx->codec_tag>>16)&0xFF)<<16)
                        + (toupper((s->avctx->codec_tag>>24)&0xFF)<<24);

    s->stream_codec_tag=          toupper( s->avctx->stream_codec_tag     &0xFF)
                               + (toupper((s->avctx->stream_codec_tag>>8 )&0xFF)<<8 )
                               + (toupper((s->avctx->stream_codec_tag>>16)&0xFF)<<16)
                               + (toupper((s->avctx->stream_codec_tag>>24)&0xFF)<<24);

    s->avctx->coded_frame= (AVFrame*)&s->current_picture;

    CHECKED_ALLOCZ(s->mb_index2xy, (s->mb_num+1)*sizeof(int)) //error ressilience code looks cleaner with this
    for(y=0; y<s->mb_height; y++){
        for(x=0; x<s->mb_width; x++){
            s->mb_index2xy[ x + y*s->mb_width ] = x + y*s->mb_stride;
        }
    }
    s->mb_index2xy[ s->mb_height*s->mb_width ] = (s->mb_height-1)*s->mb_stride + s->mb_width; //FIXME really needed?

    if (s->encoding) {
        /* Allocate MV tables */
        CHECKED_ALLOCZ(s->p_mv_table_base            , mv_table_size * 2 * sizeof(int16_t))
        CHECKED_ALLOCZ(s->b_forw_mv_table_base       , mv_table_size * 2 * sizeof(int16_t))
        CHECKED_ALLOCZ(s->b_back_mv_table_base       , mv_table_size * 2 * sizeof(int16_t))
        CHECKED_ALLOCZ(s->b_bidir_forw_mv_table_base , mv_table_size * 2 * sizeof(int16_t))
        CHECKED_ALLOCZ(s->b_bidir_back_mv_table_base , mv_table_size * 2 * sizeof(int16_t))
        CHECKED_ALLOCZ(s->b_direct_mv_table_base     , mv_table_size * 2 * sizeof(int16_t))
        s->p_mv_table           = s->p_mv_table_base            + s->mb_stride + 1;
        s->b_forw_mv_table      = s->b_forw_mv_table_base       + s->mb_stride + 1;
        s->b_back_mv_table      = s->b_back_mv_table_base       + s->mb_stride + 1;
        s->b_bidir_forw_mv_table= s->b_bidir_forw_mv_table_base + s->mb_stride + 1;
        s->b_bidir_back_mv_table= s->b_bidir_back_mv_table_base + s->mb_stride + 1;
        s->b_direct_mv_table    = s->b_direct_mv_table_base     + s->mb_stride + 1;

        if(s->msmpeg4_version){
            CHECKED_ALLOCZ(s->ac_stats, 2*2*(MAX_LEVEL+1)*(MAX_RUN+1)*2*sizeof(int));
        }
        CHECKED_ALLOCZ(s->avctx->stats_out, 256);

        /* Allocate MB type table */
        CHECKED_ALLOCZ(s->mb_type  , mb_array_size * sizeof(uint16_t)) //needed for encoding

        CHECKED_ALLOCZ(s->lambda_table, mb_array_size * sizeof(int))

        CHECKED_ALLOCZ(s->q_intra_matrix, 64*32 * sizeof(int))
        CHECKED_ALLOCZ(s->q_inter_matrix, 64*32 * sizeof(int))
        CHECKED_ALLOCZ(s->q_intra_matrix16, 64*32*2 * sizeof(uint16_t))
        CHECKED_ALLOCZ(s->q_inter_matrix16, 64*32*2 * sizeof(uint16_t))
        CHECKED_ALLOCZ(s->input_picture, MAX_PICTURE_COUNT * sizeof(Picture*))
        CHECKED_ALLOCZ(s->reordered_input_picture, MAX_PICTURE_COUNT * sizeof(Picture*))

        if(s->avctx->noise_reduction){
            CHECKED_ALLOCZ(s->dct_offset, 2 * 64 * sizeof(uint16_t))
        }
    }
    CHECKED_ALLOCZ(s->picture, MAX_PICTURE_COUNT * sizeof(Picture))

    CHECKED_ALLOCZ(s->error_status_table, mb_array_size*sizeof(uint8_t))

    if(s->codec_id==CODEC_ID_MPEG4 || (s->flags & CODEC_FLAG_INTERLACED_ME)){
        /* interlaced direct mode decoding tables */
            for(i=0; i<2; i++){
                int j, k;
                for(j=0; j<2; j++){
                    for(k=0; k<2; k++){
                        CHECKED_ALLOCZ(s->b_field_mv_table_base[i][j][k]     , mv_table_size * 2 * sizeof(int16_t))
                        s->b_field_mv_table[i][j][k]    = s->b_field_mv_table_base[i][j][k]     + s->mb_stride + 1;
                    }
                    CHECKED_ALLOCZ(s->b_field_select_table[i][j]     , mb_array_size * 2 * sizeof(uint8_t))
                    CHECKED_ALLOCZ(s->p_field_mv_table_base[i][j]     , mv_table_size * 2 * sizeof(int16_t))
                    s->p_field_mv_table[i][j]    = s->p_field_mv_table_base[i][j]     + s->mb_stride + 1;
                }
                CHECKED_ALLOCZ(s->p_field_select_table[i]      , mb_array_size * 2 * sizeof(uint8_t))
            }
    }
    if (s->out_format == FMT_H263) {
        /* ac values */
        CHECKED_ALLOCZ(s->ac_val_base, yc_size * sizeof(int16_t) * 16);
        s->ac_val[0] = s->ac_val_base + s->b8_stride + 1;
        s->ac_val[1] = s->ac_val_base + y_size + s->mb_stride + 1;
        s->ac_val[2] = s->ac_val[1] + c_size;

        /* cbp values */
        CHECKED_ALLOCZ(s->coded_block_base, y_size);
        s->coded_block= s->coded_block_base + s->b8_stride + 1;

        /* cbp, ac_pred, pred_dir */
        CHECKED_ALLOCZ(s->cbp_table  , mb_array_size * sizeof(uint8_t))
        CHECKED_ALLOCZ(s->pred_dir_table, mb_array_size * sizeof(uint8_t))
    }

    if (s->h263_pred || s->h263_plus || !s->encoding) {
        /* dc values */
        //MN: we need these for error resilience of intra-frames
        CHECKED_ALLOCZ(s->dc_val_base, yc_size * sizeof(int16_t));
        s->dc_val[0] = s->dc_val_base + s->b8_stride + 1;
        s->dc_val[1] = s->dc_val_base + y_size + s->mb_stride + 1;
        s->dc_val[2] = s->dc_val[1] + c_size;
        for(i=0;i<yc_size;i++)
            s->dc_val_base[i] = 1024;
    }

    /* which mb is a intra block */
    CHECKED_ALLOCZ(s->mbintra_table, mb_array_size);
    memset(s->mbintra_table, 1, mb_array_size);

    /* init macroblock skip table */
    CHECKED_ALLOCZ(s->mbskip_table, mb_array_size+2);
    //Note the +1 is for a quicker mpeg4 slice_end detection
    CHECKED_ALLOCZ(s->prev_pict_types, PREV_PICT_TYPES_BUFFER_SIZE);

    s->parse_context.state= -1;
    if((s->avctx->debug&(FF_DEBUG_VIS_QP|FF_DEBUG_VIS_MB_TYPE)) || (s->avctx->debug_mv)){
       s->visualization_buffer[0] = av_malloc((s->mb_width*16 + 2*EDGE_WIDTH) * s->mb_height*16 + 2*EDGE_WIDTH);
       s->visualization_buffer[1] = av_malloc((s->mb_width*16 + 2*EDGE_WIDTH) * s->mb_height*16 + 2*EDGE_WIDTH);
       s->visualization_buffer[2] = av_malloc((s->mb_width*16 + 2*EDGE_WIDTH) * s->mb_height*16 + 2*EDGE_WIDTH);
    }

    s->context_initialized = 1;

    s->thread_context[0]= s;
    threads = s->avctx->thread_count;

    for(i=1; i<threads; i++){
        s->thread_context[i]= av_malloc(sizeof(MpegEncContext));
        memcpy(s->thread_context[i], s, sizeof(MpegEncContext));
    }

    for(i=0; i<threads; i++){
        if(init_duplicate_context(s->thread_context[i], s) < 0)
           goto fail;
        s->thread_context[i]->start_mb_y= (s->mb_height*(i  ) + s->avctx->thread_count/2) / s->avctx->thread_count;
        s->thread_context[i]->end_mb_y  = (s->mb_height*(i+1) + s->avctx->thread_count/2) / s->avctx->thread_count;
    }

    return 0;
 fail:
    MPV_common_end(s);
    return -1;
}

/* init common structure for both encoder and decoder */
void MPV_common_end(MpegEncContext *s)
{
    int i, j, k;

    for(i=0; i<s->avctx->thread_count; i++){
        free_duplicate_context(s->thread_context[i]);
    }
    for(i=1; i<s->avctx->thread_count; i++){
        av_freep(&s->thread_context[i]);
    }

    av_freep(&s->parse_context.buffer);
    s->parse_context.buffer_size=0;

    av_freep(&s->mb_type);
    av_freep(&s->p_mv_table_base);
    av_freep(&s->b_forw_mv_table_base);
    av_freep(&s->b_back_mv_table_base);
    av_freep(&s->b_bidir_forw_mv_table_base);
    av_freep(&s->b_bidir_back_mv_table_base);
    av_freep(&s->b_direct_mv_table_base);
    s->p_mv_table= NULL;
    s->b_forw_mv_table= NULL;
    s->b_back_mv_table= NULL;
    s->b_bidir_forw_mv_table= NULL;
    s->b_bidir_back_mv_table= NULL;
    s->b_direct_mv_table= NULL;
    for(i=0; i<2; i++){
        for(j=0; j<2; j++){
            for(k=0; k<2; k++){
                av_freep(&s->b_field_mv_table_base[i][j][k]);
                s->b_field_mv_table[i][j][k]=NULL;
            }
            av_freep(&s->b_field_select_table[i][j]);
            av_freep(&s->p_field_mv_table_base[i][j]);
            s->p_field_mv_table[i][j]=NULL;
        }
        av_freep(&s->p_field_select_table[i]);
    }

    av_freep(&s->dc_val_base);
    av_freep(&s->ac_val_base);
    av_freep(&s->coded_block_base);
    av_freep(&s->mbintra_table);
    av_freep(&s->cbp_table);
    av_freep(&s->pred_dir_table);

    av_freep(&s->mbskip_table);
    av_freep(&s->prev_pict_types);
    av_freep(&s->bitstream_buffer);
    s->allocated_bitstream_buffer_size=0;

    av_freep(&s->avctx->stats_out);
    av_freep(&s->ac_stats);
    av_freep(&s->error_status_table);
    av_freep(&s->mb_index2xy);
    av_freep(&s->lambda_table);
    av_freep(&s->q_intra_matrix);
    av_freep(&s->q_inter_matrix);
    av_freep(&s->q_intra_matrix16);
    av_freep(&s->q_inter_matrix16);
    av_freep(&s->input_picture);
    av_freep(&s->reordered_input_picture);
    av_freep(&s->dct_offset);

    if(s->picture){
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            free_picture(s, &s->picture[i]);
        }
    }
    av_freep(&s->picture);
    s->context_initialized = 0;
    s->last_picture_ptr=
    s->next_picture_ptr=
    s->current_picture_ptr= NULL;
    s->linesize= s->uvlinesize= 0;

    for(i=0; i<3; i++)
        av_freep(&s->visualization_buffer[i]);

    avcodec_default_free_buffers(s->avctx);
}

void init_rl(RLTable *rl, uint8_t static_store[2][2*MAX_RUN + MAX_LEVEL + 3])
{
    int8_t max_level[MAX_RUN+1], max_run[MAX_LEVEL+1];
    uint8_t index_run[MAX_RUN+1];
    int last, run, level, start, end, i;

    /* If table is static, we can quit if rl->max_level[0] is not NULL */
    if(static_store && rl->max_level[0])
        return;

    /* compute max_level[], max_run[] and index_run[] */
    for(last=0;last<2;last++) {
        if (last == 0) {
            start = 0;
            end = rl->last;
        } else {
            start = rl->last;
            end = rl->n;
        }

        memset(max_level, 0, MAX_RUN + 1);
        memset(max_run, 0, MAX_LEVEL + 1);
        memset(index_run, rl->n, MAX_RUN + 1);
        for(i=start;i<end;i++) {
            run = rl->table_run[i];
            level = rl->table_level[i];
            if (index_run[run] == rl->n)
                index_run[run] = i;
            if (level > max_level[run])
                max_level[run] = level;
            if (run > max_run[level])
                max_run[level] = run;
        }
        if(static_store)
            rl->max_level[last] = static_store[last];
        else
            rl->max_level[last] = av_malloc(MAX_RUN + 1);
        memcpy(rl->max_level[last], max_level, MAX_RUN + 1);
        if(static_store)
            rl->max_run[last] = static_store[last] + MAX_RUN + 1;
        else
            rl->max_run[last] = av_malloc(MAX_LEVEL + 1);
        memcpy(rl->max_run[last], max_run, MAX_LEVEL + 1);
        if(static_store)
            rl->index_run[last] = static_store[last] + MAX_RUN + MAX_LEVEL + 2;
        else
            rl->index_run[last] = av_malloc(MAX_RUN + 1);
        memcpy(rl->index_run[last], index_run, MAX_RUN + 1);
    }
}

void init_vlc_rl(RLTable *rl)
{
    int i, q;

    for(q=0; q<32; q++){
        int qmul= q*2;
        int qadd= (q-1)|1;

        if(q==0){
            qmul=1;
            qadd=0;
        }
        for(i=0; i<rl->vlc.table_size; i++){
            int code= rl->vlc.table[i][0];
            int len = rl->vlc.table[i][1];
            int level, run;

            if(len==0){ // illegal code
                run= 66;
                level= MAX_LEVEL;
            }else if(len<0){ //more bits needed
                run= 0;
                level= code;
            }else{
                if(code==rl->n){ //esc
                    run= 66;
                    level= 0;
                }else{
                    run=   rl->table_run  [code] + 1;
                    level= rl->table_level[code] * qmul + qadd;
                    if(code >= rl->last) run+=192;
                }
            }
            rl->rl_vlc[q][i].len= len;
            rl->rl_vlc[q][i].level= level;
            rl->rl_vlc[q][i].run= run;
        }
    }
}

int ff_find_unused_picture(MpegEncContext *s, int shared){
    int i;

    if(shared){
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            if(s->picture[i].data[0]==NULL && s->picture[i].type==0) return i;
        }
    }else{
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            if(s->picture[i].data[0]==NULL && s->picture[i].type!=0) return i; //FIXME
        }
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            if(s->picture[i].data[0]==NULL) return i;
        }
    }

    av_log(s->avctx, AV_LOG_FATAL, "Internal error, picture buffer overflow\n");
    /* We could return -1, but the codec would crash trying to draw into a
     * non-existing frame anyway. This is safer than waiting for a random crash.
     * Also the return of this is never useful, an encoder must only allocate
     * as much as allowed in the specification. This has no relationship to how
     * much libavcodec could allocate (and MAX_PICTURE_COUNT is always large
     * enough for such valid streams).
     * Plus, a decoder has to check stream validity and remove frames if too
     * many reference frames are around. Waiting for "OOM" is not correct at
     * all. Similarly, missing reference frames have to be replaced by
     * interpolated/MC frames, anything else is a bug in the codec ...
     */
    abort();
    return -1;
}

static void update_noise_reduction(MpegEncContext *s){
    int intra, i;

    for(intra=0; intra<2; intra++){
        if(s->dct_count[intra] > (1<<16)){
            for(i=0; i<64; i++){
                s->dct_error_sum[intra][i] >>=1;
            }
            s->dct_count[intra] >>= 1;
        }

        for(i=0; i<64; i++){
            s->dct_offset[intra][i]= (s->avctx->noise_reduction * s->dct_count[intra] + s->dct_error_sum[intra][i]/2) / (s->dct_error_sum[intra][i]+1);
        }
    }
}

/**
 * generic function for encode/decode called after coding/decoding the header and before a frame is coded/decoded
 */
int MPV_frame_start(MpegEncContext *s, AVCodecContext *avctx)
{
    int i;
    AVFrame *pic;
    s->mb_skipped = 0;

    assert(s->last_picture_ptr==NULL || s->out_format != FMT_H264 || s->codec_id == CODEC_ID_SVQ3);

    /* mark&release old frames */
    if (s->pict_type != FF_B_TYPE && s->last_picture_ptr && s->last_picture_ptr != s->next_picture_ptr && s->last_picture_ptr->data[0]) {
      if(s->out_format != FMT_H264 || s->codec_id == CODEC_ID_SVQ3){
        avctx->release_buffer(avctx, (AVFrame*)s->last_picture_ptr);

        /* release forgotten pictures */
        /* if(mpeg124/h263) */
        if(!s->encoding){
            for(i=0; i<MAX_PICTURE_COUNT; i++){
                if(s->picture[i].data[0] && &s->picture[i] != s->next_picture_ptr && s->picture[i].reference){
                    av_log(avctx, AV_LOG_ERROR, "releasing zombie picture\n");
                    avctx->release_buffer(avctx, (AVFrame*)&s->picture[i]);
                }
            }
        }
      }
    }
alloc:
    if(!s->encoding){
        /* release non reference frames */
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            if(s->picture[i].data[0] && !s->picture[i].reference /*&& s->picture[i].type!=FF_BUFFER_TYPE_SHARED*/){
                s->avctx->release_buffer(s->avctx, (AVFrame*)&s->picture[i]);
            }
        }

        if(s->current_picture_ptr && s->current_picture_ptr->data[0]==NULL)
            pic= (AVFrame*)s->current_picture_ptr; //we already have a unused image (maybe it was set before reading the header)
        else{
            i= ff_find_unused_picture(s, 0);
            pic= (AVFrame*)&s->picture[i];
        }

        pic->reference= 0;
        if (!s->dropable){
            if (s->codec_id == CODEC_ID_H264)
                pic->reference = s->picture_structure;
            else if (s->pict_type != FF_B_TYPE)
                pic->reference = 3;
        }

        pic->coded_picture_number= s->coded_picture_number++;

        if( alloc_picture(s, (Picture*)pic, 0) < 0)
            return -1;

        s->current_picture_ptr= (Picture*)pic;
        s->current_picture_ptr->top_field_first= s->top_field_first; //FIXME use only the vars from current_pic
        s->current_picture_ptr->interlaced_frame= !s->progressive_frame && !s->progressive_sequence;
    }

    s->current_picture_ptr->pict_type= s->pict_type;
//    if(s->flags && CODEC_FLAG_QSCALE)
  //      s->current_picture_ptr->quality= s->new_picture_ptr->quality;
    s->current_picture_ptr->key_frame= s->pict_type == FF_I_TYPE;

    ff_copy_picture(&s->current_picture, s->current_picture_ptr);

    if (s->pict_type != FF_B_TYPE) {
        s->last_picture_ptr= s->next_picture_ptr;
        if(!s->dropable)
            s->next_picture_ptr= s->current_picture_ptr;
    }
/*    av_log(s->avctx, AV_LOG_DEBUG, "L%p N%p C%p L%p N%p C%p type:%d drop:%d\n", s->last_picture_ptr, s->next_picture_ptr,s->current_picture_ptr,
        s->last_picture_ptr    ? s->last_picture_ptr->data[0] : NULL,
        s->next_picture_ptr    ? s->next_picture_ptr->data[0] : NULL,
        s->current_picture_ptr ? s->current_picture_ptr->data[0] : NULL,
        s->pict_type, s->dropable);*/

    if(s->last_picture_ptr) ff_copy_picture(&s->last_picture, s->last_picture_ptr);
    if(s->next_picture_ptr) ff_copy_picture(&s->next_picture, s->next_picture_ptr);

    if(s->pict_type != FF_I_TYPE && (s->last_picture_ptr==NULL || s->last_picture_ptr->data[0]==NULL) && !s->dropable && s->codec_id != CODEC_ID_H264){
        av_log(avctx, AV_LOG_ERROR, "warning: first frame is no keyframe\n");
        assert(s->pict_type != FF_B_TYPE); //these should have been dropped if we don't have a reference
        goto alloc;
    }

    assert(s->pict_type == FF_I_TYPE || (s->last_picture_ptr && s->last_picture_ptr->data[0]));

    if(s->picture_structure!=PICT_FRAME && s->out_format != FMT_H264){
        int i;
        for(i=0; i<4; i++){
            if(s->picture_structure == PICT_BOTTOM_FIELD){
                 s->current_picture.data[i] += s->current_picture.linesize[i];
            }
            s->current_picture.linesize[i] *= 2;
            s->last_picture.linesize[i] *=2;
            s->next_picture.linesize[i] *=2;
        }
    }

    s->hurry_up= s->avctx->hurry_up;
    s->error_recognition= avctx->error_recognition;

    /* set dequantizer, we can't do it during init as it might change for mpeg4
       and we can't do it in the header decode as init is not called for mpeg4 there yet */
    if(s->mpeg_quant || s->codec_id == CODEC_ID_MPEG2VIDEO){
        s->dct_unquantize_intra = s->dct_unquantize_mpeg2_intra;
        s->dct_unquantize_inter = s->dct_unquantize_mpeg2_inter;
    }else if(s->out_format == FMT_H263 || s->out_format == FMT_H261){
        s->dct_unquantize_intra = s->dct_unquantize_h263_intra;
        s->dct_unquantize_inter = s->dct_unquantize_h263_inter;
    }else{
        s->dct_unquantize_intra = s->dct_unquantize_mpeg1_intra;
        s->dct_unquantize_inter = s->dct_unquantize_mpeg1_inter;
    }

    if(s->dct_error_sum){
        assert(s->avctx->noise_reduction && s->encoding);

        update_noise_reduction(s);
    }

#ifdef HAVE_XVMC
    if(s->avctx->xvmc_acceleration)
        return XVMC_field_start(s, avctx);
#endif
    return 0;
}

/* generic function for encode/decode called after a frame has been coded/decoded */
void MPV_frame_end(MpegEncContext *s)
{
    int i;
    /* draw edge for correct motion prediction if outside */
#ifdef HAVE_XVMC
//just to make sure that all data is rendered.
    if(s->avctx->xvmc_acceleration){
        XVMC_field_end(s);
    }else
#endif
    if(s->unrestricted_mv && s->current_picture.reference && !s->intra_only && !(s->flags&CODEC_FLAG_EMU_EDGE)) {
            s->dsp.draw_edges(s->current_picture.data[0], s->linesize  , s->h_edge_pos   , s->v_edge_pos   , EDGE_WIDTH  );
            s->dsp.draw_edges(s->current_picture.data[1], s->uvlinesize, s->h_edge_pos>>1, s->v_edge_pos>>1, EDGE_WIDTH/2);
            s->dsp.draw_edges(s->current_picture.data[2], s->uvlinesize, s->h_edge_pos>>1, s->v_edge_pos>>1, EDGE_WIDTH/2);
    }
    emms_c();

    s->last_pict_type    = s->pict_type;
    s->last_lambda_for[s->pict_type]= s->current_picture_ptr->quality;
    if(s->pict_type!=FF_B_TYPE){
        s->last_non_b_pict_type= s->pict_type;
    }
#if 0
        /* copy back current_picture variables */
    for(i=0; i<MAX_PICTURE_COUNT; i++){
        if(s->picture[i].data[0] == s->current_picture.data[0]){
            s->picture[i]= s->current_picture;
            break;
        }
    }
    assert(i<MAX_PICTURE_COUNT);
#endif

    if(s->encoding){
        /* release non-reference frames */
        for(i=0; i<MAX_PICTURE_COUNT; i++){
            if(s->picture[i].data[0] && !s->picture[i].reference /*&& s->picture[i].type!=FF_BUFFER_TYPE_SHARED*/){
                s->avctx->release_buffer(s->avctx, (AVFrame*)&s->picture[i]);
            }
        }
    }
    // clear copies, to avoid confusion
#if 0
    memset(&s->last_picture, 0, sizeof(Picture));
    memset(&s->next_picture, 0, sizeof(Picture));
    memset(&s->current_picture, 0, sizeof(Picture));
#endif
    s->avctx->coded_frame= (AVFrame*)s->current_picture_ptr;
}

/**
 * draws an line from (ex, ey) -> (sx, sy).
 * @param w width of the image
 * @param h height of the image
 * @param stride stride/linesize of the image
 * @param color color of the arrow
 */
static void draw_line(uint8_t *buf, int sx, int sy, int ex, int ey, int w, int h, int stride, int color){
    int x, y, fr, f;

    sx= av_clip(sx, 0, w-1);
    sy= av_clip(sy, 0, h-1);
    ex= av_clip(ex, 0, w-1);
    ey= av_clip(ey, 0, h-1);

    buf[sy*stride + sx]+= color;

    if(FFABS(ex - sx) > FFABS(ey - sy)){
        if(sx > ex){
            FFSWAP(int, sx, ex);
            FFSWAP(int, sy, ey);
        }
        buf+= sx + sy*stride;
        ex-= sx;
        f= ((ey-sy)<<16)/ex;
        for(x= 0; x <= ex; x++){
            y = (x*f)>>16;
            fr= (x*f)&0xFFFF;
            buf[ y   *stride + x]+= (color*(0x10000-fr))>>16;
            buf[(y+1)*stride + x]+= (color*         fr )>>16;
        }
    }else{
        if(sy > ey){
            FFSWAP(int, sx, ex);
            FFSWAP(int, sy, ey);
        }
        buf+= sx + sy*stride;
        ey-= sy;
        if(ey) f= ((ex-sx)<<16)/ey;
        else   f= 0;
        for(y= 0; y <= ey; y++){
            x = (y*f)>>16;
            fr= (y*f)&0xFFFF;
            buf[y*stride + x  ]+= (color*(0x10000-fr))>>16;
            buf[y*stride + x+1]+= (color*         fr )>>16;
        }
    }
}

/**
 * draws an arrow from (ex, ey) -> (sx, sy).
 * @param w width of the image
 * @param h height of the image
 * @param stride stride/linesize of the image
 * @param color color of the arrow
 */
static void draw_arrow(uint8_t *buf, int sx, int sy, int ex, int ey, int w, int h, int stride, int color){
    int dx,dy;

    sx= av_clip(sx, -100, w+100);
    sy= av_clip(sy, -100, h+100);
    ex= av_clip(ex, -100, w+100);
    ey= av_clip(ey, -100, h+100);

    dx= ex - sx;
    dy= ey - sy;

    if(dx*dx + dy*dy > 3*3){
        int rx=  dx + dy;
        int ry= -dx + dy;
        int length= ff_sqrt((rx*rx + ry*ry)<<8);

        //FIXME subpixel accuracy
        rx= ROUNDED_DIV(rx*3<<4, length);
        ry= ROUNDED_DIV(ry*3<<4, length);

        draw_line(buf, sx, sy, sx + rx, sy + ry, w, h, stride, color);
        draw_line(buf, sx, sy, sx - ry, sy + rx, w, h, stride, color);
    }
    draw_line(buf, sx, sy, ex, ey, w, h, stride, color);
}

/**
 * prints debuging info for the given picture.
 */
void ff_print_debug_info(MpegEncContext *s, AVFrame *pict){

    if(!pict || !pict->mb_type) return;

    if(s->avctx->debug&(FF_DEBUG_SKIP | FF_DEBUG_QP | FF_DEBUG_MB_TYPE)){
        int x,y;

        av_log(s->avctx,AV_LOG_DEBUG,"New frame, type: ");
        switch (pict->pict_type) {
            case FF_I_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"I\n"); break;
            case FF_P_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"P\n"); break;
            case FF_B_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"B\n"); break;
            case FF_S_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"S\n"); break;
            case FF_SI_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"SI\n"); break;
            case FF_SP_TYPE: av_log(s->avctx,AV_LOG_DEBUG,"SP\n"); break;
        }
        for(y=0; y<s->mb_height; y++){
            for(x=0; x<s->mb_width; x++){
                if(s->avctx->debug&FF_DEBUG_SKIP){
                    int count= s->mbskip_table[x + y*s->mb_stride];
                    if(count>9) count=9;
                    av_log(s->avctx, AV_LOG_DEBUG, "%1d", count);
                }
                if(s->avctx->debug&FF_DEBUG_QP){
                    av_log(s->avctx, AV_LOG_DEBUG, "%2d", pict->qscale_table[x + y*s->mb_stride]);
                }
                if(s->avctx->debug&FF_DEBUG_MB_TYPE){
                    int mb_type= pict->mb_type[x + y*s->mb_stride];
                    //Type & MV direction
                    if(IS_PCM(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "P");
                    else if(IS_INTRA(mb_type) && IS_ACPRED(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "A");
                    else if(IS_INTRA4x4(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "i");
                    else if(IS_INTRA16x16(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "I");
                    else if(IS_DIRECT(mb_type) && IS_SKIP(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "d");
                    else if(IS_DIRECT(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "D");
                    else if(IS_GMC(mb_type) && IS_SKIP(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "g");
                    else if(IS_GMC(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "G");
                    else if(IS_SKIP(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "S");
                    else if(!USES_LIST(mb_type, 1))
                        av_log(s->avctx, AV_LOG_DEBUG, ">");
                    else if(!USES_LIST(mb_type, 0))
                        av_log(s->avctx, AV_LOG_DEBUG, "<");
                    else{
                        assert(USES_LIST(mb_type, 0) && USES_LIST(mb_type, 1));
                        av_log(s->avctx, AV_LOG_DEBUG, "X");
                    }

                    //segmentation
                    if(IS_8X8(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "+");
                    else if(IS_16X8(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "-");
                    else if(IS_8X16(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, "|");
                    else if(IS_INTRA(mb_type) || IS_16X16(mb_type))
                        av_log(s->avctx, AV_LOG_DEBUG, " ");
                    else
                        av_log(s->avctx, AV_LOG_DEBUG, "?");


                    if(IS_INTERLACED(mb_type) && s->codec_id == CODEC_ID_H264)
                        av_log(s->avctx, AV_LOG_DEBUG, "=");
                    else
                        av_log(s->avctx, AV_LOG_DEBUG, " ");
                }
//                av_log(s->avctx, AV_LOG_DEBUG, " ");
            }
            av_log(s->avctx, AV_LOG_DEBUG, "\n");
        }
    }

    if((s->avctx->debug&(FF_DEBUG_VIS_QP|FF_DEBUG_VIS_MB_TYPE)) || (s->avctx->debug_mv)){
        const int shift= 1 + s->quarter_sample;
        int mb_y;
        uint8_t *ptr;
        int i;
        int h_chroma_shift, v_chroma_shift, block_height;
        const int width = s->avctx->width;
        const int height= s->avctx->height;
        const int mv_sample_log2= 4 - pict->motion_subsample_log2;
        const int mv_stride= (s->mb_width << mv_sample_log2) + (s->codec_id == CODEC_ID_H264 ? 0 : 1);
        s->low_delay=0; //needed to see the vectors without trashing the buffers

        avcodec_get_chroma_sub_sample(s->avctx->pix_fmt, &h_chroma_shift, &v_chroma_shift);
        for(i=0; i<3; i++){
            memcpy(s->visualization_buffer[i], pict->data[i], (i==0) ? pict->linesize[i]*height:pict->linesize[i]*height >> v_chroma_shift);
            pict->data[i]= s->visualization_buffer[i];
        }
        pict->type= FF_BUFFER_TYPE_COPY;
        ptr= pict->data[0];
        block_height = 16>>v_chroma_shift;

        for(mb_y=0; mb_y<s->mb_height; mb_y++){
            int mb_x;
            for(mb_x=0; mb_x<s->mb_width; mb_x++){
                const int mb_index= mb_x + mb_y*s->mb_stride;
                if((s->avctx->debug_mv) && pict->motion_val){
                  int type;
                  for(type=0; type<3; type++){
                    int direction = 0;
                    switch (type) {
                      case 0: if ((!(s->avctx->debug_mv&FF_DEBUG_VIS_MV_P_FOR)) || (pict->pict_type!=FF_P_TYPE))
                                continue;
                              direction = 0;
                              break;
                      case 1: if ((!(s->avctx->debug_mv&FF_DEBUG_VIS_MV_B_FOR)) || (pict->pict_type!=FF_B_TYPE))
                                continue;
                              direction = 0;
                              break;
                      case 2: if ((!(s->avctx->debug_mv&FF_DEBUG_VIS_MV_B_BACK)) || (pict->pict_type!=FF_B_TYPE))
                                continue;
                              direction = 1;
                              break;
                    }
                    if(!USES_LIST(pict->mb_type[mb_index], direction))
                        continue;

                    if(IS_8X8(pict->mb_type[mb_index])){
                      int i;
                      for(i=0; i<4; i++){
                        int sx= mb_x*16 + 4 + 8*(i&1);
                        int sy= mb_y*16 + 4 + 8*(i>>1);
                        int xy= (mb_x*2 + (i&1) + (mb_y*2 + (i>>1))*mv_stride) << (mv_sample_log2-1);
                        int mx= (pict->motion_val[direction][xy][0]>>shift) + sx;
                        int my= (pict->motion_val[direction][xy][1]>>shift) + sy;
                        draw_arrow(ptr, sx, sy, mx, my, width, height, s->linesize, 100);
                      }
                    }else if(IS_16X8(pict->mb_type[mb_index])){
                      int i;
                      for(i=0; i<2; i++){
                        int sx=mb_x*16 + 8;
                        int sy=mb_y*16 + 4 + 8*i;
                        int xy= (mb_x*2 + (mb_y*2 + i)*mv_stride) << (mv_sample_log2-1);
                        int mx=(pict->motion_val[direction][xy][0]>>shift);
                        int my=(pict->motion_val[direction][xy][1]>>shift);

                        if(IS_INTERLACED(pict->mb_type[mb_index]))
                            my*=2;

                        draw_arrow(ptr, sx, sy, mx+sx, my+sy, width, height, s->linesize, 100);
                      }
                    }else if(IS_8X16(pict->mb_type[mb_index])){
                      int i;
                      for(i=0; i<2; i++){
                        int sx=mb_x*16 + 4 + 8*i;
                        int sy=mb_y*16 + 8;
                        int xy= (mb_x*2 + i + mb_y*2*mv_stride) << (mv_sample_log2-1);
                        int mx=(pict->motion_val[direction][xy][0]>>shift);
                        int my=(pict->motion_val[direction][xy][1]>>shift);

                        if(IS_INTERLACED(pict->mb_type[mb_index]))
                            my*=2;

                        draw_arrow(ptr, sx, sy, mx+sx, my+sy, width, height, s->linesize, 100);
                      }
                    }else{
                      int sx= mb_x*16 + 8;
                      int sy= mb_y*16 + 8;
                      int xy= (mb_x + mb_y*mv_stride) << mv_sample_log2;
                      int mx= (pict->motion_val[direction][xy][0]>>shift) + sx;
                      int my= (pict->motion_val[direction][xy][1]>>shift) + sy;
                      draw_arrow(ptr, sx, sy, mx, my, width, height, s->linesize, 100);
                    }
                  }
                }
                if((s->avctx->debug&FF_DEBUG_VIS_QP) && pict->motion_val){
                    uint64_t c= (pict->qscale_table[mb_index]*128/31) * 0x0101010101010101ULL;
                    int y;
                    for(y=0; y<block_height; y++){
                        *(uint64_t*)(pict->data[1] + 8*mb_x + (block_height*mb_y + y)*pict->linesize[1])= c;
                        *(uint64_t*)(pict->data[2] + 8*mb_x + (block_height*mb_y + y)*pict->linesize[2])= c;
                    }
                }
                if((s->avctx->debug&FF_DEBUG_VIS_MB_TYPE) && pict->motion_val){
                    int mb_type= pict->mb_type[mb_index];
                    uint64_t u,v;
                    int y;
#define COLOR(theta, r)\
u= (int)(128 + r*cos(theta*3.141592/180));\
v= (int)(128 + r*sin(theta*3.141592/180));


                    u=v=128;
                    if(IS_PCM(mb_type)){
                        COLOR(120,48)
                    }else if((IS_INTRA(mb_type) && IS_ACPRED(mb_type)) || IS_INTRA16x16(mb_type)){
                        COLOR(30,48)
                    }else if(IS_INTRA4x4(mb_type)){
                        COLOR(90,48)
                    }else if(IS_DIRECT(mb_type) && IS_SKIP(mb_type)){
//                        COLOR(120,48)
                    }else if(IS_DIRECT(mb_type)){
                        COLOR(150,48)
                    }else if(IS_GMC(mb_type) && IS_SKIP(mb_type)){
                        COLOR(170,48)
                    }else if(IS_GMC(mb_type)){
                        COLOR(190,48)
                    }else if(IS_SKIP(mb_type)){
//                        COLOR(180,48)
                    }else if(!USES_LIST(mb_type, 1)){
                        COLOR(240,48)
                    }else if(!USES_LIST(mb_type, 0)){
                        COLOR(0,48)
                    }else{
                        assert(USES_LIST(mb_type, 0) && USES_LIST(mb_type, 1));
                        COLOR(300,48)
                    }

                    u*= 0x0101010101010101ULL;
                    v*= 0x0101010101010101ULL;
                    for(y=0; y<block_height; y++){
                        *(uint64_t*)(pict->data[1] + 8*mb_x + (block_height*mb_y + y)*pict->linesize[1])= u;
                        *(uint64_t*)(pict->data[2] + 8*mb_x + (block_height*mb_y + y)*pict->linesize[2])= v;
                    }

                    //segmentation
                    if(IS_8X8(mb_type) || IS_16X8(mb_type)){
                        *(uint64_t*)(pict->data[0] + 16*mb_x + 0 + (16*mb_y + 8)*pict->linesize[0])^= 0x8080808080808080ULL;
                        *(uint64_t*)(pict->data[0] + 16*mb_x + 8 + (16*mb_y + 8)*pict->linesize[0])^= 0x8080808080808080ULL;
                    }
                    if(IS_8X8(mb_type) || IS_8X16(mb_type)){
                        for(y=0; y<16; y++)
                            pict->data[0][16*mb_x + 8 + (16*mb_y + y)*pict->linesize[0]]^= 0x80;
                    }
                    if(IS_8X8(mb_type) && mv_sample_log2 >= 2){
                        int dm= 1 << (mv_sample_log2-2);
                        for(i=0; i<4; i++){
                            int sx= mb_x*16 + 8*(i&1);
                            int sy= mb_y*16 + 8*(i>>1);
                            int xy= (mb_x*2 + (i&1) + (mb_y*2 + (i>>1))*mv_stride) << (mv_sample_log2-1);
                            //FIXME bidir
                            int32_t *mv = (int32_t*)&pict->motion_val[0][xy];
                            if(mv[0] != mv[dm] || mv[dm*mv_stride] != mv[dm*(mv_stride+1)])
                                for(y=0; y<8; y++)
                                    pict->data[0][sx + 4 + (sy + y)*pict->linesize[0]]^= 0x80;
                            if(mv[0] != mv[dm*mv_stride] || mv[dm] != mv[dm*(mv_stride+1)])
                                *(uint64_t*)(pict->data[0] + sx + (sy + 4)*pict->linesize[0])^= 0x8080808080808080ULL;
                        }
                    }

                    if(IS_INTERLACED(mb_type) && s->codec_id == CODEC_ID_H264){
                        // hmm
                    }
                }
                s->mbskip_table[mb_index]=0;
            }
        }
    }
}

static inline int hpel_motion_lowres(MpegEncContext *s,
                                  uint8_t *dest, uint8_t *src,
                                  int field_based, int field_select,
                                  int src_x, int src_y,
                                  int width, int height, int stride,
                                  int h_edge_pos, int v_edge_pos,
                                  int w, int h, h264_chroma_mc_func *pix_op,
                                  int motion_x, int motion_y)
{
    const int lowres= s->avctx->lowres;
    const int s_mask= (2<<lowres)-1;
    int emu=0;
    int sx, sy;

    if(s->quarter_sample){
        motion_x/=2;
        motion_y/=2;
    }

    sx= motion_x & s_mask;
    sy= motion_y & s_mask;
    src_x += motion_x >> (lowres+1);
    src_y += motion_y >> (lowres+1);

    src += src_y * stride + src_x;

    if(   (unsigned)src_x > h_edge_pos                 - (!!sx) - w
       || (unsigned)src_y >(v_edge_pos >> field_based) - (!!sy) - h){
        ff_emulated_edge_mc(s->edge_emu_buffer, src, s->linesize, w+1, (h+1)<<field_based,
                            src_x, src_y<<field_based, h_edge_pos, v_edge_pos);
        src= s->edge_emu_buffer;
        emu=1;
    }

    sx <<= 2 - lowres;
    sy <<= 2 - lowres;
    if(field_select)
        src += s->linesize;
    pix_op[lowres](dest, src, stride, h, sx, sy);
    return emu;
}

/* apply one mpeg motion vector to the three components */
static av_always_inline void mpeg_motion_lowres(MpegEncContext *s,
                               uint8_t *dest_y, uint8_t *dest_cb, uint8_t *dest_cr,
                               int field_based, int bottom_field, int field_select,
                               uint8_t **ref_picture, h264_chroma_mc_func *pix_op,
                               int motion_x, int motion_y, int h)
{
    uint8_t *ptr_y, *ptr_cb, *ptr_cr;
    int mx, my, src_x, src_y, uvsrc_x, uvsrc_y, uvlinesize, linesize, sx, sy, uvsx, uvsy;
    const int lowres= s->avctx->lowres;
    const int block_s= 8>>lowres;
    const int s_mask= (2<<lowres)-1;
    const int h_edge_pos = s->h_edge_pos >> lowres;
    const int v_edge_pos = s->v_edge_pos >> lowres;
    linesize   = s->current_picture.linesize[0] << field_based;
    uvlinesize = s->current_picture.linesize[1] << field_based;

    if(s->quarter_sample){ //FIXME obviously not perfect but qpel will not work in lowres anyway
        motion_x/=2;
        motion_y/=2;
    }

    if(field_based){
        motion_y += (bottom_field - field_select)*((1<<lowres)-1);
    }

    sx= motion_x & s_mask;
    sy= motion_y & s_mask;
    src_x = s->mb_x*2*block_s               + (motion_x >> (lowres+1));
    src_y =(s->mb_y*2*block_s>>field_based) + (motion_y >> (lowres+1));

    if (s->out_format == FMT_H263) {
        uvsx = ((motion_x>>1) & s_mask) | (sx&1);
        uvsy = ((motion_y>>1) & s_mask) | (sy&1);
        uvsrc_x = src_x>>1;
        uvsrc_y = src_y>>1;
    }else if(s->out_format == FMT_H261){//even chroma mv's are full pel in H261
        mx = motion_x / 4;
        my = motion_y / 4;
        uvsx = (2*mx) & s_mask;
        uvsy = (2*my) & s_mask;
        uvsrc_x = s->mb_x*block_s               + (mx >> lowres);
        uvsrc_y = s->mb_y*block_s               + (my >> lowres);
    } else {
        mx = motion_x / 2;
        my = motion_y / 2;
        uvsx = mx & s_mask;
        uvsy = my & s_mask;
        uvsrc_x = s->mb_x*block_s               + (mx >> (lowres+1));
        uvsrc_y =(s->mb_y*block_s>>field_based) + (my >> (lowres+1));
    }

    ptr_y  = ref_picture[0] + src_y * linesize + src_x;
    ptr_cb = ref_picture[1] + uvsrc_y * uvlinesize + uvsrc_x;
    ptr_cr = ref_picture[2] + uvsrc_y * uvlinesize + uvsrc_x;

    if(   (unsigned)src_x > h_edge_pos                 - (!!sx) - 2*block_s
       || (unsigned)src_y >(v_edge_pos >> field_based) - (!!sy) - h){
            ff_emulated_edge_mc(s->edge_emu_buffer, ptr_y, s->linesize, 17, 17+field_based,
                             src_x, src_y<<field_based, h_edge_pos, v_edge_pos);
            ptr_y = s->edge_emu_buffer;
            if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
                uint8_t *uvbuf= s->edge_emu_buffer+18*s->linesize;
                ff_emulated_edge_mc(uvbuf  , ptr_cb, s->uvlinesize, 9, 9+field_based,
                                 uvsrc_x, uvsrc_y<<field_based, h_edge_pos>>1, v_edge_pos>>1);
                ff_emulated_edge_mc(uvbuf+16, ptr_cr, s->uvlinesize, 9, 9+field_based,
                                 uvsrc_x, uvsrc_y<<field_based, h_edge_pos>>1, v_edge_pos>>1);
                ptr_cb= uvbuf;
                ptr_cr= uvbuf+16;
            }
    }

    if(bottom_field){ //FIXME use this for field pix too instead of the obnoxious hack which changes picture.data
        dest_y += s->linesize;
        dest_cb+= s->uvlinesize;
        dest_cr+= s->uvlinesize;
    }

    if(field_select){
        ptr_y += s->linesize;
        ptr_cb+= s->uvlinesize;
        ptr_cr+= s->uvlinesize;
    }

    sx <<= 2 - lowres;
    sy <<= 2 - lowres;
    pix_op[lowres-1](dest_y, ptr_y, linesize, h, sx, sy);

    if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
        uvsx <<= 2 - lowres;
        uvsy <<= 2 - lowres;
        pix_op[lowres](dest_cb, ptr_cb, uvlinesize, h >> s->chroma_y_shift, uvsx, uvsy);
        pix_op[lowres](dest_cr, ptr_cr, uvlinesize, h >> s->chroma_y_shift, uvsx, uvsy);
    }
    //FIXME h261 lowres loop filter
}

static inline void chroma_4mv_motion_lowres(MpegEncContext *s,
                                     uint8_t *dest_cb, uint8_t *dest_cr,
                                     uint8_t **ref_picture,
                                     h264_chroma_mc_func *pix_op,
                                     int mx, int my){
    const int lowres= s->avctx->lowres;
    const int block_s= 8>>lowres;
    const int s_mask= (2<<lowres)-1;
    const int h_edge_pos = s->h_edge_pos >> (lowres+1);
    const int v_edge_pos = s->v_edge_pos >> (lowres+1);
    int emu=0, src_x, src_y, offset, sx, sy;
    uint8_t *ptr;

    if(s->quarter_sample){
        mx/=2;
        my/=2;
    }

    /* In case of 8X8, we construct a single chroma motion vector
       with a special rounding */
    mx= ff_h263_round_chroma(mx);
    my= ff_h263_round_chroma(my);

    sx= mx & s_mask;
    sy= my & s_mask;
    src_x = s->mb_x*block_s + (mx >> (lowres+1));
    src_y = s->mb_y*block_s + (my >> (lowres+1));

    offset = src_y * s->uvlinesize + src_x;
    ptr = ref_picture[1] + offset;
    if(s->flags&CODEC_FLAG_EMU_EDGE){
        if(   (unsigned)src_x > h_edge_pos - (!!sx) - block_s
           || (unsigned)src_y > v_edge_pos - (!!sy) - block_s){
            ff_emulated_edge_mc(s->edge_emu_buffer, ptr, s->uvlinesize, 9, 9, src_x, src_y, h_edge_pos, v_edge_pos);
            ptr= s->edge_emu_buffer;
            emu=1;
        }
    }
    sx <<= 2 - lowres;
    sy <<= 2 - lowres;
    pix_op[lowres](dest_cb, ptr, s->uvlinesize, block_s, sx, sy);

    ptr = ref_picture[2] + offset;
    if(emu){
        ff_emulated_edge_mc(s->edge_emu_buffer, ptr, s->uvlinesize, 9, 9, src_x, src_y, h_edge_pos, v_edge_pos);
        ptr= s->edge_emu_buffer;
    }
    pix_op[lowres](dest_cr, ptr, s->uvlinesize, block_s, sx, sy);
}

/**
 * motion compensation of a single macroblock
 * @param s context
 * @param dest_y luma destination pointer
 * @param dest_cb chroma cb/u destination pointer
 * @param dest_cr chroma cr/v destination pointer
 * @param dir direction (0->forward, 1->backward)
 * @param ref_picture array[3] of pointers to the 3 planes of the reference picture
 * @param pic_op halfpel motion compensation function (average or put normally)
 * the motion vectors are taken from s->mv and the MV type from s->mv_type
 */
static inline void MPV_motion_lowres(MpegEncContext *s,
                              uint8_t *dest_y, uint8_t *dest_cb, uint8_t *dest_cr,
                              int dir, uint8_t **ref_picture,
                              h264_chroma_mc_func *pix_op)
{
    int mx, my;
    int mb_x, mb_y, i;
    const int lowres= s->avctx->lowres;
    const int block_s= 8>>lowres;

    mb_x = s->mb_x;
    mb_y = s->mb_y;

    switch(s->mv_type) {
    case MV_TYPE_16X16:
        mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                    0, 0, 0,
                    ref_picture, pix_op,
                    s->mv[dir][0][0], s->mv[dir][0][1], 2*block_s);
        break;
    case MV_TYPE_8X8:
        mx = 0;
        my = 0;
            for(i=0;i<4;i++) {
                hpel_motion_lowres(s, dest_y + ((i & 1) + (i >> 1) * s->linesize)*block_s,
                            ref_picture[0], 0, 0,
                            (2*mb_x + (i & 1))*block_s, (2*mb_y + (i >>1))*block_s,
                            s->width, s->height, s->linesize,
                            s->h_edge_pos >> lowres, s->v_edge_pos >> lowres,
                            block_s, block_s, pix_op,
                            s->mv[dir][i][0], s->mv[dir][i][1]);

                mx += s->mv[dir][i][0];
                my += s->mv[dir][i][1];
            }

        if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY))
            chroma_4mv_motion_lowres(s, dest_cb, dest_cr, ref_picture, pix_op, mx, my);
        break;
    case MV_TYPE_FIELD:
        if (s->picture_structure == PICT_FRAME) {
            /* top field */
            mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                        1, 0, s->field_select[dir][0],
                        ref_picture, pix_op,
                        s->mv[dir][0][0], s->mv[dir][0][1], block_s);
            /* bottom field */
            mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                        1, 1, s->field_select[dir][1],
                        ref_picture, pix_op,
                        s->mv[dir][1][0], s->mv[dir][1][1], block_s);
        } else {
            if(s->picture_structure != s->field_select[dir][0] + 1 && s->pict_type != FF_B_TYPE && !s->first_field){
                ref_picture= s->current_picture_ptr->data;
            }

            mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                        0, 0, s->field_select[dir][0],
                        ref_picture, pix_op,
                        s->mv[dir][0][0], s->mv[dir][0][1], 2*block_s);
        }
        break;
    case MV_TYPE_16X8:
        for(i=0; i<2; i++){
            uint8_t ** ref2picture;

            if(s->picture_structure == s->field_select[dir][i] + 1 || s->pict_type == FF_B_TYPE || s->first_field){
                ref2picture= ref_picture;
            }else{
                ref2picture= s->current_picture_ptr->data;
            }

            mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                        0, 0, s->field_select[dir][i],
                        ref2picture, pix_op,
                        s->mv[dir][i][0], s->mv[dir][i][1] + 2*block_s*i, block_s);

            dest_y += 2*block_s*s->linesize;
            dest_cb+= (2*block_s>>s->chroma_y_shift)*s->uvlinesize;
            dest_cr+= (2*block_s>>s->chroma_y_shift)*s->uvlinesize;
        }
        break;
    case MV_TYPE_DMV:
        if(s->picture_structure == PICT_FRAME){
            for(i=0; i<2; i++){
                int j;
                for(j=0; j<2; j++){
                    mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                                1, j, j^i,
                                ref_picture, pix_op,
                                s->mv[dir][2*i + j][0], s->mv[dir][2*i + j][1], block_s);
                }
                pix_op = s->dsp.avg_h264_chroma_pixels_tab;
            }
        }else{
            for(i=0; i<2; i++){
                mpeg_motion_lowres(s, dest_y, dest_cb, dest_cr,
                            0, 0, s->picture_structure != i+1,
                            ref_picture, pix_op,
                            s->mv[dir][2*i][0],s->mv[dir][2*i][1],2*block_s);

                // after put we make avg of the same block
                pix_op = s->dsp.avg_h264_chroma_pixels_tab;

                //opposite parity is always in the same frame if this is second field
                if(!s->first_field){
                    ref_picture = s->current_picture_ptr->data;
                }
            }
        }
    break;
    default: assert(0);
    }
}

/* put block[] to dest[] */
static inline void put_dct(MpegEncContext *s,
                           DCTELEM *block, int i, uint8_t *dest, int line_size, int qscale)
{
    s->dct_unquantize_intra(s, block, i, qscale);
    s->dsp.idct_put (dest, line_size, block);
}

/* add block[] to dest[] */
static inline void add_dct(MpegEncContext *s,
                           DCTELEM *block, int i, uint8_t *dest, int line_size)
{
    if (s->block_last_index[i] >= 0) {
        s->dsp.idct_add (dest, line_size, block);
    }
}

static inline void add_dequant_dct(MpegEncContext *s,
                           DCTELEM *block, int i, uint8_t *dest, int line_size, int qscale)
{
    if (s->block_last_index[i] >= 0) {
        s->dct_unquantize_inter(s, block, i, qscale);

        s->dsp.idct_add (dest, line_size, block);
    }
}

/**
 * cleans dc, ac, coded_block for the current non intra MB
 */
void ff_clean_intra_table_entries(MpegEncContext *s)
{
    int wrap = s->b8_stride;
    int xy = s->block_index[0];

    s->dc_val[0][xy           ] =
    s->dc_val[0][xy + 1       ] =
    s->dc_val[0][xy     + wrap] =
    s->dc_val[0][xy + 1 + wrap] = 1024;
    /* ac pred */
    memset(s->ac_val[0][xy       ], 0, 32 * sizeof(int16_t));
    memset(s->ac_val[0][xy + wrap], 0, 32 * sizeof(int16_t));
    if (s->msmpeg4_version>=3) {
        s->coded_block[xy           ] =
        s->coded_block[xy + 1       ] =
        s->coded_block[xy     + wrap] =
        s->coded_block[xy + 1 + wrap] = 0;
    }
    /* chroma */
    wrap = s->mb_stride;
    xy = s->mb_x + s->mb_y * wrap;
    s->dc_val[1][xy] =
    s->dc_val[2][xy] = 1024;
    /* ac pred */
    memset(s->ac_val[1][xy], 0, 16 * sizeof(int16_t));
    memset(s->ac_val[2][xy], 0, 16 * sizeof(int16_t));

    s->mbintra_table[xy]= 0;
}

/* generic function called after a macroblock has been parsed by the
   decoder or after it has been encoded by the encoder.

   Important variables used:
   s->mb_intra : true if intra macroblock
   s->mv_dir   : motion vector direction
   s->mv_type  : motion vector type
   s->mv       : motion vector
   s->interlaced_dct : true if interlaced dct used (mpeg2)
 */
static av_always_inline
void MPV_decode_mb_internal(MpegEncContext *s, DCTELEM block[12][64],
                            int lowres_flag, int is_mpeg12)
{
    int mb_x, mb_y;
    const int mb_xy = s->mb_y * s->mb_stride + s->mb_x;
#ifdef HAVE_XVMC
    if(s->avctx->xvmc_acceleration){
        XVMC_decode_mb(s);//xvmc uses pblocks
        return;
    }
#endif

    mb_x = s->mb_x;
    mb_y = s->mb_y;

    if(s->avctx->debug&FF_DEBUG_DCT_COEFF) {
       /* save DCT coefficients */
       int i,j;
       DCTELEM *dct = &s->current_picture.dct_coeff[mb_xy*64*6];
       for(i=0; i<6; i++)
           for(j=0; j<64; j++)
               *dct++ = block[i][s->dsp.idct_permutation[j]];
    }

    s->current_picture.qscale_table[mb_xy]= s->qscale;

    /* update DC predictors for P macroblocks */
    if (!s->mb_intra) {
        if (!is_mpeg12 && (s->h263_pred || s->h263_aic)) {
            if(s->mbintra_table[mb_xy])
                ff_clean_intra_table_entries(s);
        } else {
            s->last_dc[0] =
            s->last_dc[1] =
            s->last_dc[2] = 128 << s->intra_dc_precision;
        }
    }
    else if (!is_mpeg12 && (s->h263_pred || s->h263_aic))
        s->mbintra_table[mb_xy]=1;

    if ((s->flags&CODEC_FLAG_PSNR) || !(s->encoding && (s->intra_only || s->pict_type==FF_B_TYPE) && s->avctx->mb_decision != FF_MB_DECISION_RD)) { //FIXME precalc
        uint8_t *dest_y, *dest_cb, *dest_cr;
        int dct_linesize, dct_offset;
        op_pixels_func (*op_pix)[4];
        qpel_mc_func (*op_qpix)[16];
        const int linesize= s->current_picture.linesize[0]; //not s->linesize as this would be wrong for field pics
        const int uvlinesize= s->current_picture.linesize[1];
        const int readable= s->pict_type != FF_B_TYPE || s->encoding || s->avctx->draw_horiz_band || lowres_flag;
        const int block_size= lowres_flag ? 8>>s->avctx->lowres : 8;

        /* avoid copy if macroblock skipped in last frame too */
        /* skip only during decoding as we might trash the buffers during encoding a bit */
        if(!s->encoding){
            uint8_t *mbskip_ptr = &s->mbskip_table[mb_xy];
            const int age= s->current_picture.age;

            assert(age);

            if (s->mb_skipped) {
                s->mb_skipped= 0;
                assert(s->pict_type!=FF_I_TYPE);

                (*mbskip_ptr) ++; /* indicate that this time we skipped it */
                if(*mbskip_ptr >99) *mbskip_ptr= 99;

                /* if previous was skipped too, then nothing to do !  */
                if (*mbskip_ptr >= age && s->current_picture.reference){
                    return;
                }
            } else if(!s->current_picture.reference){
                (*mbskip_ptr) ++; /* increase counter so the age can be compared cleanly */
                if(*mbskip_ptr >99) *mbskip_ptr= 99;
            } else{
                *mbskip_ptr = 0; /* not skipped */
            }
        }

        dct_linesize = linesize << s->interlaced_dct;
        dct_offset =(s->interlaced_dct)? linesize : linesize*block_size;

        if(readable){
            dest_y=  s->dest[0];
            dest_cb= s->dest[1];
            dest_cr= s->dest[2];
        }else{
            dest_y = s->b_scratchpad;
            dest_cb= s->b_scratchpad+16*linesize;
            dest_cr= s->b_scratchpad+32*linesize;
        }

        if (!s->mb_intra) {
            /* motion handling */
            /* decoding or more than one mb_type (MC was already done otherwise) */
            if(!s->encoding){
                if(lowres_flag){
                    h264_chroma_mc_func *op_pix = s->dsp.put_h264_chroma_pixels_tab;

                    if (s->mv_dir & MV_DIR_FORWARD) {
                        MPV_motion_lowres(s, dest_y, dest_cb, dest_cr, 0, s->last_picture.data, op_pix);
                        op_pix = s->dsp.avg_h264_chroma_pixels_tab;
                    }
                    if (s->mv_dir & MV_DIR_BACKWARD) {
                        MPV_motion_lowres(s, dest_y, dest_cb, dest_cr, 1, s->next_picture.data, op_pix);
                    }
                }else{
                    op_qpix= s->me.qpel_put;
                    if ((!s->no_rounding) || s->pict_type==FF_B_TYPE){
                        op_pix = s->dsp.put_pixels_tab;
                    }else{
                        op_pix = s->dsp.put_no_rnd_pixels_tab;
                    }
                    if (s->mv_dir & MV_DIR_FORWARD) {
                        MPV_motion(s, dest_y, dest_cb, dest_cr, 0, s->last_picture.data, op_pix, op_qpix);
                        op_pix = s->dsp.avg_pixels_tab;
                        op_qpix= s->me.qpel_avg;
                    }
                    if (s->mv_dir & MV_DIR_BACKWARD) {
                        MPV_motion(s, dest_y, dest_cb, dest_cr, 1, s->next_picture.data, op_pix, op_qpix);
                    }
                }
            }

            /* skip dequant / idct if we are really late ;) */
            if(s->hurry_up>1) goto skip_idct;
            if(s->avctx->skip_idct){
                if(  (s->avctx->skip_idct >= AVDISCARD_NONREF && s->pict_type == FF_B_TYPE)
                   ||(s->avctx->skip_idct >= AVDISCARD_NONKEY && s->pict_type != FF_I_TYPE)
                   || s->avctx->skip_idct >= AVDISCARD_ALL)
                    goto skip_idct;
            }

            /* add dct residue */
            if(s->encoding || !(   s->h263_msmpeg4 || s->codec_id==CODEC_ID_MPEG1VIDEO || s->codec_id==CODEC_ID_MPEG2VIDEO
                                || (s->codec_id==CODEC_ID_MPEG4 && !s->mpeg_quant))){
                add_dequant_dct(s, block[0], 0, dest_y                          , dct_linesize, s->qscale);
                add_dequant_dct(s, block[1], 1, dest_y              + block_size, dct_linesize, s->qscale);
                add_dequant_dct(s, block[2], 2, dest_y + dct_offset             , dct_linesize, s->qscale);
                add_dequant_dct(s, block[3], 3, dest_y + dct_offset + block_size, dct_linesize, s->qscale);

                if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
                    if (s->chroma_y_shift){
                        add_dequant_dct(s, block[4], 4, dest_cb, uvlinesize, s->chroma_qscale);
                        add_dequant_dct(s, block[5], 5, dest_cr, uvlinesize, s->chroma_qscale);
                    }else{
                        dct_linesize >>= 1;
                        dct_offset >>=1;
                        add_dequant_dct(s, block[4], 4, dest_cb,              dct_linesize, s->chroma_qscale);
                        add_dequant_dct(s, block[5], 5, dest_cr,              dct_linesize, s->chroma_qscale);
                        add_dequant_dct(s, block[6], 6, dest_cb + dct_offset, dct_linesize, s->chroma_qscale);
                        add_dequant_dct(s, block[7], 7, dest_cr + dct_offset, dct_linesize, s->chroma_qscale);
                    }
                }
            } else if(is_mpeg12 || (s->codec_id != CODEC_ID_WMV2)){
                add_dct(s, block[0], 0, dest_y                          , dct_linesize);
                add_dct(s, block[1], 1, dest_y              + block_size, dct_linesize);
                add_dct(s, block[2], 2, dest_y + dct_offset             , dct_linesize);
                add_dct(s, block[3], 3, dest_y + dct_offset + block_size, dct_linesize);

                if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
                    if(s->chroma_y_shift){//Chroma420
                        add_dct(s, block[4], 4, dest_cb, uvlinesize);
                        add_dct(s, block[5], 5, dest_cr, uvlinesize);
                    }else{
                        //chroma422
                        dct_linesize = uvlinesize << s->interlaced_dct;
                        dct_offset =(s->interlaced_dct)? uvlinesize : uvlinesize*8;

                        add_dct(s, block[4], 4, dest_cb, dct_linesize);
                        add_dct(s, block[5], 5, dest_cr, dct_linesize);
                        add_dct(s, block[6], 6, dest_cb+dct_offset, dct_linesize);
                        add_dct(s, block[7], 7, dest_cr+dct_offset, dct_linesize);
                        if(!s->chroma_x_shift){//Chroma444
                            add_dct(s, block[8], 8, dest_cb+8, dct_linesize);
                            add_dct(s, block[9], 9, dest_cr+8, dct_linesize);
                            add_dct(s, block[10], 10, dest_cb+8+dct_offset, dct_linesize);
                            add_dct(s, block[11], 11, dest_cr+8+dct_offset, dct_linesize);
                        }
                    }
                }//fi gray
            }
            else if (ENABLE_WMV2) {
                ff_wmv2_add_mb(s, block, dest_y, dest_cb, dest_cr);
            }
        } else {
            /* dct only in intra block */
            if(s->encoding || !(s->codec_id==CODEC_ID_MPEG1VIDEO || s->codec_id==CODEC_ID_MPEG2VIDEO)){
                put_dct(s, block[0], 0, dest_y                          , dct_linesize, s->qscale);
                put_dct(s, block[1], 1, dest_y              + block_size, dct_linesize, s->qscale);
                put_dct(s, block[2], 2, dest_y + dct_offset             , dct_linesize, s->qscale);
                put_dct(s, block[3], 3, dest_y + dct_offset + block_size, dct_linesize, s->qscale);

                if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
                    if(s->chroma_y_shift){
                        put_dct(s, block[4], 4, dest_cb, uvlinesize, s->chroma_qscale);
                        put_dct(s, block[5], 5, dest_cr, uvlinesize, s->chroma_qscale);
                    }else{
                        dct_offset >>=1;
                        dct_linesize >>=1;
                        put_dct(s, block[4], 4, dest_cb,              dct_linesize, s->chroma_qscale);
                        put_dct(s, block[5], 5, dest_cr,              dct_linesize, s->chroma_qscale);
                        put_dct(s, block[6], 6, dest_cb + dct_offset, dct_linesize, s->chroma_qscale);
                        put_dct(s, block[7], 7, dest_cr + dct_offset, dct_linesize, s->chroma_qscale);
                    }
                }
            }else{
                s->dsp.idct_put(dest_y                          , dct_linesize, block[0]);
                s->dsp.idct_put(dest_y              + block_size, dct_linesize, block[1]);
                s->dsp.idct_put(dest_y + dct_offset             , dct_linesize, block[2]);
                s->dsp.idct_put(dest_y + dct_offset + block_size, dct_linesize, block[3]);

                if(!ENABLE_GRAY || !(s->flags&CODEC_FLAG_GRAY)){
                    if(s->chroma_y_shift){
                        s->dsp.idct_put(dest_cb, uvlinesize, block[4]);
                        s->dsp.idct_put(dest_cr, uvlinesize, block[5]);
                    }else{

                        dct_linesize = uvlinesize << s->interlaced_dct;
                        dct_offset =(s->interlaced_dct)? uvlinesize : uvlinesize*8;

                        s->dsp.idct_put(dest_cb,              dct_linesize, block[4]);
                        s->dsp.idct_put(dest_cr,              dct_linesize, block[5]);
                        s->dsp.idct_put(dest_cb + dct_offset, dct_linesize, block[6]);
                        s->dsp.idct_put(dest_cr + dct_offset, dct_linesize, block[7]);
                        if(!s->chroma_x_shift){//Chroma444
                            s->dsp.idct_put(dest_cb + 8,              dct_linesize, block[8]);
                            s->dsp.idct_put(dest_cr + 8,              dct_linesize, block[9]);
                            s->dsp.idct_put(dest_cb + 8 + dct_offset, dct_linesize, block[10]);
                            s->dsp.idct_put(dest_cr + 8 + dct_offset, dct_linesize, block[11]);
                        }
                    }
                }//gray
            }
        }
skip_idct:
        if(!readable){
            s->dsp.put_pixels_tab[0][0](s->dest[0], dest_y ,   linesize,16);
            s->dsp.put_pixels_tab[s->chroma_x_shift][0](s->dest[1], dest_cb, uvlinesize,16 >> s->chroma_y_shift);
            s->dsp.put_pixels_tab[s->chroma_x_shift][0](s->dest[2], dest_cr, uvlinesize,16 >> s->chroma_y_shift);
        }
    }
}

void MPV_decode_mb(MpegEncContext *s, DCTELEM block[12][64]){
#ifndef CONFIG_SMALL
    if(s->out_format == FMT_MPEG1) {
        if(s->avctx->lowres) MPV_decode_mb_internal(s, block, 1, 1);
        else                 MPV_decode_mb_internal(s, block, 0, 1);
    } else
#endif
    if(s->avctx->lowres) MPV_decode_mb_internal(s, block, 1, 0);
    else                  MPV_decode_mb_internal(s, block, 0, 0);
}

/**
 *
 * @param h is the normal height, this will be reduced automatically if needed for the last row
 */
void ff_draw_horiz_band(MpegEncContext *s, int y, int h){
    if (s->avctx->draw_horiz_band) {
        AVFrame *src;
        int offset[4];

        if(s->picture_structure != PICT_FRAME){
            h <<= 1;
            y <<= 1;
            if(s->first_field  && !(s->avctx->slice_flags&SLICE_FLAG_ALLOW_FIELD)) return;
        }

        h= FFMIN(h, s->avctx->height - y);

        if(s->pict_type==FF_B_TYPE || s->low_delay || (s->avctx->slice_flags&SLICE_FLAG_CODED_ORDER))
            src= (AVFrame*)s->current_picture_ptr;
        else if(s->last_picture_ptr)
            src= (AVFrame*)s->last_picture_ptr;
        else
            return;

        if(s->pict_type==FF_B_TYPE && s->picture_structure == PICT_FRAME && s->out_format != FMT_H264){
            offset[0]=
            offset[1]=
            offset[2]=
            offset[3]= 0;
        }else{
            offset[0]= y * s->linesize;
            offset[1]=
            offset[2]= (y >> s->chroma_y_shift) * s->uvlinesize;
            offset[3]= 0;
        }

        emms_c();

        s->avctx->draw_horiz_band(s->avctx, src, offset,
                                  y, s->picture_structure, h);
    }
}

void ff_init_block_index(MpegEncContext *s){ //FIXME maybe rename
    const int linesize= s->current_picture.linesize[0]; //not s->linesize as this would be wrong for field pics
    const int uvlinesize= s->current_picture.linesize[1];
    const int mb_size= 4 - s->avctx->lowres;

    s->block_index[0]= s->b8_stride*(s->mb_y*2    ) - 2 + s->mb_x*2;
    s->block_index[1]= s->b8_stride*(s->mb_y*2    ) - 1 + s->mb_x*2;
    s->block_index[2]= s->b8_stride*(s->mb_y*2 + 1) - 2 + s->mb_x*2;
    s->block_index[3]= s->b8_stride*(s->mb_y*2 + 1) - 1 + s->mb_x*2;
    s->block_index[4]= s->mb_stride*(s->mb_y + 1)                + s->b8_stride*s->mb_height*2 + s->mb_x - 1;
    s->block_index[5]= s->mb_stride*(s->mb_y + s->mb_height + 2) + s->b8_stride*s->mb_height*2 + s->mb_x - 1;
    //block_index is not used by mpeg2, so it is not affected by chroma_format

    s->dest[0] = s->current_picture.data[0] + ((s->mb_x - 1) << mb_size);
    s->dest[1] = s->current_picture.data[1] + ((s->mb_x - 1) << (mb_size - s->chroma_x_shift));
    s->dest[2] = s->current_picture.data[2] + ((s->mb_x - 1) << (mb_size - s->chroma_x_shift));

    if(!(s->pict_type==FF_B_TYPE && s->avctx->draw_horiz_band && s->picture_structure==PICT_FRAME))
    {
        s->dest[0] += s->mb_y *   linesize << mb_size;
        s->dest[1] += s->mb_y * uvlinesize << (mb_size - s->chroma_y_shift);
        s->dest[2] += s->mb_y * uvlinesize << (mb_size - s->chroma_y_shift);
    }
}

void ff_mpeg_flush(AVCodecContext *avctx){
    int i;
    MpegEncContext *s = avctx->priv_data;

    if(s==NULL || s->picture==NULL)
        return;

    for(i=0; i<MAX_PICTURE_COUNT; i++){
       if(s->picture[i].data[0] && (   s->picture[i].type == FF_BUFFER_TYPE_INTERNAL
                                    || s->picture[i].type == FF_BUFFER_TYPE_USER))
        avctx->release_buffer(avctx, (AVFrame*)&s->picture[i]);
    }
    s->current_picture_ptr = s->last_picture_ptr = s->next_picture_ptr = NULL;

    s->mb_x= s->mb_y= 0;

    s->parse_context.state= -1;
    s->parse_context.frame_start_found= 0;
    s->parse_context.overread= 0;
    s->parse_context.overread_index= 0;
    s->parse_context.index= 0;
    s->parse_context.last_index= 0;
    s->bitstream_buffer_size=0;
    s->pp_time=0;
}

static void dct_unquantize_mpeg1_intra_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale)
{
    int i, level, nCoeffs;
    const uint16_t *quant_matrix;

    nCoeffs= s->block_last_index[n];

    if (n < 4)
        block[0] = block[0] * s->y_dc_scale;
    else
        block[0] = block[0] * s->c_dc_scale;
    /* XXX: only mpeg1 */
    quant_matrix = s->intra_matrix;
    for(i=1;i<=nCoeffs;i++) {
        int j= s->intra_scantable.permutated[i];
        level = block[j];
        if (level) {
            if (level < 0) {
                level = -level;
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
                level = (level - 1) | 1;
                level = -level;
            } else {
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
                level = (level - 1) | 1;
            }
            block[j] = level;
        }
    }
}

static void dct_unquantize_mpeg1_inter_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale)
{
    int i, level, nCoeffs;
    const uint16_t *quant_matrix;

    nCoeffs= s->block_last_index[n];

    quant_matrix = s->inter_matrix;
    for(i=0; i<=nCoeffs; i++) {
        int j= s->intra_scantable.permutated[i];
        level = block[j];
        if (level) {
            if (level < 0) {
                level = -level;
                level = (((level << 1) + 1) * qscale *
                         ((int) (quant_matrix[j]))) >> 4;
                level = (level - 1) | 1;
                level = -level;
            } else {
                level = (((level << 1) + 1) * qscale *
                         ((int) (quant_matrix[j]))) >> 4;
                level = (level - 1) | 1;
            }
            block[j] = level;
        }
    }
}

static void dct_unquantize_mpeg2_intra_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale)
{
    int i, level, nCoeffs;
    const uint16_t *quant_matrix;

    if(s->alternate_scan) nCoeffs= 63;
    else nCoeffs= s->block_last_index[n];

    if (n < 4)
        block[0] = block[0] * s->y_dc_scale;
    else
        block[0] = block[0] * s->c_dc_scale;
    quant_matrix = s->intra_matrix;
    for(i=1;i<=nCoeffs;i++) {
        int j= s->intra_scantable.permutated[i];
        level = block[j];
        if (level) {
            if (level < 0) {
                level = -level;
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
                level = -level;
            } else {
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
            }
            block[j] = level;
        }
    }
}

static void dct_unquantize_mpeg2_intra_bitexact(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale)
{
    int i, level, nCoeffs;
    const uint16_t *quant_matrix;
    int sum=-1;

    if(s->alternate_scan) nCoeffs= 63;
    else nCoeffs= s->block_last_index[n];

    if (n < 4)
        block[0] = block[0] * s->y_dc_scale;
    else
        block[0] = block[0] * s->c_dc_scale;
    quant_matrix = s->intra_matrix;
    for(i=1;i<=nCoeffs;i++) {
        int j= s->intra_scantable.permutated[i];
        level = block[j];
        if (level) {
            if (level < 0) {
                level = -level;
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
                level = -level;
            } else {
                level = (int)(level * qscale * quant_matrix[j]) >> 3;
            }
            block[j] = level;
            sum+=level;
        }
    }
    block[63]^=sum&1;
}

static void dct_unquantize_mpeg2_inter_c(MpegEncContext *s,
                                   DCTELEM *block, int n, int qscale)
{
    int i, level, nCoeffs;
    const uint16_t *quant_matrix;
    int sum=-1;

    if(s->alternate_scan) nCoeffs= 63;
    else nCoeffs= s->block_last_index[n];

    quant_matrix = s->inter_matrix;
    for(i=0; i<=nCoeffs; i++) {
        int j= s->intra_scantable.permutated[i];
        level = block[j];
        if (level) {
            if (level < 0) {
                level = -level;
                level = (((level << 1) + 1) * qscale *
                         ((int) (quant_matrix[j]))) >> 4;
                level = -level;
            } else {
                level = (((level << 1) + 1) * qscale *
                         ((int) (quant_matrix[j]))) >> 4;
            }
            block[j] = level;
            sum+=level;
        }
    }
    block[63]^=sum&1;
}

static void dct_unquantize_h263_intra_c(MpegEncContext *s,
                                  DCTELEM *block, int n, int qscale)
{
    int i, level, qmul, qadd;
    int nCoeffs;

    assert(s->block_last_index[n]>=0);

    qmul = qscale << 1;

    if (!s->h263_aic) {
        if (n < 4)
            block[0] = block[0] * s->y_dc_scale;
        else
            block[0] = block[0] * s->c_dc_scale;
        qadd = (qscale - 1) | 1;
    }else{
        qadd = 0;
    }
    if(s->ac_pred)
        nCoeffs=63;
    else
        nCoeffs= s->inter_scantable.raster_end[ s->block_last_index[n] ];

    for(i=1; i<=nCoeffs; i++) {
        level = block[i];
        if (level) {
            if (level < 0) {
                level = level * qmul - qadd;
            } else {
                level = level * qmul + qadd;
            }
            block[i] = level;
        }
    }
}

static void dct_unquantize_h263_inter_c(MpegEncContext *s,
                                  DCTELEM *block, int n, int qscale)
{
    int i, level, qmul, qadd;
    int nCoeffs;

    assert(s->block_last_index[n]>=0);

    qadd = (qscale - 1) | 1;
    qmul = qscale << 1;

    nCoeffs= s->inter_scantable.raster_end[ s->block_last_index[n] ];

    for(i=0; i<=nCoeffs; i++) {
        level = block[i];
        if (level) {
            if (level < 0) {
                level = level * qmul - qadd;
            } else {
                level = level * qmul + qadd;
            }
            block[i] = level;
        }
    }
}

/**
 * set qscale and update qscale dependent variables.
 */
void ff_set_qscale(MpegEncContext * s, int qscale)
{
    if (qscale < 1)
        qscale = 1;
    else if (qscale > 31)
        qscale = 31;

    s->qscale = qscale;
    s->chroma_qscale= s->chroma_qscale_table[qscale];

    s->y_dc_scale= s->y_dc_scale_table[ qscale ];
    s->c_dc_scale= s->c_dc_scale_table[ s->chroma_qscale ];
}
