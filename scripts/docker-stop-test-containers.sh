#!/bin/sh

if [[ `uname` == 'Darwin' ]]; then
    eval "$(docker-machine env default)"
    docker stop docker-http
    docker rm docker-http
fi

docker stop httpd
docker rm httpd
