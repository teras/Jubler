// Bare-bones ffmpeg (cvs latest) audio decoding and minmax caching.
//
// Audio decoding and minmax caching code: Thanos Kyritsis
// Date: November 2005
//
// JNI, Java specific code: Panayotis Katsaloulis
// Date: November 2005
//

/* This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "com_panayotis_jubler_media_preview_decoders_NativeDecoder.h"
#include "defaults.h"
#include "utilities.h"

JNIEXPORT jboolean JNICALL Java_com_panayotis_jubler_media_preview_decoders_NativeDecoder_makeCache(JNIEnv * env, jobject this, jstring audio, jstring cache, jstring original) {
    const char *audio_c;
    const char *cache_c;
    const char *original_c;
    
    float val;
    AVCodec *codec=NULL;
    AVPacket pkt;
    AVCodecContext *ccx=NULL;
    AVFormatContext *fcx=NULL;
    int got_audio=0, len=0, err=0, audio_index=-1, i=0, j=0, bytecounter=0, packsize=0, codec_is_open=-1;
    unsigned char *packptr;
    unsigned char channels = 0;
    jlong pack_pts=0;
    char sample;
    int step = sizeof(sample)*2;
    char *maxsample = NULL, *minsample = NULL;
    int charsize = sizeof(char);
    float ratewindow = 0;
    unsigned int offset=0, maxbyte=0, sampledcounter=1;
    FILE *cachefile=NULL;
    uint8_t *outbuf=NULL;
    jboolean nobrk = JNI_TRUE;
    
    int ENDIANESS = isLittleEndian();
    
    av_register_all();
    
    /* translate Java strings into C strings */
    audio_c  = (*env)->GetStringUTFChars(env, audio, 0);
    cache_c  = (*env)->GetStringUTFChars(env, cache, 0);
    original_c  = (*env)->GetStringUTFChars(env, original, 0);
    
    jboolean ret = JNI_FALSE;
    
    /* Find callback function & interrupt flag */
    jclass cls = (*env)->GetObjectClass(env, this);
    jmethodID mid = (*env)->GetMethodID(env, cls, "updateViewport", "(F)V");
    jmethodID iid = (*env)->GetMethodID(env, cls, "getInterruptStatus", "()Z");
    if (mid != 0 && iid != 0) {
        ret = JNI_TRUE;
        
        // Open the input file.
        err = av_open_input_file(&fcx, audio_c, NULL, 0, NULL);
        cachefile = fopen(cache_c, "wb");
        if(err<0){
            DEBUG(env, this, "makeCache", "Could not open audio file '%s'.", audio_c);
            ret = JNI_FALSE;
        }
        if (!cachefile) {
            DEBUG(env, this, "makeCache", "Could not open cache file '%s'.", cache_c);
            ret = JNI_FALSE;
        }
        outbuf = malloc(AVCODEC_MAX_AUDIO_FRAME_SIZE);
        if(outbuf==NULL) {
            DEBUG(env, this, "makeCache", "Could not allocate memory for outbuf.");
            ret = JNI_FALSE;
        }
        
        if (ret != JNI_FALSE) {
            // Find the stream info.
            av_find_stream_info(fcx);
            
            /* Find the first supported codec in the audio streams */
            for(i=0; i<fcx->nb_streams; i++){
                ccx=fcx->streams[i]->codec;
                if(ccx->codec_type==CODEC_TYPE_AUDIO) {
                    /* Found an audio stream, check if codec is supported */
                    codec = avcodec_find_decoder(ccx->codec_id);
                    if(codec){
                        /* codec is supported, proceed */
                        audio_index = i;
                        break;
                    }
                }
            }
        }
        
        /* Find codec id */
        if(audio_index < 0){
            DEBUG(env, this, "makeCache", "Audio stream with supported codec not found.");
            ret = JNI_FALSE;
        }
        else {
            /* open it */
            if ((codec_is_open = avcodec_open(ccx, codec)) < 0) {
                DEBUG(env, this, "makeCache", "Could not open codec.");
                ret = JNI_FALSE;
            }
            else {
                /* See how many channels the input file contains */
                channels = ccx->channels;
                ratewindow = (float)ccx->sample_rate / RESOLUTION;
                maxbyte = (sampledcounter) * channels * step * ratewindow;
                maxsample = malloc(channels);
                if(maxsample==NULL) {
                    DEBUG(env, this, "makeCache", "Could not allocate memory for maxsample.");
                    ret = JNI_FALSE;
                }
                minsample = malloc(channels);
                if(minsample==NULL) {
                    DEBUG(env, this, "makeCache", "Could not allocate memory for minsample.");
                    ret = JNI_FALSE;
                }
                
                if (ret != JNI_FALSE) {
                    /* Initialize these arrays */
                    for (j=1;j<=channels;j++) {
                        minsample[j-1] = 127;
                        maxsample[j-1] = -128;
                    }
                    /* Let's write the header, for start */
                    fprintf(cachefile, "JACACHE\1");	/* Store magic key and version number */
                    fwrite(&channels, 1, 1, cachefile);	/* Store number of channels */
                    storeBigEndian(RESOLUTION, cachefile);	/* Store samples per second in big endian fashion */
                    storeBigEndian((unsigned short int)strlen(original_c), cachefile);	/* Store filename string size in big endian fashion */
                    fprintf(cachefile, "%s", original_c); /* Store UTF8 filename string */
                    /* End of header */
                }
            }
        }
        
        
        
        /* Here we convert an audio stream into cache */
        while (ret != JNI_FALSE && nobrk != JNI_FALSE) {
            // Read a frame/packet
            if(av_read_frame(fcx, &pkt) < 0 ) break;
            packsize = pkt.size;
            packptr = pkt.data;
            // Make sure this packet belongs to the stream we want
            if(pkt.stream_index==audio_index){
                while (packsize > 0 && nobrk != FALSE) {
                    // Rescale the times
                    pack_pts = av_rescale_q(pkt.pts, fcx->streams[audio_index]->time_base, AV_TIME_BASE_Q);
                    // Decode the paket
                    got_audio = AVCODEC_MAX_AUDIO_FRAME_SIZE;
                    len = avcodec_decode_audio2(ccx, (short *)outbuf, &got_audio, pkt.data, pkt.size);
                    
                    if (len < 0) {
                        DEBUG(env, this, "makeCache", "Error while decoding.");
                        break;
                    }
                    
                    packsize -= len;
                    packptr += len;
                    
                    if (got_audio > 0) {
                        // Output cache
                        // bytecounter counts for each 'chunk' avcodec gives us, not the whole stream
                        // bytecounter+offset is our current position in the whole stream
                        bytecounter=0;
                        // got_audio is the length of the 'chunk'
                        while (bytecounter < got_audio) {
                            // we check if the byte of the next sample is beyond the byte of the downsampling window
                            if (bytecounter+offset + step*channels >= maxbyte) {
                                for (j = 1; j <= channels ; j++) {
                                    // actually sample is not the whole sample, but the data for one channel each time
                                    sample = (char)outbuf[bytecounter+ENDIANESS];
                                    // min max averaging: only keep the highest and the lowest sample value
                                    if (maxsample[j-1] < sample) maxsample[j-1] = sample;
                                    if (minsample[j-1] > sample) minsample[j-1] = sample;
                                    fwrite(&maxsample[j-1], charsize, 1, cachefile);
                                    fwrite(&minsample[j-1], charsize, 1, cachefile);
                                    // reset min and max
                                    minsample[j-1] = 127;
                                    maxsample[j-1] = -128;
                                    // move 'chunk' byte pointer to current sample, next channel
                                    bytecounter += step;
                                }
                                // increase the counter showing how many samples we've averaged
                                sampledcounter++;
                                // move downsampling window pointer to the next window position
                                maxbyte = (sampledcounter) * channels * step * ratewindow;
                            }
                            else {
                                for (j = 1; j <= channels; j++) {
                                    sample = (char)outbuf[bytecounter+ENDIANESS];
                                    // min max averaging
                                    if (maxsample[j-1] < sample) maxsample[j-1] = sample;
                                    if (minsample[j-1] > sample) minsample[j-1] = sample;
                                    // move 'chunk' byte pointer to current sample, but next channel
                                    bytecounter += step;
                                }
                            }
                        }
                        /* val is for the percentage progressbar in the gui */
                        val = (float) pack_pts / fcx->duration;
                        /* Check if interrupt has been assigned */
                        if ((*env)->CallBooleanMethod(env, this, iid)) {
                            DEBUG(env, this, "makeCache", "Creation of cache file interrupted!");
                            ret = JNI_FALSE;
                            nobrk = JNI_FALSE;
                            break;
                        }
                        /* Update visual progress bar */
                        (*env)->CallVoidMethod(env, this, mid, val);
                        /* add to the offset our current position */
                        offset += bytecounter;
                    }
                }
            }
            av_free_packet(&pkt);
        } // end of while(ret != JNI_FALSE && nobrk != JNI_FALSE)
    } // end of if(!mid)
    
    /* free memory reserved for Java->C strings */
    (*env)->ReleaseStringUTFChars(env, audio, audio_c);
    (*env)->ReleaseStringUTFChars(env, cache, cache_c);
    (*env)->ReleaseStringUTFChars(env, original, original_c);
    
    /* Clean up */
    if(maxsample != NULL)  free(maxsample);
    if(minsample != NULL)  free(minsample);
    if(cachefile != NULL)  fclose(cachefile);
    if(codec_is_open >= 0) avcodec_close(ccx);
    if(outbuf != NULL)     free(outbuf);
    if(fcx != NULL)        av_close_input_file(fcx);
    
    return ret;
}

