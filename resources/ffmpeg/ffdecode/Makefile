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
FFMPEG=${CPATH}/../ffmpeg-0.7.12

# Use this path for static libraries
LIBRARIES=${CPATH}/../libraries

# Path of Jubler distribution
JUBLER=${CPATH}/../../..

# RELATIVE Path of ffdecode to top Jubler directory
FFDECODE=resources/ffmpeg/ffdecode

# Where javah lives
JAVAH=javah

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
	LIBS=-L${LIBRARIES} -lswscale${LIBSUF} -lavformat${LIBSUF} -lavcodec${LIBSUF} -lavutil${LIBSUF} -lm ${XLIBS}
	INCS=-I${FFMPEG} -I${LIBRARIES}/include/${LIBSUF}
endif


ifeq (${MAKESYS},win32)
	JINCLUDE=-I${WININCLOC} -I${WININCLOC}/win32
else
ifeq (${MAKESYS},win64)
	JINCLUDE=-I${WININCLOC} -I${WININCLOC}/win32
else
	JINCLUDE=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/${SYSTEM} -I/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -I/Developer/SDKs/MacOSX10.7.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
endif
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
ifeq (${MAKESYS},win64)
	XLIBS=
	CCPREF=x86_64-w64-mingw32-
	ARCH=x86_64
	LD=${CC} -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -shared
	LIBNAME=${PROJ}.dll
	XFCONFIG=--cross-prefix=${CCPREF} --enable-memalign-hack --target-os=mingw32 --disable-dxva2 --arch=${ARCH}
	GCCOPTS=-D_JNI_IMPLEMENTATION_
	STRIP=${CCPREF}strip -s
endif
ifeq (${MAKESYS},darwin_x86_64)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=x86_64
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.x86_64
	XFCONFIG=--enable-shared --arch=x86_64 --target-os=darwin
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4
	STRIP=echo No stripping on 
endif
ifeq (${MAKESYS},darwin_i386)
	XLIBS=-lz -lbz2
	CCPREF=
	ARCH=i386
	LD=${CC} -dynamiclib -framework JavaVM
	LIBNAME=${PROJ}.i386
	XFCONFIG=--enable-shared --arch=x86 --target-os=darwin --disable-mmx --disable-mmx2 --disable-sse --disable-ssse3 --disable-amd3dnow --disable-amd3dnowext --enable-pic
	GCCOPTS=-fno-common -arch ${ARCH} -mmacosx-version-min=10.4 -read_only_relocs suppress
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

FCONFIG=--disable-debug --enable-gpl --enable-nonfree --enable-pthreads --disable-ffmpeg --disable-ffserver --disable-ffplay --disable-ffprobe --disable-encoders --enable-encoder=pcm_s16le --enable-encoder=pcm_s16be --disable-muxers --enable-muxer=wav --disable-devices --disable-doc --disable-network --disable-libdc1394 --disable-postproc --extra-cflags="-I${FAAD}/include ${GCCOPTS}"  --extra-ldflags="-L${LIBRARIES} ${GCCOPTS}" ${XFCONFIG}

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

win64:javacheck distclean
	${MAKE} -e library MAKESYS=win64
	rm -rf ${JUBLER}/src/lib
	
darwin:javacheck
	${MAKE} distclean
	${MAKE} -e LIB MAKESYS=darwin_i386
	${MAKE} distclean
	${MAKE} -e LIB MAKESYS=darwin_x86_64
	${MAKE} distclean
	lipo ${PROJ}.i386 ${PROJ}.x86_64 -create -output ${MACLIBNAME}
	TIGER=`otool -vR ${MACLIBNAME} | grep UNIX` ; if [ -n "$$TIGER" ] ; then echo "Tiger compatibility problem!" ; echo $$TIGER ; exit 1 ; fi
	rm -f ${PROJ}.i386 ${PROJ}.x86_64
	${MAKE} -e libinstall LIBNAME=${MACLIBNAME}

library:clean ${LIBNAME} libinstall

libinstall:
	mkdir -p ${JUBLER}/dist/lib
	mkdir -p ${JUBLER}/src/lib
	cd ${JUBLER}/dist/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi
	cd ${JUBLER}/src/lib && if [ ! -e ${LIBNAME} ] ; then ln -s ../../${FFDECODE}/${LIBNAME} . ; fi


LIB:${LIBNAME}

${LIBNAME}:${LIBRARYREQ} ${OBJS} ${RANLIBREQ}
	${LD} ${LDFLAGS} ${GCCOPTS} -o ${LIBNAME} ${OBJS} ${LIBS}
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



${LIBRARYREQ}:
	mkdir -p ${LIBRARIES}
	cd ${FFMPEG} && ./${FFCONF} ${FCONFIG} && ${MAKE} -j3
	cp ${FFMPEG}/libavformat/libavformat.a ${LIBRARIES}/libavformat${LIBSUF}.a
	cp ${FFMPEG}/libavcodec/libavcodec.a ${LIBRARIES}/libavcodec${LIBSUF}.a
	cp ${FFMPEG}/libavutil/libavutil.a ${LIBRARIES}/libavutil${LIBSUF}.a
	cp ${FFMPEG}/libswscale/libswscale.a ${LIBRARIES}/libswscale${LIBSUF}.a
	mkdir -p ${LIBRARIES}/include/${LIBSUF}/libavutil
	cp ${FFMPEG}/libavutil/avconfig.h ${LIBRARIES}/include/${LIBSUF}/libavutil
	cd ${FFMPEG} && ${MAKE} distclean
	rm -f ${FFMPEG}/libswscale/libswscale-uninstalled.pc ${FFMPEG}/libswscale/libswscale.pc


ranlib:
	${RANLIB} ${LIBRARIES}/libavformat${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavcodec${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libavutil${LIBSUF}.a
	${RANLIB} ${LIBRARIES}/libswscale${LIBSUF}.a
