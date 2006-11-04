#!/bin/sh
#
#
# This file is part of Jubler.
#
# Jubler is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# Jubler is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Jubler; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

DEBUG=
CHECK_JDK=
VERSION=1.5


# Get options
while [ "$1"x != "x" ]; do
	case $1 in
		-d)
			DEBUG=yes
			;;
		-j)
			CHECK_JDK=yes
			;;
		-v)
			VERSION=$2
			shift 1
			;;
	esac
	shift 1
done



debug () {
	if [ "$DEBUG" ] ; then echo >&2 $@ ; fi
}


# $1: the path to check if this is JDK or a simple java path
check_for_JDK () {
	if [ "$CHECK_JDK" == "" ] ; then
		echo $1
		exit 0
	fi
	JDKBIN=`dirname $1`
	JDK=`dirname $JDKBIN`
	if [ -e $JDK/include/jni.h ] ; then
		export JAVA_HOME=$JDK
		echo $JAVA_HOME
		exit 0
	fi
	debug "  *** Java executable found but not proper JDK subsystem"
}

# #1 : The java binary to search for
check_java_bin () {
	debug Searching for $1
	if [ -x $1 ] ; then
		VERS=`$1 -version 2>&1 | grep 'java.version' | grep $VERSION`
		if [ "$VERS" ] ; then
			check_for_JDK $1
		fi
	fi
}


# $1: java executable to check
check_java () {
	if [ ! -d $1 ] ; then return ; fi
	check_java_bin $1/bin/java
}


# $1: possible java path list
find_in_list () {
	for i in $1; do
		check_java $i
	done
}



# $1: the directory to search for
find_in_paths () {
	DIRS="`ls -1d $1/* 2>/dev/null`"
	DIRS="$1 $DIRS"
	find_in_list "$DIRS"
}

# Search in macosx styled paths
find_in_macosx() {
	check_java /System/Library/Frameworks/JavaVM.framework/Versions/$VERSION/Home
}

# Search in system $PATH
find_in_system_path () {
	OLDIFS=$IFS
	IFS="
"
	check_java_bin $JAVA_HOME/bin/java
	PATHLIST=`echo $PATH | tr ":" "\n"`
	for i in $PATHLIST ; do
		check_java_bin $i/java
	done
	IFS=$OLDIFS
}


debug
debug "Version=$VERSION JDK=$CHECK_JDK"

# Find java
find_in_macosx
find_in_system_path
find_in_paths /usr/java
find_in_paths /usr/share/java
find_in_paths /usr/local/java
find_in_paths /usr/lib/java
find_in_paths /opt
find_in_paths /usr/local
find_in_paths /usr
find_in_paths /usr/lib
find_in_paths /usr/lib/jvm

exit 1
