// Bare-bones ffmpeg (cvs latest) precise time seeking and frame decoding
//
// Precise time seeking and frame decoding code: Thanos Kyritsis
// Date: November 2005
//
// JNI, Java specific and endianess code: Panayotis Katsaloulis
// Date: November 2005
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
#include "libswscale/swscale.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "com_panayotis_jubler_media_preview_decoders_FFMPEG.h"
#include "defaults.h"
#include "utilities.h"

#define MAXSIZE 16383


AVPicture *decodeFrame(JNIEnv * env, jobject this, const char *input_filename, jlong timepos, jint *width, jint *height, jfloat resize);
int file_info(JNIEnv * env, jobject this, char *input_filename);
void storenumb (jbyte * data, int number);

static int sws_flags = SWS_BICUBIC;


JNIEXPORT jbyteArray JNICALL Java_com_panayotis_jubler_media_preview_decoders_FFMPEG_grabFrame(JNIEnv * env, jobject this, jstring video, jlong time, jfloat resize) {
    /* Pointers for c-like strings */
    const char *video_c;
    
    /* Here we'll store the frame for java */
    jbyteArray matrix = NULL;
    jbyte* matrixdata = NULL;
    
    /* Frame raw data */
    AVPicture* pict;
    jint width, height;

    /* translate Java strings into C strings */
    video_c  = (*env)->GetStringUTFChars(env, video, 0);
    
    /* Grab the desired frame */
    pict = decodeFrame(env, this, video_c, time, &width, &height, resize);
    if (pict) {
        
		// make array
        matrix = (*env)->NewByteArray(env, width*height*3+4);	// 3 bytes per pixel + picture width information (2*2)
        
		if (matrix) {
            /* Find pointer for matrix size */
            matrixdata = (*env)->GetByteArrayElements(env, matrix, 0);
            
            /* Copy the actual color map to picture buffer */
            memcpy(matrixdata, pict->data[0], 3*width*height);
           
            /* This is a trick: the first 4 bytes are not video data but the size of the video */
            storenumb(matrixdata+(3*width*height), width);
            storenumb(matrixdata+(3*width*height)+2, height);

            /* Release the matrix data pointer */
            (*env)->ReleaseByteArrayElements(env, matrix, matrixdata, 0);
        } else {
            DEBUG(env, this, "grabFrame", "Can not create array into memory.");
        }
        /* Release the picture data */
        avpicture_free(pict);
        free(pict);
    } else {
        DEBUG(env, this, "grabFrame", "Could not load frame.");
    }
    
    /* free memory reserved for Java->C strings */
    (*env)->ReleaseStringUTFChars(env, video, video_c);
    
    return matrix;
}




