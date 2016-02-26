#!/bin/sh

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/docker-init.sh

# Finally we can push the tagged image
docker push rbmh-docker.docker.rbmhops.net/${PROJECT_NAME}:${PROJECT_VERSION}
