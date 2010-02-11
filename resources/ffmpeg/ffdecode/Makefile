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
JAVA_HOME=${shell /bin/sh ../../system/findjava.sh -j}

# Current Path
CPATH=${shell pwd}

# Path of the local ffmpeg source structure (already inside the source version of Jubler)
FFMPEG=${CPATH}/../ffmpeg-0.5

# Path of local FAAD2 library
FAAD=${CPATH}/../ffmpeg-libs/faad2

# Use this path for static libraries
LIBRARIES=${CPATH}/../libraries

# Path of Jubler distribution
JUBLER=${CPATH}/../../..

# RELATIVE Path of ffdecode to top Jubler directory
FFDECODE=resources/ffmpeg/ffdecode

# Where javah lives
JAVAH=${JAVA_HOME}/bin/javah

# Where Windows include files for JNI can be found
WININCLOC=${JUBLER}/../../Tools/jvm/include

# Various options
FFCONF=configure
LIBSUF=_${SYSTEM}.${ARCH}
MAKEOPTS=-j3

# Find requested files
NATIVES=${shell cd ../../src && grep -r ' native ' * | grep -v '/.svn/' | gawk -F ':' '{print $$1}' | uniq | sed -e 's/\.java$$//g' | tr '/' '.' }
SRCS=${shell ls 2>/dev/null *.c}
OBJS=${SRCS:.c=.o}
SYSTEM=${shell echo ${MAKESYS} | sed -e 's/_.*//g'}
ARCHITECTURE=${shell uname -m}

ifeq (${STATIC},no)
	LIBRARYREQ=
	RANLIBREQ=
	LIBS=${shell pkg-config --libs libavformat libavutil libavcodec libswscale}
	INCS=${shell pkg-config --cflags libavformat libavutil libavcodec libswscale}
else
	LIBRARYREQ=${LIBRARIES}/libavformat${LIBSUF}.a
	RANLIBREQ=ranlib
	LIBS=-L${LIBRARIES} -lavformat${LIBSUF} -lavcodec${LIBSUF} -lavutil${LIBSUF} -lfaad${LIBSUF} -lm ${XLIBS}
	INCS=-I${FFMPEG}
endif

ifeq (${MAKESYS},win32)
	JINCLUDE=-I${WININCLOC} -I${WININCLOC}/win32
else
	JINCLUDE=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/${SYSTEM}
endif

ifeq (${MAKESYS},win32)
	XLIBS=
	M586=$(shell which i586-mingw32msvc-gcc)
	CCPREF=$(shell if test Q != ${M586}Q  ; then echo i586-mingw32msvc- ; else echo i386-mingw32- ; fi)
	ARCH=i386
	LD=${CC} -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -shared
	LIBNAME=${PROJ}.dll
	XFCONFIG=--cross-prefix=${CCPREF} --enable-memalign-hack --target-os=mingw32
	GCCOPTS=-D_JNI_IMPLEMENTATION_
	STRIP=${CCPREF}strip -s
endif
ifeq (${MAKESYS},darwin_ppc)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=ppc
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.ppc
	XFCONFIG=--enable-shared --arch=powerpc --enable-cross-compile --disable-altivec
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif
ifeq (${MAKESYS},darwin_x86_64)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=x86_64
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.x86_64
	XFCONFIG=--enable-shared --arch=x86_64
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif
ifeq (${MAKESYS},darwin_i386)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=i386
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.i386
	XFCONFIG=--enable-shared --arch=unknown
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif
ifeq (${MAKESYS},linux_i686)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=${ARCHITECTURE}
	LD=${CC} -shared
	LIBNAME=lib${PROJ}.so
	XFCONFIG=
	GCCOPTS=
	STRIP=${CCPREF}strip -s
endif
ifeq (${MAKESYS},linux_x86_64)
	XLIBS=-lz
	CCPREF=
	ARCH=${ARCHITECTURE}
	LD=${CC} -shared
	LIBNAME=lib${PROJ}.so
	XFCONFIG=--disable-mmx
	GCCOPTS=-fPIC
	STRIP=${CCPREF}strip -s
endif


FCONFIG=--disable-debug --enable-gpl --disable-ffmpeg --disable-ffserver --disable-ffplay --disable-encoders --enable-encoder=pcm_s16le --enable-encoder=pcm_s16be --disable-muxers --enable-muxer=wav --disable-devices --disable-vhook --disable-ipv6 --disable-network --disable-libdc1394 --enable-libfaad --extra-cflags="-I${FAAD}/include ${GCCOPTS}"  --extra-ldflags="-L${LIBRARIES} ${GCCOPTS}" ${XFCONFIG}

CCPREF_NOS=${shell echo ${CCPREF} | sed -e 's/-$$//g' }
DCONFIG=--with-mp4v2 --host=${CCPREF_NOS}

CC=${CCPREF}gcc
RANLIB=${CCPREF}ranlib

ifeq (${NOSTRIP},) 
	STRIPCMD=${STRIP}
else
	STRIPCMD=@echo Bypass stripping
endif


