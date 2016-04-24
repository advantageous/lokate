package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.DiscoveryServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

public class DockerDiscoveryServiceFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWithNullConfig() {
        DiscoveryServiceFactory factory = new DockerDiscoveryServiceFactory();
        factory.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNoConfig() {
        DiscoveryServiceFactory factory = new DockerDiscoveryServiceFactory();
        factory.create(Collections.emptyList());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWithMultipleConfig() {
        DiscoveryServiceFactory factory = new DockerDiscoveryServiceFactory();
        factory.create(Arrays.asList(
                URI.create("docker://foo"),
                URI.create("docker://bar")
        ));
    }

    @Test
    public void testCreate() {
        DiscoveryServiceFactory factory = new DockerDiscoveryServiceFactory();
        DiscoveryService service = factory.create(Collections.singletonList(URI.create("docker://foo")));
        Assert.assertNotNull(service);
    }
}
