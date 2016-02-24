package com.redbullmediahouse.platform.discovery.impl;

import io.vertx.core.Vertx;
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

    private final Map<String, Integer> nameToPorts = new HashMap<String, Integer>() {
        {
            this.put("rbss-marathon-master-dev", 17151);
        }
    };
    private Vertx vertx;
    private AmazonEc2DiscoveryService dnsDiscoveryService;

    @Before
    public void setupVerticle(final TestContext context) throws Exception {
        vertx = rule.vertx();
        final Map<String, Integer> nameToPort = new HashMap<>();

        nameToPort.put("rbss-marathon-master-dev", 8080);


        dnsDiscoveryService = new AmazonEc2DiscoveryService(vertx, "sound-select-key",
                nameToPort, false, "ec2.us-west-2.amazonaws.com");
    }

    @Test
    public void testIntegration(final TestContext context) throws Exception {

        final Async async = context.async();

        dnsDiscoveryService.lookupServiceByName("rbss-marathon-master-dev",
                serviceDefinitionAsyncResult -> {
                    context.assertEquals(8080, serviceDefinitionAsyncResult.result().getPort());
                    async.complete();
                });

    }


    @Test
    public void testNotFound(final TestContext context) throws Exception {

        final Async async = context.async();

        dnsDiscoveryService.lookupServiceByName("bullshit",
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.failed());
                    serviceDefinitionAsyncResult.cause().printStackTrace();
                    async.complete();
                });

    }

    @Test
    public void testIntegrationUsingPort(final TestContext context) throws Exception {

        final Async async = context.async();

        dnsDiscoveryService.lookupServiceByNameAndContainerPort("rbss-marathon-master-dev", 8090,
                serviceDefinitionAsyncResult -> {
                    context.assertEquals(8090, serviceDefinitionAsyncResult.result().getPort());
                    async.complete();
                });

    }
}