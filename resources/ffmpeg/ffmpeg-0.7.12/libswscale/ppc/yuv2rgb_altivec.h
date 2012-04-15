/*
 * AltiVec-enhanced yuv2yuvX
 *
 * Copyright (C) 2004 Romain Dolbeau <romain@dolbeau.org>
 * based on the equivalent C code in swscale.c
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

#ifndef PPC_YUV2RGB_ALTIVEC_H
#define PPC_YUV2RGB_ALTIVEC_H 1

void ff_yuv2packedX_altivec(SwsContext *c, const int16_t *lumFilter,
                            const int16_t **lumSrc, int lumFilterSize,
                            const int16_t *chrFilter, const int16_t **chrUSrc,
                            const int16_t **chrVSrc, int chrFilterSize,
                            const int16_t **alpSrc, uint8_t *dest,
                            int dstW, int dstY);

#endif /* PPC_YUV2RGB_ALTIVEC_H */
