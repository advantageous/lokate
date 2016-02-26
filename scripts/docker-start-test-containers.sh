#!/bin/sh

if [[ `uname` == 'Darwin' ]]; then
    eval "$(docker-machine env default)"
    docker run -d -p 2375:2375 --volume=/var/run/docker.sock:/var/run/docker.sock --name=docker-http sequenceiq/socat
fi

docker run -d -P --name=httpd httpd
sleep 3
