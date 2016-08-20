package io.advantageous.discovery;

import io.advantageous.discovery.impl.DiscoveryServiceImpl;
import io.advantageous.reakt.promise.Promise;

import java.net.URI;
import java.util.List;

/**
 * Look up a service with a discovery URI.
 *
 * @author Geoff Chandler.
 */
public interface DiscoveryService {

    String QUERY_SCHEME = "discovery";
    String RESULT_SCHEME = "service";

    static DiscoveryService create(final URI... endpointConfigurations) {
        if (endpointConfigurations.length == 0) {
            return new DiscoveryServiceImpl(URI.create("dns://CONFIG"));
        } else {
            return new DiscoveryServiceImpl(endpointConfigurations);
        }
    }

    static DiscoveryService create(final List<URI> endpointConfigurations) {
        if (endpointConfigurations.size() == 0) {
            return new DiscoveryServiceImpl(URI.create("dns://CONFIG"));
        } else {
            return new DiscoveryServiceImpl(endpointConfigurations.toArray(new URI[endpointConfigurations.size()]));
        }
    }

    default Promise<List<URI>> lookupService(String query) {
        return lookupService(URI.create(query));
    }

    Promise<List<URI>> lookupService(URI query);
}
