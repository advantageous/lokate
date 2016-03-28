package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.ServiceDefinition;
import io.advantageous.qbit.reactive.Callback;
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

        marathonDiscoveryService.lookupServiceByName(
                serviceDefinitionAsyncResult -> {
                    context.assertTrue(serviceDefinitionAsyncResult.getPort() > 10_000);
                    async.complete();
                }, "chronos");
    }

    @Test
    public void testLookupServiceByNameAndContainerPort(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByNameAndContainerPort(serviceDefinitionAsyncResult -> {
            context.assertNotNull(serviceDefinitionAsyncResult.getAddress());
            context.assertNotNull(serviceDefinitionAsyncResult.getPort());
            async.complete();
        }, "chronos", 8080);
    }

    @Test
    public void testLookupNotFound(final TestContext context) throws Exception {

        final Async async = context.async();

        marathonDiscoveryService.lookupServiceByNameAndContainerPort(new Callback<ServiceDefinition>() {
            @Override
            public void accept(ServiceDefinition serviceDefinition) {
            }

            @Override
            public void onError(Throwable error) {
                async.complete();
            }
        }, "crap", 9000);

    }
}