#!/bin/sh

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/docker-init.sh

MARATHON_APP_ID="rbmh-discovery-service-staging"
MESOS_URL="http://mesos-master1.rbss.staging.rbmhops.net:8080/v2/apps"

RESPONSE=`curl -I $MESOS_URL/$MARATHON_APP_ID 2>/dev/null | head -n 1 | awk -F" " '{print $2}'`

if [ 200 -eq $RESPONSE ]; then
	curl -X PUT -H "Content-Type: application/json" $MESOS_URL/$MARATHON_APP_ID -d @"build/mesos.deploy.json"
else
	curl -X POST -H "Content-Type: application/json" $MESOS_URL -d @"build/mesos.deploy.json"
fi
