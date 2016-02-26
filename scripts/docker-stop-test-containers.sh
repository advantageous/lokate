#!/bin/sh

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/docker-init.sh

if [[ `uname` == 'Darwin' ]]; then
    docker stop docker-http
    docker rm docker-http
fi

docker stop httpd
docker rm httpd
