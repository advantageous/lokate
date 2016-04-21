package com.redbullsoundselect.platform.discovery;

import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.ServiceLoader.load;

/**
 * docker:http://localhost:3500/
 * dns://localhost:8600/
 * marathon:http://marathon.staging.rbmhops.net:8080/
 * aws:http://ec2.us-west-2.amazonaws.com/?env=staging
 * <p>
 * discovery:docker:///impressions-service?containerPort=8080
 * discovery:docker:http://localhost:3500/impressions-service?containerPort=8080
 * <p>
 * discovery:dns:A:///admin.rbss-impressions-service-staging.service.consul?port=9090
 * discovery:dns:A://localhost:8600/admin.rbss-impressions-service-staging.service.consul?port=8080
 * <p>
 * discovery:dns:SRV:///admin.rbss-impressions-service-staging.service.consul
 * discovery:dns:SRV://localhost:8600/admin.rbss-impressions-service-staging.service.consul
 * <p>
 * discovery:marathon:///impressions-service?portIndex=0
 * discovery:marathon:http://marathon.rbmhops.net:8080/impressions-service?portIndex=0
 * <p>
 * discovery:consul:///impressions-service?name=eventbus&staging
 * discovery:consul:http://consul.rbmhops.net:3500/impressions-service?name=eventbus&staging
 * <p>
 * discovery:aws:///impressions-service?port=8080
 * discovery:aws:http://ec2.us-west-2.amazonaws.com/impressions-service?port=8080
 * <p>
 * discovery:echo:http://localhost:8080/myservice
 *
 * @author rick and geoff
 */
public class DiscoveryServiceImpl implements DiscoveryService {

    private final Map<String, DiscoveryService> discoveryServices = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Create a DiscoveryService with a list of service endpoint configurations.  These configurations will be passed to
     * the factories that are registered for their schemes.
     *
     * @param endpointConfigurations list of URIs that configure the various discovery service factories
     */
    public DiscoveryServiceImpl(final List<URI> endpointConfigurations) {

        final List<DiscoveryServiceFactory> factoryList = new ArrayList<>();
        load(DiscoveryServiceFactory.class).forEach(factoryList::add);
        load(DiscoveryServiceFactory.class, DiscoveryServiceImpl.class.getClassLoader()).forEach(factoryList::add);
        if (factoryList.isEmpty()) throw new IllegalStateException("Cannot find Factory under META-INF/services/"
                + DiscoveryServiceFactory.class.getName() + " on classpath");
        factoryList.forEach(factory -> {
            logger.debug("Using DiscoveryService {} with scheme {}", factory.getClass().getName(), factory.getScheme());
            discoveryServices.put(factory.getScheme(), factory.create(endpointConfigurations.stream()
                    .filter(uri -> uri.getScheme().equals(factory.getScheme()))
                    .collect(Collectors.toList()))
            );
        });
    }

    @Override
    public Promise<ServiceDefinition> lookupService(final URI query) {
        if (!query.getScheme().equals(QUERY_SCHEME))
            throw new IllegalArgumentException("discovery uris must begin with \"" + QUERY_SCHEME + ":\"");
        return this.discoveryServices.computeIfAbsent(URI.create(query.getSchemeSpecificPart()).getScheme(), (key) -> {
            throw new IllegalArgumentException("discovery scheme not registered: " + QUERY_SCHEME + ":" + key);
        }).lookupService(query);
    }

}
