package com.redbullsoundselect.platform.discovery;

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

    default Promise<List<URI>> lookupService(String query) {
        return lookupService(URI.create(query));
    }

    Promise<List<URI>> lookupService(URI query);

}
