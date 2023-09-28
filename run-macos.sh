#!/bin/sh
set -e # do not continue if one command fails

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
JAVA=$DIR/jre/Contents/Home/bin/java
JAR=$DIR/jeniffer2.jar

"$JAVA" -version || (echo "Java was not properly unpacked. Deleting cache, please try again!" && rm -rf $HERE && exit 1)

test -f "$JAR" || (echo "Jeniffer2 was not properly unpacked. Deleting cache, please try again!" && rm -rf $HERE && exit 1)

exec "$JAVA" -XstartOnFirstThread -jar "$JAR" "$@"
