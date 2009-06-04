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


#ifndef DEFINES_H
#define DEFINES_H

/* How many cache files we can hold in memory simultaneously */
#define DICTLENGTH 100

/* Number of "visual" samples */
#define CACHELENGTH 1000

/* Samples per second */
#define RESOLUTION 1000


//#define DATA_LITTLE_ENDIAN 1
//#define DATA_BIG_ENDIAN 0
//#define ENDIANESS DATA_LITTLE_ENDIAN

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif

#endif
