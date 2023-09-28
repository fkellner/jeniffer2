#!/usr/bin/env bash
set -e # do not continue if one command fails

HERE=${BASH_SOURCE%/*}

"$HERE/jre/bin/java" -version || (echo "Java was not properly unpacked. Deleting cache, please try again!" && rm -rf $HERE && exit 1)

test -f "$HERE/jeniffer2.jar" || (echo "Jeniffer2 was not properly unpacked. Deleting cache, please try again!" && rm -rf $HERE && exit 1)

"$HERE/jre/bin/java" -jar "$HERE/jeniffer2.jar" "$@"
