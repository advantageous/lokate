#!/bin/sh

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/docker-init.sh

docker build -t rbmh-docker.docker.rbmhops.net/$PROJECT_NAME:$PROJECT_VERSION build/