AVPicture* decodeFrame(JNIEnv * env, jobject this, const char *input_filename, jlong seek_time, jint *width, jint *height, jfloat resize) {
    /* *TERAS* This should be done at the beginning */
    av_register_all();
    
	static struct SwsContext *swsContext = NULL;

    int err=0, i=0, len=0, got_picture, video_index=-1, pack_duration=0, codec_is_open=-1;
    int retflag=TRUE;
    jlong pack_pts=0, comp_pts=0;
    
    AVFormatContext *fcx=NULL;
    AVCodecContext *ccx=NULL;
    AVCodec *codec=NULL;
    
    AVPicture *pict = malloc(sizeof(AVPicture));
    AVPacket pkt;
    AVFrame *frame=avcodec_alloc_frame();
    
    /* Open the input file */
    err = av_open_input_file(&fcx, input_filename, NULL, 0, NULL);
    if(err<0){
        DEBUG(env, this, "decodeFrame", "Could not open file '%s'.", input_filename);
        retflag = FALSE;
    }
    if(pict==NULL) {
        DEBUG(env, this, "decodeFrame", "Could not allocate memory for pict.");
        retflag = FALSE;
    }
    
    if (retflag != FALSE) {
        // Find the stream info.
        err = av_find_stream_info(fcx);
        
        // Find the first supported codec in the video streams.
        for(i=0; i<fcx->nb_streams; i++){
            ccx=fcx->streams[i]->codec;
            if(ccx->codec_type==CODEC_TYPE_VIDEO) {
                // Found a video stream, check if codec is supported
                codec = avcodec_find_decoder(ccx->codec_id);
                if (codec) {
                    // codec is supported, proceed
                    video_index=i;
                    break;
                }
            }
        }
    }
    
    if(video_index < 0){
        DEBUG(env, this, "decodeFrame", "Video stream with supported codec not found.");
        retflag = FALSE;
    }
    else {
        // Open codec
        if((codec_is_open = avcodec_open(ccx, codec)) < 0 ) {
            DEBUG(env, this, "decodeFrame", "Could not open codec.");
            retflag = FALSE;
        }
        else {
            if (fcx->start_time != AV_NOPTS_VALUE) {
                seek_time += fcx->start_time;
            }
            // Do a check that we don't seek beyond the movie duration
            if(seek_time > fcx->duration) {
                DEBUG(env, this, "decodeFrame", "Seek time cannot be greater than input's file duration.");
                retflag = FALSE;
            }
            else {
                // Seek to the nearest keyframe before the seek_time we asked
                av_seek_frame(fcx, video_index, av_rescale_q(seek_time, AV_TIME_BASE_Q, fcx->streams[video_index]->time_base), AVSEEK_FLAG_BACKWARD);
            }
        }
    }
    
    // Run this loop until we reach the requested time position
    // Decode proper
    while(retflag != FALSE) {
        // Read a frame/packet
        if(av_read_frame(fcx, &pkt) < 0 ) break;
        // Make sure this packet belongs to the stream we want
        if(pkt.stream_index==video_index){
            // Rescale the times
            if (pkt.pts != AV_NOPTS_VALUE) comp_pts = pkt.pts;
            pack_pts = av_rescale_q(comp_pts, fcx->streams[video_index]->time_base, AV_TIME_BASE_Q);
            pack_duration = av_rescale_q(pkt.duration, fcx->streams[video_index]->time_base, AV_TIME_BASE_Q);
            comp_pts += pkt.duration;
            // Decode this packet
            len = avcodec_decode_video(ccx, frame, &got_picture, pkt.data, pkt.size);
            if (len < 0) {
                DEBUG(env, this, "decodeFrame", "Error while decoding.");
                retflag = FALSE;
                break;
            }
            if (got_picture) {
                // If we got the time exactly, or we are already past the seek time,
                // this is the frame we want
                if (pack_pts >= seek_time) {
                    av_free_packet(&pkt);
                    break;
                }
                // If the next frame will be past our seek_time, this is the frame we want
                else if ( pack_pts + pack_duration > seek_time ) {
                    av_free_packet(&pkt);
                    break;
                }
            }
        }
        av_free_packet(&pkt);
    }
    if (retflag != FALSE) {
        /* Calculating new picture size and keep aspect ratio */
        *width = (ccx->width) * resize;
       *height = (ccx->height) * resize;
        if (*width > MAXSIZE ) {
            *height = ( (*height) * MAXSIZE) / (*width);
            *width = MAXSIZE;
        }
        if (*height > MAXSIZE) {
            *width = ( (*width) * MAXSIZE) / (*height);
            *height = MAXSIZE;
        }

		DEBUG(env, this, "decodeFrame", "Resampling from (%i,%i) with resize factor %f to (%i,%i)",ccx->width, ccx->height, resize,*width, *height);
        // Allocate an AVPicture
        avpicture_alloc(pict, PIX_FMT_RGB24, *width, *height);
		swsContext = sws_getCachedContext(swsContext,
			ccx->width, ccx->height, ccx->pix_fmt,
			*width, *height, PIX_FMT_RGB24,
			sws_flags, NULL, NULL, NULL);
		if (swsContext == NULL) {
			DEBUG(env, this, "decodeFrame", "swscale context initialization failed.");
			*width = *height = -1;
	 	} else {
			sws_scale(swsContext,
				((AVPicture *)frame)->data,
				((AVPicture *)frame)->linesize,
				0, ccx->height,
				((AVPicture *)pict)->data,
				((AVPicture *)pict)->linesize); 
		}
    }
    
    // Clean up
    if (frame != FALSE)     av_free(frame);
    if (codec_is_open >= 0) avcodec_close(ccx);
    if (fcx != NULL)        av_close_input_file(fcx);
    
    if (retflag != FALSE) {
        return pict;
    }
    else {
        return NULL;
    }
}


int file_info(JNIEnv * env, jobject this, char * input_filename) { int err=0;
    AVFormatContext * fcx=NULL;
    
    av_register_all();
    
    // Open the input file.
    err = av_open_input_file(&fcx, input_filename, NULL, 0, NULL);
    if(err<0){
        DEBUG(env, this, "file_info", "Could not open file '%s'.", input_filename);
        return 1;
    }
    
    // Find the stream info
    err = av_find_stream_info(fcx);
    
    // Give us information about the file and exit
    dump_format(fcx, 0, input_filename, FALSE);
    
    av_close_input_file(fcx);
    return 0;
}

void storenumb (jbyte * data, int number) {
	data[0] = number/128;
	data[1] = number % 128;	
}

