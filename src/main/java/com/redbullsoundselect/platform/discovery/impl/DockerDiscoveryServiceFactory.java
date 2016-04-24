package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.DiscoveryServiceFactory;

import java.net.URI;
import java.util.List;

class DockerDiscoveryServiceFactory implements DiscoveryServiceFactory {

    @Override
    public String getScheme() {
        return DockerDiscoveryService.SCHEME;
    }

    @Override
    public DiscoveryService create(final List<URI> uris) {
        if (uris.size() > 1)
            throw new UnsupportedOperationException("the docker discovery service only supports one configuration.");
        if (uris.size() == 0)
            throw new IllegalArgumentException("you must specify a configuration URI for the docker discovery service");
        return new DockerDiscoveryService(uris.get(0));
    }
}
