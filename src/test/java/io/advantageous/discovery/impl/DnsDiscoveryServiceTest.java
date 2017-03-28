package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.advantageous.test.DockerHostUtils;
import io.advantageous.test.DockerTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Category(DockerTest.class)
public class DnsDiscoveryServiceTest {

    private static final URI[] TEST_CONFIGS;

    static {
        TEST_CONFIGS = new URI[]{
                URI.create("dns://ns-620.awsdns-13.net:53"),
                URI.create("dns://" + DockerHostUtils.getDockerHost() + ":8600")
        };
        System.out.println("Test configs: " + Arrays.toString(TEST_CONFIGS));
    }

    @Test
    public void testConstruct() throws Exception {
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        Assert.assertNotNull(service);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithWrongConfig() throws Exception {
        new DnsDiscoveryService(URI.create("bogus://foo"));
    }

    @Test(expected = RejectedPromiseException.class)
    public void testWithNullQuery() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService((URI) null).asHandler().invokeWithPromise(promise.asHandler());
        promise.asHandler().get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("bogus://bogus").asHandler().invokeWithPromise(promise.asHandler());
        promise.asHandler().get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongSubScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:bogus://bogus").asHandler().invokeWithPromise(promise.asHandler());
        promise.asHandler().get();
    }

    @Test
    public void testQueryA() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=100").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryANoPort() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net").asHandler().invokeWithPromise(promise);
        promise.asHandler().get();
    }

    @Test(expected = RejectedPromiseException.class)
    public void testQueryABadPort() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=bogus").asHandler().invokeWithPromise(promise);
        promise.asHandler().get();
    }

    @Test
    public void testQueryAThatDoesNotExist() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:A:///potato.redbull.com?port=100").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testQueryAWithBadPrimary() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(URI.create("dns://0.0.0.0:53"), TEST_CONFIGS[0]);
        service.lookupService("dns:A:///ipsec1.rbss.staging.rbmhops.net?port=100").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test
    public void testQuerySRV() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:SRV:///consul.service.consul").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
        Assert.assertEquals("/consul", result.get(0).getPath());
    }

    @Test
    public void testQuerySRVWithUnknownHost() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        DiscoveryService service = new DnsDiscoveryService(TEST_CONFIGS);
        service.lookupService("dns:SRV:///bogus.consul").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

}
