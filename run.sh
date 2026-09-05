#!/usr/bin/env bash
# Startar väderappen (kompilera först om out/ saknas).
cd "$(dirname "$0")"

if [ ! -d out ]; then
    ./build.sh
fi

exec java -cp out Main "$@"