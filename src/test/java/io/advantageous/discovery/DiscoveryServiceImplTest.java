package io.advantageous.discovery;

import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

public class DiscoveryServiceImplTest {

    @Test
    public void testEmptyConstruction() throws Exception {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Assert.assertNotNull(discoveryService);
        Assert.assertEquals(1, discoveryService.getRegisteredServiceClasses().size());
    }


    @Test
    public void testDnsNoHost() throws Exception {
        DiscoveryService discoveryService = DiscoveryService.create();

        final List<URI> uriList = discoveryService.lookupService(URI.create("discovery:dns:A:///google.com?port=80"))
                .invokeAsBlockingPromise().get();

        Assert.assertTrue(uriList.size() > 0);
    }


    @Test
    public void testConstructionWithServiceLoader() throws Exception {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl(
                URI.create("dns://ns-620.awsdns-13.net:53"),
                URI.create("consul:http://192.168.99.100:8500")
        );
        Assert.assertNotNull(discoveryService);
        Assert.assertEquals(3, discoveryService.getRegisteredServiceClasses().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionWithServiceLoaderForUnknownScheme() throws Exception {
        new DiscoveryServiceImpl(URI.create("bogus://bogus"));
    }

    @Test
    public void testQuery() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        discoveryService.registerService("test", query -> invokablePromise(promise ->
                promise.resolve(Collections.singletonList(URI.create(DiscoveryService.RESULT_SCHEME + "://location/"))))
        );
        Promise<List<URI>> promise = Promises.blockingPromise();
        discoveryService.lookupService("discovery:test:///service").invokeWithPromise(promise);
        List<URI> results = promise.get();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals("location", results.get(0).getHost());
    }

    @Test
    public void testEcho() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        discoveryService.lookupService("discovery:echo:///service").invokeWithPromise(promise);
        List<URI> results = promise.get();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals("/service", results.get(0).getPath());
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryBadScheme() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        discoveryService.lookupService("bogus:///bogus").invokeWithPromise(promise);
        promise.get();
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryUnregisteredService() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        discoveryService.lookupService("discovery:bogus:///bogus").invokeWithPromise(promise);
        promise.get();
    }

}
