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


echo -n "Searching for Java..."
JAVABIN=`/bin/sh ./findjava.sh $@`

if [ -n "$JAVABIN" ] ; then
	echo "found in $JAVABIN"
	"$JAVABIN" -jar ./Jubler-install-linux.jar
	exit 0
fi

# If java was found we should have exit now
echo "not found!"
echo -n Press [RETURN] to see additional information on how to resolve this issue.
read NL

cat <<EOF

The autofind script could not locate a valid Java installation of version 1.5 or 6 in your system. If you don't have one, please go to http://www.java.com and download the latest JRE from there.

If the script was unable to find the Java distribution, then you have to declare the top-level installation directory, using the following command:
  export JAVA_HOME=/path/to/java/installation
or if you have csh/tcsh:
  setenv JAVA_HOME /path/to/java/installation
e.g.: export JAVA_HOME=/home/user/jdk1.5.0

To check what is happening, it is possible to rerun the installer using the debug option "-- -d"
e.g.: /bin/sh ./Jubler-VERSION-linux.sh -- -d
Then please cut & copy the produced output and send it to me (panayotis@panayotis.com)

If everything else fails, it is possible to bypass the automatic java path finder. If you have java in your path write the following commands:
  /bin/sh ./Jubler-VERSION-linux.sh --noexec --keep
  cd self
  java -jar Jubler-install-linux.jar
Please don't forget to send me a bug report about this!

EOF
