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
#include "avformat.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "com_panayotis_jubler_preview_decoders_FFMPEG.h"
#include "defaults.h"
#include "utilities.h"


/*
 * Class:     com_panayotis_jubler_preview_decoders_FFMPEG
 * Method:    grabFPS
 * Signature: (Ljava/lang/String;)F
 */
JNIEXPORT jfloat JNICALL Java_com_panayotis_jubler_preview_decoders_FFMPEG_grabFPS
  (JNIEnv * env, jobject this, jstring video)
{

	/* Pointers for c-like strings */
	const char *video_c;
	
	/* Here we'll store the FPS */
	jfloat FPS = -1;
	

	/* translate Java strings into C strings */
	video_c  = (*env)->GetStringUTFChars(env, video, 0);

	
	/* free memory reserved for Java->C strings */
	(*env)->ReleaseStringUTFChars(env, video, video_c);

	return FPS;
}

