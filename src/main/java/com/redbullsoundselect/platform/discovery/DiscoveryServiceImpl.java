package com.redbullsoundselect.platform.discovery;

import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
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
 * discovery:consul:///impressions-service?name=eventbus&staging
 * discovery:consul:http://consul.rbmhops.net:3500/impressions-service?name=eventbus&staging
 * <p>
 * discovery:echo:http://localhost:8080/myservice
 *
 * @author Geoff Chandler
 * @author Rick Hightower
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class DiscoveryServiceImpl implements DiscoveryService {

    private final Map<String, DiscoveryService> discoveryServices = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Create a DiscoveryService with a list of service endpoint configurations.  These configurations will be passed to
     * the factories that are registered for their schemes.
     *
     * @param endpointConfigurations URIs that configure the various discovery service factories
     */
    public DiscoveryServiceImpl(final URI... endpointConfigurations) {

        /*
        First we load all the factories listed in META-INF services into map
         */
        final Map<String, DiscoveryServiceFactory> factoryMap = new HashMap<>();
        load(DiscoveryServiceFactory.class).forEach((factory -> factoryMap.put(factory.getScheme(), factory)));
        load(DiscoveryServiceFactory.class, DiscoveryServiceImpl.class.getClassLoader()).forEach(factory ->
                factoryMap.put(factory.getScheme(), factory)
        );

        /*
        Now we create a configuration map that groups the URIs with the same scheme
         */
        final Map<String, List<URI>> configMap = new HashMap<>();
        Arrays.asList(endpointConfigurations).forEach(uri ->
                configMap.computeIfAbsent(uri.getScheme(), scheme -> new ArrayList<>()).add(uri)
        );

        /*
        Finally we create each discovery service using the grouped configuration and put them in the registry map.
         */
        configMap.entrySet().forEach((entry) -> this.registerService(entry.getKey(),
                factoryMap.computeIfAbsent(entry.getKey(), (scheme) -> {
                    throw new IllegalArgumentException("no factory for scheme " + scheme);
                }).create(entry.getValue()))
        );
    }

    /**
     * Lookup a service with a URI Query.
     *
     * @param query the URI that defines your query
     * @return a service definition that matches your query
     */
    @Override
    public Promise<List<URI>> lookupService(final URI query) {
        logger.debug("looking up service for query: {}", query);
        if (!query.getScheme().equals(QUERY_SCHEME))
            throw new IllegalArgumentException("discovery uris must begin with \"" + QUERY_SCHEME + ":\"");
        return this.discoveryServices.computeIfAbsent(URI.create(query.getSchemeSpecificPart()).getScheme(), (key) -> {
            throw new IllegalArgumentException("discovery scheme not registered: " + QUERY_SCHEME + ":" + key);
        }).lookupService(URI.create(query.getSchemeSpecificPart()));
    }

    /**
     * Register a service for a scheme
     *
     * @param scheme  the discovery scheme for queries
     * @param service the service to register
     */
    void registerService(final String scheme, final DiscoveryService service) {
        Objects.requireNonNull(scheme, "scheme must not be null");
        Objects.requireNonNull(service, "service must be set.");
        logger.info("registering {} to handle discovery for the schema {}", service.getClass().getSimpleName(), scheme);
        this.discoveryServices.put(scheme, service);
    }

    /**
     * Get a list of the registered services
     *
     * @return the list
     */
    List<Class> getRegisteredServiceClasses() {
        return this.discoveryServices.values().stream().map(DiscoveryService::getClass).collect(Collectors.toList());
    }
}
