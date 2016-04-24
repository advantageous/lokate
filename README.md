# Discovery Service
Vert.x service for service discovery.

## Configuration URIs

Docker
```
docker:http://localhost:3500/
```

DNS

```
dns://localhost:8600/
```

Marathon
```
marathon:http://marathon.staging.rbmhops.net:8080/
```

AWS
```
aws:http://ec2.us-west-2.amazonaws.com/?env=staging
```
## Query URIs

Docker Query using default host and port.
```
discovery:docker:///impressions-service?containerPort=8080
```

Docker Query using custom host/port
```
discovery:docker:http://localhost:3500/impressions-service?containerPort=8080
```

DNS A Record Query.
```
discovery:dns:A:///admin.rbss-impressions-service-staging.service.consul?port=9090
discovery:dns:A://localhost:8600/admin.rbss-impressions-service-staging.service.consul?port=8080
```

DNS SVR Record Query.

```
discovery:dns:SRV:///admin.rbss-impressions-service-staging.service.consul
discovery:dns:SRV://localhost:8600/admin.rbss-impressions-service-staging.service.consul
```

Consul Query
```
discovery:consul:///impressions-service?name=eventbus&staging
discovery:consul:http://consul.rbmhops.net:3500/impressions-service?name=eventbus&staging
```

Echo Query.
```
discovery:echo:http://localhost:8080/myservice
```
