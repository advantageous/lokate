package com.redbullsoundselect.platform.discovery;

import java.net.URI;
import java.util.List;

/**
 * Created by gcc on 4/20/16.
 */
public interface DiscoveryServiceFactory {

    String getScheme();

    DiscoveryService create(List<URI> uris);
}
