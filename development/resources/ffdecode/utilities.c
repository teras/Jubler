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


#include "utilities.h"
#include <stdarg.h>
#include <stdlib.h>

void storeBigEndian(unsigned short int number, FILE * outfile) {
    unsigned char data;
    
    data = number >> 8;
    fwrite(&data, 1, 1, outfile);
    
    data = number % 256;
    fwrite(&data, 1, 1, outfile);
}

unsigned short int retrieveBigEndian(FILE * outfile) {
    int iochar;
    
    unsigned short int ret;
    
    iochar = fgetc(outfile);
    if (iochar==EOF) return 0;
    ret = iochar*256;
    
    iochar = fgetc(outfile);
    if (iochar==EOF) return 0;
    ret += iochar;
    
    return ret;
}



int isLittleEndian() {
    /* Data structure to test for endianness */
    union endian_data { int word; char bytes[sizeof(int)]; };
    union endian_data d;
    d.word = 0x1;
    if (d.bytes[0] == 0x1) return 1;
    return 0;
}

#undef printf
void DEBUG(JNIEnv *env, jobject this, const char *section, const char *expr, ...) {
	char * buffer = malloc(1000);
	char * pos = buffer;	
	int hm = snprintf(buffer, 100, "%s:", section);
	if (hm>99) hm = 99;
	pos += hm;
	*pos = ' ';
	pos++;

	va_list list;
    va_start(list, expr);
    vsnprintf(pos, 900, expr, list);
    va_end(list);

	jclass cls = (*env)->GetObjectClass(env, this);
	jmethodID mid = (*env)->GetMethodID(env, cls, "debug", "(Ljava/lang/String;)V");

	if (mid==NULL)
		 printf("Unable to use callback feature!");
	else {
		jstring message = (*env)->NewStringUTF(env, buffer);
		if (message!=NULL) {
			(*env)->CallVoidMethod(env, this, mid, message);
		}
	}
	free(buffer);
}
#define printf Use_DEBUG_instead_of_printf .
