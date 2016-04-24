package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.DiscoveryServiceFactory;

import java.net.URI;
import java.util.List;

public class DnsDiscoveryServiceFactory implements DiscoveryServiceFactory {

    @Override
    public String getScheme() {
        return DnsDiscoveryService.SCHEME;
    }

    @Override
    public DiscoveryService create(final List<URI> uris) {
        if (uris == null || uris.size() == 0)
            throw new IllegalArgumentException("you must specify a configuration URI for the dns discovery service");
        return new DnsDiscoveryService(uris.toArray(new URI[uris.size()]));
    }
}
