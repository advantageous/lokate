package com.redbullmediahouse.platform.discovery;

import com.redbullmediahouse.platform.IntegrationTests;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.redbullmediahouse.platform.discovery.DiscoveryVerticle.SERVICE_ADDRESS;
import static io.vertx.serviceproxy.ProxyHelper.createProxy;

/**
 * Test the discovery verticle.
 */

@Category(IntegrationTests.class)
@RunWith(VertxUnitRunner.class)
public class DiscoveryVerticleTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();
    private Vertx vertx;

    @Before
    public void setupVerticle(final TestContext context) throws Exception {
        vertx = rule.vertx();

        final Async async = context.async();

        vertx.deployVerticle(new DiscoveryVerticle(vertx), event -> {
            context.assertTrue(event.succeeded());
            async.complete();
        });
    }

    @Test
    public void testServiceLookupHitDocker(final TestContext context) throws Exception {

        final Async async = context.async();

        final DiscoveryService discoveryService = createProxy(DiscoveryService.class, vertx, SERVICE_ADDRESS);
        discoveryService.lookupServiceByName("httpd", event -> {
            context.assertTrue(event.succeeded());
            context.assertNotNull(event.result());
            context.assertNotNull(event.result().getAddress());
            async.complete();
        });
    }

    @Test
    public void testServiceLookupHitDNS(final TestContext context) throws Exception {

        final Async async = context.async();

        final DiscoveryService discoveryService = createProxy(DiscoveryService.class, vertx, SERVICE_ADDRESS);
        discoveryService.lookupServiceByName("ipsec1", event -> {
            context.assertTrue(event.succeeded());
            context.assertNotNull(event.result());
            context.assertEquals(100, event.result().getPort());
            async.complete();
        });
    }

    @Test
    public void testServiceLookupMarathon(final TestContext context) throws Exception {

        final Async async = context.async();

        final DiscoveryService discoveryService = createProxy(DiscoveryService.class, vertx, SERVICE_ADDRESS);
        discoveryService.lookupServiceByName("chronos", event -> {
            context.assertTrue(event.succeeded());
            context.assertNotNull(event.result());
            context.assertNotNull(event.result().getPort());
            async.complete();
        });
    }


    @Test
    public void testAmazonEC2Lookup(final TestContext context) throws Exception {

        final Async async = context.async();

        final DiscoveryService discoveryService = createProxy(DiscoveryService.class, vertx, SERVICE_ADDRESS);
        discoveryService.lookupServiceByName("rbss.staging.zookeeper1", event -> {
            context.assertTrue(event.succeeded());
            context.assertNotNull(event.result());
            context.assertNotNull(event.result().getPort());
            async.complete();
        });
    }

}
