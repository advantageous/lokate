package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.DiscoveryServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

public class ConsulDiscoveryServiceFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWithNullConfig() {
        DiscoveryServiceFactory factory = new ConsulDiscoveryServiceFactory();
        factory.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNoConfig() {
        DiscoveryServiceFactory factory = new ConsulDiscoveryServiceFactory();
        factory.create(Collections.emptyList());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWithMultipleConfig() {
        DiscoveryServiceFactory factory = new ConsulDiscoveryServiceFactory();
        factory.create(Arrays.asList(
                URI.create("consul://foo"),
                URI.create("consul://bar")
        ));
    }

    @Test
    public void testCreate() {
        DiscoveryServiceFactory factory = new ConsulDiscoveryServiceFactory();
        DiscoveryService service = factory.create(Collections.singletonList(URI.create("consul://foo")));
        Assert.assertNotNull(service);
    }
}
