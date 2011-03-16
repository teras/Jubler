#!/bin/sh
#
#
# This file is part of Jubler.
#
# Jubler is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, version 2 of the License.
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
VERSION='1.[567].[0123456789]'
ACCEPT_GCJ=


# Get options
while [ -n "$1" ]; do
	case $1 in
		-d)
			DEBUG=yes
			;;
		-j)
			CHECK_JDK=yes
			;;
		-g)
			ACCEPT_GCJ=yes
			;;
		-v)
			VERSION=$2
			shift 1
			;;
	esac
	shift 1
done



debug () {
	if [ -n "$DEBUG" ] ; then echo >&2 "$@" ; fi
}


# $1: the path to check if this is JDK or a simple java path
check_for_JDK () {
	if [ -z "$CHECK_JDK" ] ; then
		debug ":) JRE found in $1"
		echo $1
		exit 0
	fi
	JDKBIN=`dirname "$1"`
	JDK=`dirname "$JDKBIN"`
	if [ -f "$JDK/include/jni.h" ] ; then
		JAVA_HOME=$JDK
		export JAVA_HOME
		debug ":) JDK found under $JAVA_HOME"
		echo $JAVA_HOME
		exit 0
	fi
	debug '** Java executable found but not proper JDK subsystem'
}

# #1 : The java binary to search for
check_java_bin () {
	debug "-- Searching for $1"
	if [ -x "$1" ] ; then
		JAVARES=`"$1" -version 2>&1`
		VERS=`echo $JAVARES | grep 'java.version' | grep $VERSION`
		if [ "$VERS" ] ; then
			IS_GCJ=`echo $JAVARES | grep gij`
			if [ -z "$IS_GCJ" -o -n "$ACCEPT_GCJ" ] ; then
				check_for_JDK $1
			fi

		fi
	fi
}


# $1: java executable to check
check_java () {
	if [ ! -d "$1" ] ; then return ; fi
	check_java_bin "$1/bin/java"
}


# $1: possible java path list
find_in_list () {
	for i in $1; do
		check_java "$i"
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
	check_java /System/Library/Frameworks/JavaVM.framework/Versions/1.7/Home
	check_java /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home
	check_java /System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home
}

# Search in system $PATH
find_in_system_path () {
	OLDIFS=$IFS
	IFS="
"
	check_java_bin $JAVA_HOME/bin/java
	PATHLIST=`echo $PATH | tr ":" "\n"`
	for i in $PATHLIST ; do
		check_java_bin "$i/java"
	done
	IFS=$OLDIFS
}


debug "!! Information: Version=$VERSION, JDK=$CHECK_JDK, ACCEPT_GCJ=$ACCEPT_GCJ"

# Find java
debug ">> Search in system paths"
find_in_system_path

debug ">> Search in common java directories"
find_in_macosx
find_in_paths /usr/java
find_in_paths /usr/share/java
find_in_paths /usr/local/java
find_in_paths /usr/lib/java
find_in_paths /usr/lib/jvm

debug ">> Looking in common locations"
find_in_paths /opt
find_in_paths /usr/local
find_in_paths /usr
find_in_paths /usr/lib

debug "XX Unable to locate Java"
exit 1
