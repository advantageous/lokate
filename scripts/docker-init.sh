#!/bin/sh

if [[ `uname` == 'Darwin' ]]; then
    docker-machine start default
    eval "$(docker-machine env default)"
fi

# Get the project version and name from the manifest that we generated when making the jar.
export PROJECT_VERSION=`grep Project-Version build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.'`
export PROJECT_NAME=`grep Project-Name build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.-'`
