package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
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
import static org.junit.Assert.*;

public class DiscoveryServiceImplTest {

    @Test
    public void testEmptyConstruction() throws Exception {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        assertNotNull(discoveryService);
        assertEquals(1, discoveryService.getRegisteredServiceClasses().size());
    }


    @Test
    public void testDnsNoHost() throws Exception {
        DiscoveryService discoveryService = DiscoveryService.create();

        final List<URI> uriList = discoveryService.lookupService(URI.create("discovery:dns:A:///google.com?port=80"))
                .invokeAsBlockingPromise().get();

        assertTrue(uriList.size() > 0);
    }


    @Test
    public void testConstructionWithServiceLoader() throws Exception {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl(
                URI.create("dns://ns-620.awsdns-13.net:53"),
                URI.create("consul:http://192.168.99.100:8500")
        );
        assertNotNull(discoveryService);
        assertEquals(3, discoveryService.getRegisteredServiceClasses().size());
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
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("location", results.get(0).getHost());
    }

    @Test
    public void testEcho() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        discoveryService.lookupService("discovery:echo:http://foo.com:9090").invokeWithPromise(promise);
        List<URI> results = promise.get();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("foo.com", results.get(0).getHost());
        assertEquals(9090, results.get(0).getPort());
    }

    @Test
    public void testEchos() {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        discoveryService.lookupService("discovery:echo:http://foo.com:9090,http://bar.com:9091").invokeWithPromise(promise);
        List<URI> results = promise.get();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("foo.com", results.get(0).getHost());
        assertEquals(9090, results.get(0).getPort());
        assertEquals("bar.com", results.get(1).getHost());
        assertEquals(9091, results.get(1).getPort());
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
