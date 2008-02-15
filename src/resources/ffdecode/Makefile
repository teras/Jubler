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

# Current Path
CPATH=${shell pwd}

# Path of the local ffmpeg source structure (already inside the source version of Jubler)
FFMPEG=${CPATH}/../ffmpeg-svn

# Path of local FAAD2 library
FAAD=${CPATH}/../ffmpeg-libs/faad2

# Use this path for static libraries
LIBRARIES=${CPATH}/../libraries

# Path of Jubler distribution
JUBLER=${CPATH}/../..

# RELATIVE Path of ffdecode to top Jubler directory
FFDECODE=resources/ffdecode

# Where javah lives
JAVAH=${JAVA_HOME}/bin/javah

# Various options
FFCONF=configure
LIBSUF=_${SYSTEM}.${ARCH}
MAKEOPTS=-j3

# Find requested files
NATIVES=${shell cd ../../src && grep -r ' native ' * | grep -v '/.svn/' | gawk -F ':' '{print $$1}' | uniq | sed -e 's/\.java$$//g' | tr '/' '.' }
SRCS=${shell ls 2>/dev/null *.c}
OBJS=${SRCS:.c=.o}


ifeq (${STATIC},no)
	LIBRARYREQ=
	RANLIBREQ=
	LIBS=${shell pkg-config --libs libavformat libavutil libavcodec}
	INCS=${shell pkg-config --cflags libavformat libavutil libavcodec}
else
	LIBRARYREQ=libreqs
	RANLIBREQ=ranlib
	LIBS=-L${LIBRARIES} -lavformat${LIBSUF} -lavcodec${LIBSUF} -lavutil${LIBSUF} -lfaad${LIBSUF} -lm ${XLIBS}
	INCS=-I${FFMPEG}/libavcodec -I${FFMPEG}/libavformat -I${FFMPEG}/libavutil -I${FAAD}/include
endif

ifeq (${SYSTEM},linux)
	XLIBS=-lz
	PREF=
	ARCH=${shell uname -m}
	LD=${CC} -shared
	LIBNAME=lib${PROJ}.so
	XFCONFIG=
	GCCOPTS=-fPIC
	STRIP=${PREF}strip -s
endif
ifeq (${SYSTEM},win32)
	XLIBS=
	PREF=i586-mingw32msvc-
	ARCH=i386
	LD=${CC} -D_JNI_IMPLEMENTATION_ -Wl,--kill-a -shared
	LIBNAME=${PROJ}.dll
	XFCONFIG=--cross-prefix=${PREF} --enable-memalign-hack --target-os=mingw32
	GCCOPTS=
	STRIP=${PREF}strip -s
endif
ifeq (${SYSTEM},darwinppc)
	XLIBS=-lz
	PREF=
	ARCH=ppc
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.ppc
	XFCONFIG=--enable-shared --arch=powerpc --cross-compile
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif
ifeq (${SYSTEM},darwini386)
	XLIBS=-lz
	PREF=
	ARCH=i386
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.i386
	XFCONFIG=--enable-shared --arch=unknown
	GCCOPTS=-fno-common -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif


FCONFIG=--disable-debug --enable-gpl --disable-ffmpeg --disable-ffserver --disable-ffplay --disable-encoders --disable-muxers --enable-muxer=wav --disable-vhook --disable-ipv6 --disable-network --disable-libdc1394 --enable-libfaad --extra-cflags="-I${FAAD}/include ${GCCOPTS}"  --extra-ldflags="-L${LIBRARIES} ${GCCOPTS}" ${XFCONFIG} --disable-mmx

CC=${PREF}gcc
RANLIB=${PREF}ranlib


MACLIBNAME=lib${PROJ}.jnilib

.SUFFIXES:.o .c

.c.o:
	${CC} ${CFLAGS} ${GCCOPTS} -std=c99 -pedantic -O3 -Wall ${INCS} -I${JAVA_HOME}/include -I${JAVA_HOME}/include/${SYSTEM} -c -o $@ $?


develop:javacheck
	make -e `uname -s|tr [A-Z] [a-z]`


linux:javacheck distclean
	make -e library SYSTEM=linux
	rm -rf ${JUBLER}/src/lib

linuxdyn:javacheck distclean
	make -e library SYSTEM=linux STATIC=no
	rm -rf ${Jubler}/src/lib

win32:javacheck distclean
	make -e library SYSTEM=win32
	rm -rf ${JUBLER}/src/lib

darwin:javacheck
	make darwinppc
	make darwini386
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

${LIBNAME}:${LIBRARYREQ} ${OBJS} ${RANLIBREQ}
	${LD} ${GCCOPTS} -o ${LIBNAME} ${OBJS} ${LIBS}
	${STRIP} ${LIBNAME}


clean:
	rm -f *.o

distclean:clean
	rm -rf ${JUBLER}/dist/lib
	rm -rf ${JUBLER}/src/lib
	rm -f *.dll *.so *.jnilib

javacheck:
ifeq (${JAVA_HOME},)
	@echo "** Java Home not found"
	exit 1
endif


native:
	cd ${JUBLER} && ant compile
	cd ${JUBLER}/build/classes && for JO in ${NATIVES} ; do ${JAVAH} -jni $$JO ; done
	mv ${JUBLER}/build/classes/*.h .


libreqs:${LIBRARIES}/libavformat${LIBSUF}.a

${LIBRARIES}/libfaad${LIBSUF}.a:
	cd ${FAAD} && CFLAGS="${GCCOPTS}" LDFLAGS="${GCCOPTS}" ./configure --with-mp4v2 && make -j3
	mkdir -p ${LIBRARIES}
	cp ${FAAD}/libfaad/.libs/libfaad.a ${LIBRARIES}/libfaad${LIBSUF}.a
	cd ${FAAD} && make distclean

${LIBRARIES}/libavformat${LIBSUF}.a: ${LIBRARIES}/libfaad${LIBSUF}.a
	mkdir -p ${LIBRARIES}
	cd ${LIBRARIES} ; rm -f libfaad.a && ln -s libfaad${LIBSUF}.a libfaad.a
	cd ${FFMPEG} && ./${FFCONF} ${FCONFIG} && make -j3
	cp ${FFMPEG}/libavformat/libavformat.a ${LIBRARIES}/libavformat${LIBSUF}.a
	cp ${FFMPEG}/libavcodec/libavcodec.a ${LIBRARIES}/libavcodec${LIBSUF}.a
	cp ${FFMPEG}/libavutil/libavutil.a ${LIBRARIES}/libavutil${LIBSUF}.a
	cd ${FFMPEG} && make distclean


ranlib:
	${RANLIB} ${LIBRARIES}/libavformat${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavcodec${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavutil${LIBSUF}.a