MACLIBNAME=lib${PROJ}.jnilib

.SUFFIXES:.o .c

.c.o:
	${CC} ${CFLAGS} ${GCCOPTS} -std=c99 -pedantic -O3 -Wall ${INCS} ${JINCLUDE} -c -o $@ $?


develop:javacheck
	${MAKE} -e `uname -s|tr [A-Z] [a-z]`


linux:linux_${ARCHITECTURE}

linux_i686:javacheck distclean
	${MAKE} -e library MAKESYS=linux_i686
	rm -rf ${JUBLER}/src/lib

linux_x86_64:javacheck distclean
	${MAKE} -e library MAKESYS=linux_x86_64
	rm -rf ${JUBLER}/src/lib

linuxdyn:linuxdyn_${ARCHITECTURE}

linuxdyn_i686:javacheck distclean
	${MAKE} -e library MAKESYS=linux_i686 STATIC=no
	rm -rf ${Jubler}/src/lib

linuxdyn_x86_64:javacheck distclean
	${MAKE} -e library MAKESYS=linux_x86_64 STATIC=no
	rm -rf ${Jubler}/src/lib

win32:javacheck distclean
	${MAKE} -e library MAKESYS=win32
	rm -rf ${JUBLER}/src/lib


darwin:javacheck
	${MAKE} distclean
	${MAKE} -e LIB MAKESYS=darwin_x86_64
	${MAKE} distclean
	${MAKE} -e LIB MAKESYS=darwin_ppc
	${MAKE} distclean
	${MAKE} -e LIB MAKESYS=darwin_i386
	${MAKE} distclean
	lipo ${PROJ}.ppc ${PROJ}.i386 ${PROJ}.x86_64 -create -output ${MACLIBNAME}
	TIGER=`otool -vR ${MACLIBNAME} | grep UNIX` ; if [ -n "$$TIGER" ] ; then echo "Tiger compatibility problem!" ; echo $$TIGER ; exit 1 ; fi
	rm -f ${PROJ}.ppc ${PROJ}.i386 ${PROJ}.x86_64
	${MAKE} -e libinstall LIBNAME=${MACLIBNAME}

library:clean ${LIBNAME} libinstall

libinstall:
	mkdir -p ${JUBLER}/dist/lib
	mkdir -p ${JUBLER}/src/lib
	cd ${JUBLER}/dist/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi
	cd ${JUBLER}/src/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi


LIB:${LIBNAME}

${LIBNAME}:${LIBRARYREQ} ${OBJS} ${RANLIBREQ}
	${LD} ${GCCOPTS} -o ${LIBNAME} ${OBJS} ${LIBS}
	${STRIPCMD} ${LIBNAME}


clean:
	rm -f *.o

distclean:clean
	rm -rf ${JUBLER}/dist/lib/*ffdecode*
	rm -rf ${JUBLER}/src/lib/*ffdecode*
	rm -f *.dll *.so *.jnilib

destroy:
	rm -rf ../libraries

javacheck:
ifeq (${JAVA_HOME},)
	@echo "** Java Home not found"
	exit 1
endif


native:
	cd ${JUBLER} && ant compile
	cd ${JUBLER}/build/classes && for JO in ${NATIVES} ; do ${JAVAH} -jni $$JO ; done
	mv ${JUBLER}/build/classes/*.h .



${LIBRARIES}/libfaad${LIBSUF}.a:
	cd ${FAAD} && CFLAGS="${GCCOPTS}" LDFLAGS="${GCCOPTS}" ./configure ${DCONFIG} && mv frontend/Makefile frontend/Makefile.orig && echo >frontend/Makefile "all:" && ${MAKE} -j3
	mkdir -p ${LIBRARIES}
	cp ${FAAD}/libfaad/.libs/libfaad.a ${LIBRARIES}/libfaad${LIBSUF}.a
	cd ${FAAD} && mv frontend/Makefile.orig frontend/Makefile && ${MAKE} distclean

${LIBRARYREQ}: ${LIBRARIES}/libfaad${LIBSUF}.a
	mkdir -p ${LIBRARIES}
	cd ${LIBRARIES} ; rm -f libfaad.a && ln -s libfaad${LIBSUF}.a libfaad.a
	cd ${FFMPEG} && ./${FFCONF} ${FCONFIG} && ${MAKE} -j3
	cp ${FFMPEG}/libavformat/libavformat.a ${LIBRARIES}/libavformat${LIBSUF}.a
	cp ${FFMPEG}/libavcodec/libavcodec.a ${LIBRARIES}/libavcodec${LIBSUF}.a
	cp ${FFMPEG}/libavutil/libavutil.a ${LIBRARIES}/libavutil${LIBSUF}.a
	cd ${FFMPEG} && ${MAKE} distclean
	rm -f ${FFMPEG}/libswscale/libswscale-uninstalled.pc ${FFMPEG}/libswscale/libswscale.pc
	rm -f ${LIBRARIES}/libfaad.a


ranlib:
	${RANLIB} ${LIBRARIES}/libfaad${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavformat${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavcodec${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavutil${LIBSUF}.a
