package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
import io.advantageous.discovery.DiscoveryServiceFactory;

import java.net.URI;
import java.util.List;

public class ConsulDiscoveryServiceFactory implements DiscoveryServiceFactory {

    @Override
    public String getScheme() {
        return ConsulDiscoveryService.SCHEME;
    }

    @Override
    public DiscoveryService create(final List<URI> uris) {
        if (uris == null || uris.size() == 0)
            throw new IllegalArgumentException("you must specify a consul configuration URI");
        if (uris.size() > 1)
            throw new UnsupportedOperationException("consul service discovery only accepts one configuration URI");
        return new ConsulDiscoveryService(uris.get(0));
    }
}
