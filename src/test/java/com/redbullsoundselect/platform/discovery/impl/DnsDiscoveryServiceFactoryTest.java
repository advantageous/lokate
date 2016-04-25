package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.DiscoveryServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

public class DnsDiscoveryServiceFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWithNullConfig() {
        DiscoveryServiceFactory factory = new DnsDiscoveryServiceFactory();
        factory.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNoConfig() {
        DiscoveryServiceFactory factory = new DnsDiscoveryServiceFactory();
        factory.create(Collections.emptyList());
    }

    @Test
    public void testCreate() {
        DiscoveryServiceFactory factory = new DnsDiscoveryServiceFactory();
        DiscoveryService service = factory.create(Collections.singletonList(URI.create("dns://foo")));
        Assert.assertNotNull(service);
    }
}
