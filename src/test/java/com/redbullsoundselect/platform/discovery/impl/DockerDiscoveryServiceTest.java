package com.redbullsoundselect.platform.discovery.impl;

import com.redbullmediahouse.platform.config.ConfigUtils;
import com.redbullsoundselect.platform.discovery.ServiceDefinition;
import com.typesafe.config.Config;
import io.vertx.core.Vertx;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class DockerDiscoveryServiceTest {

    protected static DockerDiscoveryService discoveryService;

    @BeforeClass
    public static void setup() {

        System.out.println("DOCKER HOST IS SET TO " + System.getenv("DOCKER_HOST"));
        final Config dockerConfig =
                ConfigUtils.getConfig("com.redbullsoundselect.platform.discovery").getConfig("provider-config.docker");
        discoveryService = new DockerDiscoveryService(
                Vertx.vertx(),
                URI.create(dockerConfig.getString("host")).getHost(),
                dockerConfig.getInt("port")
        );
    }

    @Test
    public void testLookupByName() throws Exception {

        AtomicReference<ServiceDefinition> resultAtomicReference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        discoveryService.lookupServiceByName(serviceDefinitionAsyncResult -> {
            resultAtomicReference.set(serviceDefinitionAsyncResult);
            latch.countDown();
        }, "httpd");

        latch.await(5, TimeUnit.SECONDS);

        assertNotNull(resultAtomicReference.get());
        ServiceDefinition serviceDefinition = resultAtomicReference.get();

        assertFalse(serviceDefinition.getAddress().isEmpty());
    }

    @Test
    public void testLookupByNameAndPort() throws Exception {

        AtomicReference<ServiceDefinition> resultAtomicReference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        discoveryService.lookupServiceByNameAndContainerPort(serviceDefinitionAsyncResult -> {
            resultAtomicReference.set(serviceDefinitionAsyncResult);
            latch.countDown();
        }, "httpd", 80);

        latch.await(5, TimeUnit.SECONDS);

        assertNotNull(resultAtomicReference.get());
        ServiceDefinition serviceDefinition = resultAtomicReference.get();

        assertFalse(serviceDefinition.getAddress().isEmpty());
    }

}
