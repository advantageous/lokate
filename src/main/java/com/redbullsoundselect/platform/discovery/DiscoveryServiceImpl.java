package com.redbullsoundselect.platform.discovery;

import io.advantageous.reakt.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static java.util.ServiceLoader.load;

/**
 * Discovery service that dispatches queries to registered services based on query URI scheme.
 *
 * @author Geoff Chandler
 * @author Rick Hightower
 */
@SuppressWarnings("WeakerAccess")
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
        Load all the factories listed in META-INF services into map
         */
        final Map<String, DiscoveryServiceFactory> factoryMap = new HashMap<>();
        load(DiscoveryServiceFactory.class).forEach((factory -> factoryMap.put(factory.getScheme(), factory)));
        load(DiscoveryServiceFactory.class, DiscoveryServiceImpl.class.getClassLoader()).forEach(factory ->
                factoryMap.put(factory.getScheme(), factory)
        );

        /*
        Create a configuration map that groups the URIs with the same scheme
         */
        final Map<String, List<URI>> configMap = new HashMap<>();
        Arrays.asList(endpointConfigurations).forEach(uri ->
                configMap.computeIfAbsent(uri.getScheme(), scheme -> new ArrayList<>()).add(uri)
        );

        /*
        Create each discovery service using the grouped configuration and put them in the registry map.
         */
        configMap.entrySet().forEach((entry) -> this.registerService(entry.getKey(),
                factoryMap.computeIfAbsent(entry.getKey(), (scheme) -> {
                    throw new IllegalArgumentException("no factory for scheme " + scheme);
                }).create(entry.getValue()))
        );

        /*
        Create a basic echo service to return a literal of the requested URI
         */
        this.registerService("echo", query -> invokablePromise(promise ->
                promise.resolve(Collections.singletonList(URI.create(query.getSchemeSpecificPart())))));
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

        return invokablePromise(promise -> {
            if (!query.getScheme().equals(QUERY_SCHEME)) {
                promise.reject("discovery uris must begin with \"" + QUERY_SCHEME + ":\"");
                return;
            }
            final String scheme = URI.create(query.getSchemeSpecificPart()).getScheme();
            if (!this.discoveryServices.containsKey(scheme)) {
                promise.reject("discovery scheme not registered: " + QUERY_SCHEME + ":" + scheme);
                return;
            }
            this.discoveryServices.get(scheme).lookupService(URI.create(query.getSchemeSpecificPart()))
                    .invokeWithPromise(promise);
        });
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
