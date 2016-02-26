#!/bin/sh

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/docker-init.sh

# Get the project version from the manifest that we generated when making the jar.
PROJECT_VERSION=`grep Project-Version build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.'`
PROJECT_NAME=`grep Project-Name build/tmp/jar/MANIFEST.MF | awk '{print $2}' | tr -dc '[:alnum:]\.'`

# Finally we can push the tagged image
docker push rbmh-docker.docker.rbmhops.net/${PROJECT_NAME}:${PROJECT_VERSION}
