package com.redbullmediahouse.platform.discovery.impl;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;


@RunWith(VertxUnitRunner.class)
public class AmazonEc2DiscoveryServiceTest {

    @Rule
    public final RunTestOnContext rule = new RunTestOnContext();

    private AmazonEc2DiscoveryService ec2DiscoveryService;

    @Before
    public void setupVerticle() throws Exception {
        final Map<String, Integer> nameToPort = new HashMap<>();
        nameToPort.put("rbss.staging.zookeeper1", 8080);
        ec2DiscoveryService =
                new AmazonEc2DiscoveryService(rule.vertx(), nameToPort, false, "ec2.us-west-2.amazonaws.com");
    }

    @Test
    public void testIntegration(final TestContext context) throws Exception {

        final Async async = context.async();

        ec2DiscoveryService.lookupServiceByName("rbss.staging.zookeeper1",
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertEquals(8080, serviceDefinitionAsyncResult.result().getPort());
                    async.complete();
                });

    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testNotFound(final TestContext context) throws Exception {

        final Async async = context.async();

        ec2DiscoveryService.lookupServiceByName("bullshit",
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.failed());
                    serviceDefinitionAsyncResult.cause().printStackTrace();
                    async.complete();
                }
        );
    }

    @Test
    public void testIntegrationUsingPort(final TestContext context) throws Exception {

        final Async async = context.async();

        ec2DiscoveryService.lookupServiceByNameAndContainerPort("rbss.staging.zookeeper1", 8080,
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertEquals(8080, serviceDefinitionAsyncResult.result().getPort());
                    async.complete();
                }
        );
    }
}