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

@RunWith(VertxUnitRunner.class)
public class MarathonDiscoveryServiceTest {


    @Rule
    public final RunTestOnContext rule = new RunTestOnContext();
    private final String host = "10.0.4.103";
    private final int port = 8080;
    private Vertx vertx;
    private MarathonDiscoveryService marathonDiscoveryService;

    @Before
    public void setUp() throws Exception {
        vertx = rule.vertx();
        marathonDiscoveryService = new MarathonDiscoveryService(vertx, port, host, "v2");
    }

    @Test
    public void testLookupServiceByName(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByName("hdfs",
                serviceDefinitionAsyncResult -> {

                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertTrue(serviceDefinitionAsyncResult.result().getPort() > 10_000);
                    async.complete();

                });
    }

    @Test
    public void testLookupServiceByNameAndContainerPort(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByNameAndContainerPort("hdfs", 10006,
                serviceDefinitionAsyncResult -> {

                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertEquals(17151, serviceDefinitionAsyncResult.result().getPort());
                    async.complete();

                });
    }


    @Test
    public void testLookupNotFound(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByNameAndContainerPort("crap", 9000,
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.failed());
                    async.complete();
                });
    }
}