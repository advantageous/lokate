package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;

public class ConsulDiscoveryServiceTest {

    private static final URI TEST_CONFIG = URI.create("consul:http://192.168.99.100:8500");

    @Test
    public void testConstruct() throws Exception {
        DiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        Assert.assertNotNull(service);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructNoConfig() throws Exception {
        new ConsulDiscoveryService(null);
    }

    @Test(expected = RejectedPromiseException.class)
    public void testWithNullQuery() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService((URI) null).invokeWithPromise(promise);
        promise.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryWithBadScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("bogus://localhost/httpd").invokeWithPromise(promise);
        promise.get();
    }

    @Test
    public void testQueryByName() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("consul:///consul").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }
}
