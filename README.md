# Discovery Service
Vert.x service for service discovery.

## Configuration URIs
docker:http://localhost:3500/
dns://localhost:8600/
marathon:http://marathon.staging.rbmhops.net:8080/
aws:http://ec2.us-west-2.amazonaws.com/?env=staging

## Query URIs
discovery:docker:///impressions-service?containerPort=8080
discovery:docker:http://localhost:3500/impressions-service?containerPort=8080

discovery:dns:A:///admin.rbss-impressions-service-staging.service.consul?port=9090
discovery:dns:A://localhost:8600/admin.rbss-impressions-service-staging.service.consul?port=8080

discovery:dns:SRV:///admin.rbss-impressions-service-staging.service.consul
discovery:dns:SRV://localhost:8600/admin.rbss-impressions-service-staging.service.consul

discovery:consul:///impressions-service?name=eventbus&staging
discovery:consul:http://consul.rbmhops.net:3500/impressions-service?name=eventbus&staging

discovery:echo:http://localhost:8080/myservice
