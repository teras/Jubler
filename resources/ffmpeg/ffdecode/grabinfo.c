// Bare-bones ffmpeg (cvs latest) precise time seeking and frame decoding
//
// Precise time seeking and frame decoding code: Thanos Kyritsis
// Date: December 2006
//
// JNI, Java specific and endianess code: Panayotis Katsaloulis
// Date: December 2006
//

/* This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

// Use the correct path here for avformat.
#include "libavformat/avformat.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "com_panayotis_jubler_media_preview_decoders_FFMPEG.h"
#include "defaults.h"
#include "utilities.h"

jfloat dump_fps(const char *input_filename);
void get_information(JNIEnv * env, jobject this, jfloat* dimension, const char* input_filename);


/*
 * Class:     com_panayotis_jubler_media_preview_decoders_FFMPEG
 * Method:    grabDimension
 * Signature: (Ljava/lang/String;)[I
 */
JNIEXPORT jfloatArray JNICALL Java_com_panayotis_jubler_media_preview_decoders_FFMPEG_grabInformation(JNIEnv * env, jobject this, jstring video) {
    /* Pointers for c-like strings */
    const char *video_c;
    /* Result integer table */
    jfloatArray dimension = NULL;
    jfloat* matrixdata = NULL;
    
    /* initialize java array */
    dimension = (*env)->NewFloatArray(env, 4);
    if (dimension==NULL) return NULL;
    
    /* get array data position */
    matrixdata = (*env)->GetFloatArrayElements(env, dimension, 0);
    
    /* translate Java strings into C strings */
    video_c  = (*env)->GetStringUTFChars(env, video, 0);
    
    get_information(env, this, matrixdata, video_c);
    
    /* free memory reserved for Java->C strings */
    (*env)->ReleaseStringUTFChars(env, video, video_c);
    
    /* release integer data */
    (*env)->ReleaseFloatArrayElements(env, dimension, matrixdata, 0);
    
    return dimension;
}

/* Get the actual dimension of a video file and store it in a two position integer */
void get_information(JNIEnv * env, jobject this, jfloat* dim, const char* video_c) {
    int err=0, i=0;
    AVFormatContext * fcx=NULL;
    jboolean ret = JNI_TRUE;
    
    av_register_all();
    
    // Open the input file.
    err = av_open_input_file(&fcx, video_c, NULL, 0, NULL);
    if(err<0) {
        DEBUG(env, this, "get_information", "Could not open file '%s'.", video_c);
        ret = JNI_FALSE;
    }
    
    if (ret != JNI_FALSE) {
        // Find the stream info
        err = av_find_stream_info(fcx);
        if (fcx->duration != AV_NOPTS_VALUE)
            dim[2] = ((float)fcx->duration) / AV_TIME_BASE;
        else
            dim[2] = 0;
        
        // Give us information about the resolution and exit
        for(i=0;i<fcx->nb_streams;i++) {
            AVStream *st = fcx->streams[i];
            if(st->codec->codec_type == CODEC_TYPE_VIDEO) {
                dim[0] = st->codec->width;
                dim[1] = st->codec->height;
                
                /* Calc FPS */
                if(st->r_frame_rate.den && st->r_frame_rate.num) {
                    dim[3] = av_q2d(st->r_frame_rate);
                }
                else {
                    dim[3] = 1/av_q2d(st->codec->time_base);
                }
                break; // we only need the first supported stream
            }
        }
    }
    
    if(fcx != NULL) av_close_input_file(fcx);
}

