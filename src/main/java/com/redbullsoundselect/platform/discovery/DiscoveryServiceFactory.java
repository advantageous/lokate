package com.redbullsoundselect.platform.discovery;

import java.net.URI;
import java.util.List;

/**
 * Interface for discovery service factories.  These are used by the service loader to create discovery services
 * from configuration URIs.
 */
public interface DiscoveryServiceFactory {

    String getScheme();

    DiscoveryService create(List<URI> uris);
}
