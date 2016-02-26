#!/bin/sh

if [[ `uname` == 'Darwin' ]]; then
    eval "$(docker-machine env default)"
fi

PROJECT_VERSION=`grep Project-Version build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.'`
PROJECT_NAME=`grep Project-Name build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.'`

docker build -t rbmh-docker.docker.rbmhops.net/$PROJECT_NAME:$PROJECT_VERSION build/
