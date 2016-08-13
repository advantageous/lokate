package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
import io.advantageous.discovery.DiscoveryServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

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

    @Test
    public void testReadConfFromSystem() {
        List<URI> servers = DnsDiscoveryService.readDnsConf();
        Assert.assertNotNull(servers);
        Assert.assertTrue(servers.size() > 0);
    }
}
