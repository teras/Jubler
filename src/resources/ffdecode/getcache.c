
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


#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "com_panayotis_jubler_media_preview_decoders_NativeDecoder.h"
#include "defaults.h"
#include "utilities.h"


struct dictionary {
	char * name;
	size_t namesize;
	char * data;
	size_t datasize;
	unsigned char channels;
};


static struct dictionary dict[DICTLENGTH];


struct dictionary * lookup(const char * fname);
void loadCache(struct dictionary * dict);
void populateMatrix(size_t start, size_t finish, char * data, size_t datatsize, char channels, jfloat * cache);



JNIEXPORT jfloatArray JNICALL Java_com_panayotis_jubler_media_preview_decoders_NativeDecoder_grabCache
  (JNIEnv * env, jobject this, jstring cfile, jdouble from, jdouble to)
{
	/* translate Java strings into C strings */
	const char * cache_c  = (*env)->GetStringUTFChars(env, cfile, 0);
	
	/* get a pointer for this data cache */
	struct dictionary * entry = lookup(cache_c);
	
	/* free memory reserved for Java->C strings, we don't need it anymore */
	(*env)->ReleaseStringUTFChars(env, cfile, cache_c);
			
	/* If we couldn't get a pointer for a data cache, we have to exit */
	if (entry==NULL) {
		DEBUG("grabCache", "Could not get a pointer for a data cache.\n");
		return NULL;
	}
	
	jfloatArray cmatrix = (*env)->NewFloatArray(env, CACHELENGTH * entry->channels * 2);
	if (cmatrix==NULL) {
		DEBUG("grabCache", "Could not reserve memory for new array.\n");
		return NULL;
	}
	jfloat *cache = (*env)->GetFloatArrayElements(env, cmatrix, 0);

	populateMatrix(from * RESOLUTION, to * RESOLUTION, entry->data, entry-> datasize, entry->channels, cache);
	/* Release the matrix data pointer */
	(*env)->ReleaseFloatArrayElements(env, cmatrix, cache, 0);
	
	return cmatrix;
}




JNIEXPORT void JNICALL Java_com_panayotis_jubler_media_preview_decoders_NativeDecoder_forgetCache
  (JNIEnv *env, jobject this, jstring cache) 
{
	int pointer;
	
	/* translate Java strings into C strings */
	const char * cache_c  = (*env)->GetStringUTFChars(env, cache, 0);

	/* Find if the required cache is already loaded */
	size_t fname_size = strlen(cache_c);
	for (pointer = 0 ; pointer < DICTLENGTH && dict[pointer].namesize>0 ; pointer++ ) {
		if (fname_size==dict[pointer].namesize) {	/* If it has the same size (fast) */
			if (strncmp(dict[pointer].name, cache_c, fname_size) == 0 ) {	/* And the same name, slower */
				break;
			}
		}
	}
	
	/* The cache was found */
	if (pointer<DICTLENGTH) {
		int last;

		/* Free up memory */
		free(dict[pointer].name);
		free(dict[pointer].data);
		dict[pointer].name = NULL;
		dict[pointer].data = NULL;
		dict[pointer].namesize = 0;
		dict[pointer].datasize = 0;
		dict[pointer].channels = 0;

		/* Find last valid entry */
		for (last = pointer+1 ; last < DICTLENGTH && dict[last].namesize>0 ; last++ );
		last--;

		if (last>pointer) {
			DEBUG("forgetCache", "Moving file.\n");
			dict[pointer].name = dict[last].name;
			dict[pointer].data = dict[last].data;
			dict[pointer].namesize = dict[last].namesize;
			dict[pointer].datasize = dict[last].datasize;
			dict[pointer].channels = dict[last].channels;

			dict[last].name = NULL;
			dict[last].data = NULL;
			dict[last].namesize = 0;
			dict[last].datasize = 0;
			dict[last].channels = 0;
		}
		DEBUG("forgetCache", "Cleaning up #%i (from %i) cache file '%s'.\n", pointer, last, cache_c);
	}
	
	/* free memory reserved for Java->C strings, we don't need it anymore */
	(*env)->ReleaseStringUTFChars(env, cache, cache_c);
	
}


