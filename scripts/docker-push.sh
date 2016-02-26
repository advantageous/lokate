#!/bin/sh

if [[ `uname` == 'Darwin' ]]; then
    eval "$(docker-machine env default)"
fi

# Get the project version from the manifest that we generated when making the jar.
export VER=`grep Project-Version build/tmp/shadowJar/MANIFEST.MF | awk '{print $2}'`
export NAME=`grep Project-Name build/tmp/jar/MANIFEST.MF | awk '{print $2}'`

# Finally we can push the tagged image
docker push rbmh-docker.docker.rbmhops.net/${NAME}:${VER}
