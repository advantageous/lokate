package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;

public class DnsDiscoveryServiceTest {

    private static final String CONSUL_HOST;
    private static final URI[] TEST_CONFIGS;

    static {
        final String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null) {
            CONSUL_HOST = dockerHost.split(":")[0].substring(2);
        } else {
            CONSUL_HOST = "192.168.99.100";
        }
        TEST_CONFIGS = new URI[]{
                URI.create("dns://ns-620.awsdns-13.net:53"),
                URI.create("dns://" + CONSUL_HOST + ":8600")
        };
        System.out.println("Test configs: " + TEST_CONFIGS);
    }

    @Test
    public void testConstruct() throws Exception {
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        Assert.assertNotNull(service);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithNoConfig() throws Exception {
        new DnsDiscoveryService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithWrongConfig() throws Exception {
        new DnsDiscoveryService(URI.create("bogus://foo"));
    }

    @Test(expected = RejectedPromiseException.class)
    public void testWithNullQuery() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService((URI) null).invokeWithPromise(promise);
        promise.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("bogus://bogus").invokeWithPromise(promise);
        promise.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongSubScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:bogus://bogus").invokeWithPromise(promise);
        promise.get();
    }

    @Test
    public void testQueryA() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=100").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryANoPort() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net").invokeWithPromise(promise);
        promise.get();
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryABadPort() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=bogus").invokeWithPromise(promise);
        promise.get();
    }

    @Test
    public void testQueryAThatDoesNotExist() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///potato.redbull.com?port=100").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testQueryAWithBadPrimary() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(URI.create("dns://0.0.0.0:53"), TEST_CONFIGS[0]);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=100").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test
    public void testQuerySRV() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:SRV:///consul.service.consul").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
        Assert.assertEquals("/consul", result.get(0).getPath());
    }

    @Test
    public void testQuerySRVWithUnknownHost() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise();
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:SRV:///bogus.consul").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

}
