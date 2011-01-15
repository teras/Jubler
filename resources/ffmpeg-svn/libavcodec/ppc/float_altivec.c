/*
 * Copyright (c) 2006 Luca Barbato <lu_zero@gentoo.org>
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

#include "libavcodec/dsputil.h"

#include "gcc_fixes.h"

#include "dsputil_altivec.h"

static void vector_fmul_altivec(float *dst, const float *src, int len)
{
    int i;
    vector float d0, d1, s, zero = (vector float)vec_splat_u32(0);
    for(i=0; i<len-7; i+=8) {
        d0 = vec_ld(0, dst+i);
        s = vec_ld(0, src+i);
        d1 = vec_ld(16, dst+i);
        d0 = vec_madd(d0, s, zero);
        d1 = vec_madd(d1, vec_ld(16,src+i), zero);
        vec_st(d0, 0, dst+i);
        vec_st(d1, 16, dst+i);
    }
}

static void vector_fmul_reverse_altivec(float *dst, const float *src0,
                                        const float *src1, int len)
{
    int i;
    vector float d, s0, s1, h0, l0,
                 s2, s3, zero = (vector float)vec_splat_u32(0);
    src1 += len-4;
    for(i=0; i<len-7; i+=8) {
        s1 = vec_ld(0, src1-i);              // [a,b,c,d]
        s0 = vec_ld(0, src0+i);
        l0 = vec_mergel(s1, s1);             // [c,c,d,d]
        s3 = vec_ld(-16, src1-i);
        h0 = vec_mergeh(s1, s1);             // [a,a,b,b]
        s2 = vec_ld(16, src0+i);
        s1 = vec_mergeh(vec_mergel(l0,h0),   // [d,b,d,b]
                        vec_mergeh(l0,h0));  // [c,a,c,a]
                                             // [d,c,b,a]
        l0 = vec_mergel(s3, s3);
        d = vec_madd(s0, s1, zero);
        h0 = vec_mergeh(s3, s3);
        vec_st(d, 0, dst+i);
        s3 = vec_mergeh(vec_mergel(l0,h0),
                        vec_mergeh(l0,h0));
        d = vec_madd(s2, s3, zero);
        vec_st(d, 16, dst+i);
    }
}

static void vector_fmul_add_add_altivec(float *dst, const float *src0,
                                        const float *src1, const float *src2,
                                        int src3, int len, int step)
{
    int i;
    vector float d, s0, s1, s2, t0, t1, edges;
    vector unsigned char align = vec_lvsr(0,dst),
                         mask = vec_lvsl(0, dst);

#if 0 //FIXME: there is still something wrong
    if (step == 2) {
        int y;
        vector float d0, d1, s3, t2;
        vector unsigned int sel =
                vec_mergeh(vec_splat_u32(-1), vec_splat_u32(0));
        t1 = vec_ld(16, dst);
        for (i=0,y=0; i<len-3; i+=4,y+=8) {

            s0 = vec_ld(0,src0+i);
            s1 = vec_ld(0,src1+i);
            s2 = vec_ld(0,src2+i);

//          t0 = vec_ld(0, dst+y);  //[x x x|a]
//          t1 = vec_ld(16, dst+y); //[b c d|e]
            t2 = vec_ld(31, dst+y); //[f g h|x]

            d = vec_madd(s0,s1,s2); // [A B C D]

                                                 // [A A B B]

                                                 // [C C D D]

            d0 = vec_perm(t0, t1, mask); // [a b c d]

            d0 = vec_sel(vec_mergeh(d, d), d0, sel);   // [A b B d]

            edges = vec_perm(t1, t0, mask);

            t0 = vec_perm(edges, d0, align); // [x x x|A]

            t1 = vec_perm(d0, edges, align); // [b B d|e]

            vec_stl(t0, 0, dst+y);

            d1 = vec_perm(t1, t2, mask); // [e f g h]

            d1 = vec_sel(vec_mergel(d, d), d1, sel); // [C f D h]

            edges = vec_perm(t2, t1, mask);

            t1 = vec_perm(edges, d1, align); // [b B d|C]

            t2 = vec_perm(d1, edges, align); // [f D h|x]

            vec_stl(t1, 16, dst+y);

            t0 = t1;

            vec_stl(t2, 31, dst+y);

            t1 = t2;
        }
    } else
    #endif
    if (step == 1 && src3 == 0)
        for (i=0; i<len-3; i+=4) {
            t0 = vec_ld(0, dst+i);
            t1 = vec_ld(15, dst+i);
            s0 = vec_ld(0, src0+i);
            s1 = vec_ld(0, src1+i);
            s2 = vec_ld(0, src2+i);
            edges = vec_perm(t1 ,t0, mask);
            d = vec_madd(s0,s1,s2);
            t1 = vec_perm(d, edges, align);
            t0 = vec_perm(edges, d, align);
            vec_st(t1, 15, dst+i);
            vec_st(t0, 0, dst+i);
        }
    else
        ff_vector_fmul_add_add_c(dst, src0, src1, src2, src3, len, step);
}


static vector signed short
float_to_int16_one_altivec(const float *src)
{
    vector float s0 = vec_ld(0, src);
    vector float s1 = vec_ld(16, src);
    vector signed int t0 = vec_cts(s0, 0);
    vector signed int t1 = vec_cts(s1, 0);
    return vec_packs(t0,t1);
}

static void float_to_int16_altivec(int16_t *dst, const float *src, int len)
{
    int i;
    vector signed short d0, d1, d;
    vector unsigned char align;
    if(((long)dst)&15) //FIXME
    for(i=0; i<len-7; i+=8) {
        d0 = vec_ld(0, dst+i);
        d = float_to_int16_one_altivec(src+i);
        d1 = vec_ld(15, dst+i);
        d1 = vec_perm(d1, d0, vec_lvsl(0,dst+i));
        align = vec_lvsr(0, dst+i);
        d0 = vec_perm(d1, d, align);
        d1 = vec_perm(d, d1, align);
        vec_st(d0, 0, dst+i);
        vec_st(d1,15, dst+i);
    }
    else
    for(i=0; i<len-7; i+=8) {
        d = float_to_int16_one_altivec(src+i);
        vec_st(d, 0, dst+i);
    }
}

static void
float_to_int16_interleave_altivec(int16_t *dst, const float **src,
                                  long len, int channels)
{
    int i;
    vector signed short d0, d1, d2, c0, c1, t0, t1;
    vector unsigned char align;
    if(channels == 1)
        float_to_int16_altivec(dst, src[0], len);
    else
        if (channels == 2) {
        if(((long)dst)&15)
        for(i=0; i<len-7; i+=8) {
            d0 = vec_ld(0, dst + i);
            t0 = float_to_int16_one_altivec(src[0] + i);
            d1 = vec_ld(31, dst + i);
            t1 = float_to_int16_one_altivec(src[1] + i);
            c0 = vec_mergeh(t0, t1);
            c1 = vec_mergel(t0, t1);
            d2 = vec_perm(d1, d0, vec_lvsl(0, dst + i));
            align = vec_lvsr(0, dst + i);
            d0 = vec_perm(d2, c0, align);
            d1 = vec_perm(c0, c1, align);
            vec_st(d0,  0, dst + i);
            d0 = vec_perm(c1, d2, align);
            vec_st(d1, 15, dst + i);
            vec_st(d0, 31, dst + i);
            dst+=8;
        }
        else
        for(i=0; i<len-7; i+=8) {
            t0 = float_to_int16_one_altivec(src[0] + i);
            t1 = float_to_int16_one_altivec(src[1] + i);
            d0 = vec_mergeh(t0, t1);
            d1 = vec_mergel(t0, t1);
            vec_st(d0,  0, dst + i);
            vec_st(d1, 16, dst + i);
            dst+=8;
        }
    } else {
        DECLARE_ALIGNED(16, int16_t, tmp[len]);
        int c, j;
        for (c = 0; c < channels; c++) {
            float_to_int16_altivec(tmp, src[c], len);
            for (i = 0, j = c; i < len; i++, j+=channels) {
                dst[j] = tmp[i];
            }
        }
   }
}

void float_init_altivec(DSPContext* c, AVCodecContext *avctx)
{
    c->vector_fmul = vector_fmul_altivec;
    c->vector_fmul_reverse = vector_fmul_reverse_altivec;
    c->vector_fmul_add_add = vector_fmul_add_add_altivec;
    if(!(avctx->flags & CODEC_FLAG_BITEXACT)) {
        c->float_to_int16 = float_to_int16_altivec;
        c->float_to_int16_interleave = float_to_int16_interleave_altivec;
    }
}