void populateMatrix(size_t samplestart, size_t samplefinish, char * data, size_t datasize, char channels, jfloat * cache) {
	
	int bytespersample = channels * 2;
	double samplestep = ((double)samplefinish-samplestart)/RESOLUTION;
	
	char min[channels];
	char max[channels];
	
	/* The actual minmax routine starts here */
	size_t currentdata = samplestart * bytespersample;
	int chan;
	int cachepos = 0;
	int visual;
	for (visual = 0 ; visual < RESOLUTION && currentdata < datasize; visual++ ) {
		size_t dataobstacle = ((visual+1) * samplestep + samplestart) * bytespersample;
		/* clean up min max */
		for (chan = 0 ; chan < channels ; chan++) {
			min[chan] = 127;
			max[chan] = -128;
		}
		
		/* Find minmax */
		while (currentdata < dataobstacle && currentdata < datasize ) {
			for (chan = 0 ; chan < channels ; chan++) {
				if (max[chan]<data[currentdata]) max[chan]=data[currentdata];
				currentdata++;
				if (min[chan]>data[currentdata]) min[chan]=data[currentdata];
				currentdata++;
			}
		}
		/* Store data in cache */
		for (chan = 0 ; chan < channels ; chan++) {
			cache[cachepos++] = ((int)(max[chan])+128)/255.0f;
			cache[cachepos++] = ((int)(min[chan])+128)/255.0f;
		}
	}
}


/* Look up a filename in the loaded cache database. 
 * If the file is already loaded, return the cache alrady in memory.
 * Or else, load the file and return the fresh loaded cache */
struct dictionary * lookup(const char * fname) {
	int pointer;

	/* Find if the required cache is already loaded */
	size_t fname_size = strlen(fname);
	for (pointer = 0 ; pointer < DICTLENGTH && dict[pointer].namesize>0 ; pointer++ ) {
		if (fname_size==dict[pointer].namesize) {	/* If it has the same size (fast) */
			if (strncmp(dict[pointer].name, fname, fname_size) == 0 ) {	/* And the same name, slower */
				return &dict[pointer];	/* We have already loaded this cache file! */
			}
		}
	}
	
	if (pointer>=DICTLENGTH) {	/* We don't have any more space left to store this cache file */
		DEBUG("lookup", "Audio cache lookup table is full, please increase DICTLENGTH in defaults.h .\n");
		return NULL;
	}
	
	dict[pointer].name = malloc(fname_size+1);	/* first store filename */
	if (dict[pointer].name==NULL) {
		DEBUG("lookup", "Could not allocate memory to store filename '%s'.\n", fname);
		return NULL;
	}
	strncpy(dict[pointer].name, fname, fname_size+1);
	
	loadCache(&dict[pointer]); 	/* Then load cache into memory */
	if (dict[pointer].data == NULL ) {
		DEBUG("lookup", "Unable to load cache.\n");
		free(dict[pointer].name);
		dict[pointer].name = NULL;
		return NULL;
	}

	dict[pointer].namesize = fname_size;	/* At the end stor ethe size of the filename */
	
	return &dict[pointer];
}


/* Load a cache file into memory */
void loadCache(struct dictionary * dict) {
	FILE *in;
	size_t bytes_read;
	
	/* find cache size */
	if ( (in = fopen(dict->name, "rb")) == NULL ) {
		DEBUG("loadCache", "Could not open file '%s'.\n", dict->name);
		return;
	}
	
	/* Find the fill filename size
	 * We need to correct this number by removing the header */
	fseek (in, 0, SEEK_END);
	dict->datasize = ftell(in);
	
	/* Find channels */
	fseek(in, 8, SEEK_SET);
	dict->channels = (unsigned char)fgetc(in);
	
	/* Skip resolution definition */
	fgetc(in); fgetc(in);
	
	/* Skip finame definition */
	fseek (in, retrieveBigEndian(in), SEEK_CUR);

	/* Find the actual data size */
	dict->datasize = dict->datasize - ftell(in);

	dict->data = malloc(dict->datasize);	/* reserve cache data */
	if (dict->data!=NULL) {
		bytes_read = fread(dict->data, 1, dict->datasize, in);	/* load cache into memory */
		if (bytes_read!=dict->datasize) {
			DEBUG("loadCache", "WARNING: wanted & read bytes differ. Wave preview might not be complete.\n");
		}
		dict->datasize = bytes_read;
	}
	else {
		dict->channels = 0;
		dict->datasize = 0;
	}
	
	fclose(in);
}
