package com.redbullsoundselect.platform.discovery.impl;

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
    private MarathonDiscoveryService marathonDiscoveryService;

    @Before
    public void setUp() throws Exception {
        marathonDiscoveryService = new MarathonDiscoveryService(
                rule.vertx(),
                8080,
                "mesos-master1.rbss.staging.rbmhops.net",
                "v2"
        );
    }

    @Test
    public void testLookupServiceByName(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByName("chronos",
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertTrue(serviceDefinitionAsyncResult.result().getPort() > 10_000);
                    async.complete();
                });
    }

    @Test
    public void testLookupServiceByNameAndContainerPort(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByNameAndContainerPort("chronos", 8080,
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.succeeded());
                    context.assertNotNull(serviceDefinitionAsyncResult.result().getAddress());
                    context.assertNotNull(serviceDefinitionAsyncResult.result().getPort());
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