package com.redbullmediahouse.platform.discovery.impl;

import com.redbullmediahouse.platform.config.ConfigUtils;
import com.redbullmediahouse.platform.discovery.ServiceDefinition;
import com.typesafe.config.Config;
import io.vertx.core.AsyncResult;
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
        final Config dockerConfig =
                ConfigUtils.getConfig("com.redbullmediahouse.platform.discovery").getConfig("provider-config.docker");
        discoveryService = new DockerDiscoveryService(
                Vertx.vertx(),
                URI.create(dockerConfig.getString("host")).getHost(),
                dockerConfig.getInt("port")
        );
    }

    @Test
    public void testLookupByName() throws Exception {

        AtomicReference<AsyncResult<ServiceDefinition>> resultAtomicReference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        discoveryService.lookupServiceByName("httpd", serviceDefinitionAsyncResult -> {
            resultAtomicReference.set(serviceDefinitionAsyncResult);
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        assertNotNull(resultAtomicReference.get());
        assertTrue(resultAtomicReference.get().succeeded());
        ServiceDefinition serviceDefinition = resultAtomicReference.get().result();

        assertFalse(serviceDefinition.getAddress().isEmpty());
    }

    @Test
    public void testLookupByNameAndPort() throws Exception {

        AtomicReference<AsyncResult<ServiceDefinition>> resultAtomicReference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        discoveryService.lookupServiceByNameAndContainerPort("httpd", 80, serviceDefinitionAsyncResult -> {
            resultAtomicReference.set(serviceDefinitionAsyncResult);
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        assertNotNull(resultAtomicReference.get());
        assertTrue(resultAtomicReference.get().succeeded());
        ServiceDefinition serviceDefinition = resultAtomicReference.get().result();

        assertFalse(serviceDefinition.getAddress().isEmpty());
    }

}
