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

