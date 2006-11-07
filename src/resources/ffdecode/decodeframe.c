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
#include "avformat.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "com_panayotis_jubler_preview_decoders_FFMPEG.h"
#include "defaults.h"
#include "utilities.h"

AVPicture *decodeFrame (const char *input_filename, jlong timepos, jint *width, jint *height);
int file_info(char *input_filename);


JNIEXPORT jintArray JNICALL Java_com_panayotis_jubler_preview_decoders_FFMPEG_grabFrame
  (JNIEnv * env, jobject this, jstring video, jlong time, jboolean issmall)
{	
	/* Pointers for c-like strings */
	const char *video_c;
	
	/* Here we'll store the frame for java */
	jintArray matrix = NULL;
	jint* matrixdata = NULL;
	
	/* Frame raw data */
	AVPicture* pict;
	jint width, height;
	
	/* translate Java strings into C strings */
	video_c  = (*env)->GetStringUTFChars(env, video, 0);

	int zoom = 1;
	int shift = 0;
	if (issmall) {
		zoom = 2;
		shift = 2;	// it's 2 because we have 2 dimensions, so it's 4 cells
	}
	
	/* Finf endianess */
	int LE = isLittleEndian();

	/* Grab the desired frame */
	pict = decodeFrame(video_c, time, &width, &height);
	if (pict) {
		
		jint rwidth = width>>(shift/2);
		jint rheight = height>>(shift/2);
		jint size = rwidth*rheight+2;	// Size of the return array
		
		// make array
		matrix = (*env)->NewIntArray(env, size);
		if (matrix) {
			/* Find pointer for matrix size */
			matrixdata = (*env)->GetIntArrayElements(env, matrix, 0);
			
			/* This is a trick: the first 2 elements are not video data but the size of the video */
			matrixdata[0] = rwidth;
			matrixdata[1] = rheight;

			int mpos, dpos;	/* Matrix position, data position */
			int xe, ye, xi, yi;
			jint r,g,b;
			uint8_t *data = pict->data[0];
			jint * mdata = &matrixdata[2];
		
			/* Create a normal or a zoomed out picture */

			/* The heart of the optimization: dpos in interlan yi loop, outside x1 : 
			 * dpos = (xe*zoom + (ye*zoom+yi)*width)<<2;	
			 * Finds the offsert depending on the row */
			dpos = mpos = 0;
			
			int zoom_4 = zoom*4;
			int width_4 = width*4;
			int zoom_width_4 = zoom * width_4;
			int dpos_nextrow = width_4 - zoom*4;
			int xe_zoom_4 = 0;
			int ye_zoom_width_4 = 0;
			int yi_width_4 = 0;
			for (ye = 0 ; ye < rheight ; ye++) {
				for (xe = 0; xe < rwidth ; xe++) {
					r=g=b=0;
					dpos = xe_zoom_4 + ye_zoom_width_4;
					for(yi = 0 ; yi < zoom ; yi++) {
						for(xi = 0 ; xi < zoom ; xi++) {
							if (LE){
								b+=data[dpos++];
								g+=data[dpos++];
								r+=data[dpos++];
								dpos++; // Ignore alpha channel
							} else {
								dpos++; // Ignore alpha channel
								r+=data[dpos++];
								g+=data[dpos++];
								b+=data[dpos++];
							}
						}
						dpos += dpos_nextrow;
					}
					r >>= shift;
					g >>= shift;
					b >>= shift;
					if (r>0xff || g>0xff || b>0xff)	printf("Q");
					mdata[mpos]=0xff000000|(r<<16)|(g<<8)|b;
					mpos++;

					/* Optimization variables */
					xe_zoom_4 += zoom_4;
					yi_width_4 = 0;
				}
				/* Optimization variables */
				xe_zoom_4 = 0;
				ye_zoom_width_4 += zoom_width_4;
			}
			//memcpy(matrixdata+2, pict->data[0], width*height*4);

			/* Release the matrix data pointer */
			(*env)->ReleaseIntArrayElements(env, matrix, matrixdata, 0);
		} else {
			printf("Can not create array into memory\n");
		}
		/* Release the picture data */
		avpicture_free(pict);
		free(pict);
	} else {
		printf("Could not load frame.\n");
	}

	/* free memory reserved for Java->C strings */
	(*env)->ReleaseStringUTFChars(env, video, video_c);

	return matrix;
}




AVPicture* decodeFrame (const char *input_filename, const jlong seek_time, jint *width, jint *height)
{
	/* *TERAS* This should be done at the beginning */
	av_register_all();
	
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
		printf("Can't open file: %s\n", input_filename);
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
		printf("Video stream with supported codec not found.\n");
		retflag = FALSE;
	}
	else {
		// Open codec
		if((codec_is_open = avcodec_open(ccx, codec)) < 0 ) {
			printf("Could not open codec.\n");
			retflag = FALSE;
		}
		else {
			// Do a check that we don't seek beyond the movie duration
			if(seek_time > fcx->duration) {
				printf("Seek time cannot be greater than input's file duration\n");
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
					printf("Error while decoding\n");
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
		// Allocate an AVPicture
		avpicture_alloc(pict, PIX_FMT_RGBA32, ccx->width, ccx->height);
		img_convert(pict, PIX_FMT_RGBA32, (AVPicture*) frame, ccx->pix_fmt, ccx->width, ccx->height);
	
		*width = ccx->width;
		*height = ccx->height;
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


int file_info(char * input_filename) {
	int err=0;
	AVFormatContext * fcx=NULL;
	
	av_register_all();

	// Open the input file.
	err = av_open_input_file(&fcx, input_filename, NULL, 0, NULL);
	if(err<0){
		printf("Can't open file: %s\n", input_filename);
		return 1;
	}

	// Find the stream info
	err = av_find_stream_info(fcx);
	
	// Give us information about the file and exit
	dump_format(fcx, 0, input_filename, FALSE);

	av_close_input_file(fcx);
	return 0;
}



