package com.redbullsoundselect.platform.discovery;

import io.advantageous.reakt.promise.Promise;

import java.net.URI;

/**
 * Look up a service with a discovery URI.
 *
 * @author Geoff Chandler.
 */
public interface DiscoveryService {

    String QUERY_SCHEME = "discovery";

    default Promise<ServiceDefinition> lookupService(String query) {
        return lookupService(URI.create(query));
    }

    Promise<ServiceDefinition> lookupService(URI query);

}
