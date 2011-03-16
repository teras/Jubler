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


#ifndef UTILITIES_H
#define UTILITIES_H

#include <stdio.h>
#include <jni.h>

#define printf Use_DEBUG_instead_of_printf .

void storeBigEndian(unsigned short int number, FILE * outfile);
unsigned short int retrieveBigEndian(FILE * outfile);

int isLittleEndian();

void DEBUG(JNIEnv *env, jobject obj, const char *section, const char *expr, ...);

#endif
