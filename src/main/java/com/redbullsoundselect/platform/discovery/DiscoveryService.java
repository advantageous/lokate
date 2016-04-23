package com.redbullsoundselect.platform.discovery;

import io.advantageous.reakt.promise.Promise;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Look up a service with a discovery URI.
 *
 * @author Geoff Chandler.
 */
public interface DiscoveryService {

    ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    String QUERY_SCHEME = "discovery";
    String RESULT_SCHEME = "service";

    default Promise<URI> lookupRandomService(URI query) {
        return lookupService(query).thenMap(list -> list.get(Math.abs(RANDOM.nextInt()) % (list.size() - 1)));
    }

    default Promise<URI> lookupFirstService(URI query) {
        return lookupService(query).thenMap(list -> list.get(0));
    }

    default Promise<List<URI>> lookupService(String query) {
        return lookupService(URI.create(query));
    }

    Promise<List<URI>> lookupService(URI query);

}
