#######################################################
# Change the following variables to match your system #
#######################################################

# Variables should be automatically found!


##############################################################
# Usually you don't need to change anything after this point #
##############################################################

# Name of the produced library
PROJ=ffdecode

# Paths of include files
JAVA_HOME=${shell /bin/sh ../system/findjava.sh -j}

# Path of the local ffmpeg source structure (already inside the source version of Jubler)
FFMPEG=../ffmpeg-svn

# Use this path for static libraries
LIBRARIES=../libraries

# Path of Jubler distribution
JUBLER=../..

# Path of ffdecode relative to top Jubler directory
FFDECODE=resources/ffdecode

# Where javah lives
JAVAH=${JAVA_HOME}/bin/javah

# Various options
FFCONF=configure
LIBSUF=_${SYSTEM}
MAKEOPTS=-j3

# Find requested files
NATIVES=${shell cd ../../src && grep -r ' native ' * | grep -v '/.svn/' | gawk -F ':' '{print $$1}' | uniq | sed -e 's/\.java$$//g' | tr '/' '.' }
SRCS=${shell ls 2>/dev/null *.c}
OBJS=${SRCS:.c=.o}


ifeq (${CFLAGS},)
	COMPFLAGS=-std=c99 -pedantic -O3 -Wall
else
	COMPFLAGS=${CFLAGS}
endif

ifeq (${STATIC},no)
	FFMPEGREQ=
	RANLIBREQ=
	LIBS=${shell pkg-config --libs libavformat libavutil libavcodec}
	INCS=${shell pkg-config --cflags libavformat libavutil libavcodec}
else
	FFMPEGREQ=ffmpeg
	RANLIBREQ=ranlib
	LIBS=-L${LIBRARIES} -lavformat${LIBSUF} -lavcodec${LIBSUF} -lavutil${LIBSUF} -lm ${XLIBS}
	INCS=-I${FFMPEG}/libavcodec -I${FFMPEG}/libavformat -I${FFMPEG}/libavutil
endif

ifeq (${SYSTEM},linux)
	XLIBS=-lz
	PREF=
	ARCH=
	LD=${CC} -shared
	LIBNAME=lib${PROJ}.so
	FCOPTS=
	STRIP=${PREF}strip -s
endif
ifeq (${SYSTEM},win32)
	PREF=i586-mingw32msvc-
#	PREF=i686-pc-mingw32-
	ARCH=
	LD=${CC} -D_JNI_IMPLEMENTATION_ -Wl,--kill-a -shared
	LIBNAME=${PROJ}.dll
	XLIBS=
	FCOPTS=--enable-mingw32 --cross-prefix=${PREF} --cross-compile --enable-memalign-hack
	STRIP=${PREF}strip -s
endif
ifeq (${SYSTEM},darwinppc)
	PREF=
	ARCH=-arch ppc
	#LD=${CC} -bundle
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.ppc
	XLIBS=-lz
	FCOPTS=--arch=powerpc --extra-cflags="-fno-common ${ARCH}" --extra-ldflags="${ARCH}" --cross-compile --enable-shared
	STRIP=echo No stripping on 
endif
ifeq (${SYSTEM},darwini386)
	PREF=
	ARCH=
	#LD=${CC} -bundle
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.i386
	XLIBS=-lz
	FCOPTS=--disable-mmx --arch=unknown --extra-cflags=-fno-common --enable-shared
	STRIP=echo No stripping on 
endif

FCONFCOMMON=--disable-debug --enable-gpl --enable-a52 --disable-ffmpeg --disable-ffserver --disable-ffplay --disable-encoders --disable-vhook --disable-ipv6 --disable-network --disable-dv1394 --disable-bktr --disable-v4l2 --disable-v4l --disable-audio-beos --disable-audio-oss

CC=${PREF}gcc ${ARCH}
RANLIB=${PREF}ranlib


MACLIBNAME=lib${PROJ}.jnilib

.SUFFIXES:.o .c

.c.o:
	${CC} ${COMPFLAGS} ${INCS} -I${JAVA_HOME}/include -I${JAVA_HOME}/include/${SYSTEM} -c -o $@ $?


develop:
	make -e `uname -s|tr [A-Z] [a-z]`


linux:distclean
	make -e library SYSTEM=linux
	rm -rf ${JUBLER}/src/lib

linuxdyn:distclean
	make -e library SYSTEM=linux STATIC=no
	rm -rf ${Jubler}/src/lib

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

library:clean ${LIBNAME} libinstall

libinstall:
	mkdir -p ${JUBLER}/dist/lib
	mkdir -p ${JUBLER}/src/lib
	cd ${JUBLER}/dist/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi
	cd ${JUBLER}/src/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi


LIB:${LIBNAME}

${LIBNAME}:${FFMPEGREQ} ${OBJS} ${RANLIBREQ}
	${LD} ${ARCH} -o ${LIBNAME} ${OBJS} ${LIBS}
	${STRIP} ${LIBNAME}


clean:
	rm -f *.o

distclean:clean
	rm -rf ${JUBLER}/dist/lib
	rm -rf ${JUBLER}/src/lib
	rm -f *.dll *.so *.jnilib



native:
	cd ${JUBLER} && ant compile
	cd ${JUBLER}/build/classes && for JO in ${NATIVES} ; do ${JAVAH} -jni $$JO ; done
	mv ${JUBLER}/build/classes/*.h .


ffmpeg:${LIBRARIES}/libavformat${LIBSUF}.a


${LIBRARIES}/libavformat${LIBSUF}.a:
	cd ${FFMPEG} && ./${FFCONF} ${FCONFCOMMON} ${FCOPTS} && make
	mkdir -p ${LIBRARIES}
	cp ${FFMPEG}/libavformat/libavformat.a ${LIBRARIES}/libavformat${LIBSUF}.a
	cp ${FFMPEG}/libavcodec/libavcodec.a ${LIBRARIES}/libavcodec${LIBSUF}.a
	cp ${FFMPEG}/libavutil/libavutil.a ${LIBRARIES}/libavutil${LIBSUF}.a
	cd ${FFMPEG} && make distclean


ranlib:
	${RANLIB} ${LIBRARIES}/libavformat${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavcodec${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavutil${LIBSUF}.a
