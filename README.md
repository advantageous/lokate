# Discovery Service
Vert.x service for service discovery.

## Configuration URIs


DNS
```
dns://localhost:8600/
```

Consul
```
consul:http://192.168.99.100:8500
```
## Query URIs



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


## Echo

One service
```
discovery:echo:http://foo.com:8080
```

Many
```
discovery:echo:http://foo.com:9090,http://bar.com:9091
```


You can use echo for testing locally. 

## Notes
We used to support Docker, EC2 and Mesos

Docker
```
docker:http://localhost:3500/
```


Docker Query using custom host/port
```
discovery:docker:http://localhost:3500/impressions-service?containerPort=8080
```


Docker Query using default host and port.
```
discovery:docker:///impressions-service?containerPort=8080
```

We dropped the support. We might add it back. 
If we add it back, we will do it as a separate jar file.

We use [JDK services](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html).

Resources/META-INF.services/io.advantageous.discovery.DiscoveryServiceFactory
```
io.advantageous.discovery.impl.DnsDiscoveryServiceFactory
io.advantageous.discovery.impl.ConsulDiscoveryServiceFactory
```

