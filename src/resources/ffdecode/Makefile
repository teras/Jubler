#######################################################
# Change the following variables to match your system #
#######################################################

# Paths of include files
JAVA_HOME=$(shell /bin/sh ../system/findjava.sh -j -v 1.5)



##############################################################
# Usually you don't need to change anything after this point #
##############################################################

# Path of the ffmpeg source structure (already inside the source version of Jubler)
FFMPEG=../ffmpeg-svn

FFCONF=configure.patched

# Path of Jubler distribution
JUBLER=../..

# Path of ffdecode relative to top Jubler directory
FFDECODE=resources/ffdecode

# Where javah lives
JAVAH=${JAVA_HOME}/bin/javah

# Use three threads instead of one
MAKEOPTS=-j3

#Use this path to check for libraries
LIBRARIES=../libraries


PROJ=ffdecode
OBJPATH="com.panayotis.jubler.preview.decoders.FFMPEG com.panayotis.jubler.preview.decoders.NativeDecoder"
LIBPATH=-L${LIBRARIES}
LIBS=-lavformat${LIBSUF} -lavcodec${LIBSUF} -lavutil${LIBSUF} -lm
INCS=-I${FFMPEG}/libavcodec -I${FFMPEG}/libavformat -I${FFMPEG}/libavutil

LINKOPTS=${LIBPATH} ${LIBS} ${XLIBS}
LIBSUF=_${SYSTEM}

SRCS=$(shell ls 2>/dev/null *.c)
OBJS=$(SRCS:.c=.o)


ifeq (${SYSTEM},linux)
	PREF=
	ARCH=
	LD=${CC} -shared
	LIBNAME=lib${PROJ}.so
	XLIBS=-lz
	FCOPTS=--enable-gpl --enable-a52 --disable-debug
endif
ifeq (${SYSTEM},win32)
	PREF=i586-mingw32msvc-
#	PREF=i686-pc-mingw32-
	ARCH=
	LD=${CC} -D_JNI_IMPLEMENTATION_ -Wl,--kill-a -shared
	LIBNAME=$(PROJ).dll
	XLIBS=
	FCOPTS=--enable-gpl --enable-a52 --disable-debug --enable-mingw32 --cross-prefix=${PREF} --enable-memalign-hack --cc=gcc
endif
ifeq (${SYSTEM},darwinppc)
	PREF=
	ARCH=-arch ppc
	#LD=${CC} -bundle
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.ppc
	XLIBS=-lz
#	FCOPTS=--enable-gpl --enable-memalign-hack --enable-a52 --disable-debug  --extra-cflags="-fno-common ${ARCH}" --enable-shared
	FCOPTS=--enable-gpl --cpu=powerpc --enable-a52 --disable-debug  --extra-cflags="-fno-common ${ARCH}" --enable-shared
endif
ifeq (${SYSTEM},darwini386)
	PREF=
	ARCH=
	#LD=${CC} -bundle
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.i386
	XLIBS=-lz
	FCOPTS=--enable-gpl --disable-mmx --cpu=unknown --enable-a52 --disable-debug  --extra-cflags=-fno-common --enable-shared
endif

CC=${PREF}gcc ${ARCH}
RANLIB=${PREF}ranlib
STRIP=${PREF}strip


MACLIBNAME=lib${PROJ}.jnilib

.SUFFIXES:.o .c

.c.o:
	$(CC) -c -O3 -Wall ${INCS} -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SYSTEM) -std=c99 -pedantic -o $@ $?


develop:
	make -e `uname -s|tr [A-Z] [a-z]`


linux:distclean
	make -e library SYSTEM=linux
	rm -rf ${JUBLER}/src/lib


win32:distclean
	make -e library SYSTEM=win32
	rm -rf ${JUBLER}/src/lib

darwin:
	make darwini386
	make darwinppc
	make distclean
	lipo ${PROJ}.ppc ${PROJ}.i386 -create -output ${MACLIBNAME}
	rm -f ${PROJ}.ppc ${PROJ}.i386
	make -e libinstall LIBNAME=${MACLIBNAME}

darwinppc:distclean
	make -e LIB SYSTEM=darwinppc

darwini386:distclean
	make -e LIB SYSTEM=darwini386

library:clean $(LIBNAME) libinstall

libinstall:
	mkdir -p ${JUBLER}/dist/lib
	mkdir -p ${JUBLER}/src/lib
	cd ${JUBLER}/dist/lib ; if [ ! -e $(LIBNAME) ] ; then ln -s ../../${FFDECODE}/$(LIBNAME) . ; fi
	cd ${JUBLER}/src/lib ; if [ ! -e $(LIBNAME) ] ; then ln -s ../../${FFDECODE}/$(LIBNAME) . ; fi


LIB:${LIBNAME}

$(LIBNAME):ffmpeg $(OBJS) ranlib
	$(LD) $(ARCH) -o $(LIBNAME) $(OBJS) $(LINKOPTS)
#	$(STRIP) $(LIBNAME)


clean:
	rm -f *.o

distclean:clean
	rm -rf ${JUBLER}/dist/lib
	rm -rf ${JUBLER}/src/lib
	rm -f *.dll *.so *.jnilib


native:
	cd ${JUBLER} && ant compile
	cd ${JUBLER}/build/classes ; for JO in $(OBJPATH) ; do $(JAVAH) -jni $$JO ; done
	mv ${JUBLER}/build/classes/*.h .


ffmpeg:${LIBRARIES}/libavformat${LIBSUF}.a


${LIBRARIES}/libavformat${LIBSUF}.a:
	cd ${FFMPEG} ; ./${FFCONF} ${FCOPTS} ; cd libavformat ; make ${MAKEOPTS}; cd ../libavcodec ; make ${MAKEOPTS}; cd ../libavutil ; make ${MAKEOPTS}
	mkdir -p ${LIBRARIES}
	cp ${FFMPEG}/libavformat/libavformat.a ${LIBRARIES}/libavformat${LIBSUF}.a
	cp ${FFMPEG}/libavcodec/libavcodec.a ${LIBRARIES}/libavcodec${LIBSUF}.a
	cp ${FFMPEG}/libavutil/libavutil.a ${LIBRARIES}/libavutil${LIBSUF}.a
	cd ${FFMPEG} ; make distclean


ranlib:
	${RANLIB} ${LIBRARIES}/libavformat${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavcodec${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavutil${LIBSUF}.a
