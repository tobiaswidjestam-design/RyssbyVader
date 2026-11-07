#!/usr/bin/env bash
# Starts the weather app (compiles first if out/ is missing).
cd "$(dirname "$0")"

if [ ! -d out ]; then
    ./build.sh
fi

exec java -cp out Main "$@"