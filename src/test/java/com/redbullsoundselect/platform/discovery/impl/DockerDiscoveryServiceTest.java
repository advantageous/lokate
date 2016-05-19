package com.redbullsoundselect.platform.discovery.impl;

import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class DockerDiscoveryServiceTest {

    private static final URI TEST_CONFIG;

    static {
        final String dockerHostEnv = System.getenv("DOCKER_HOST");
        final String dockerHost;

        if (dockerHostEnv == null) {
            dockerHost = "Mac OS X".equals(System.getProperty("os.name")) ? "192.168.99.100" : "localhost";
        } else {
            dockerHost = URI.create(dockerHostEnv).getHost();
        }

        TEST_CONFIG = URI.create("docker:http://" + dockerHost + ":" + 2375);
        System.out.println("Test config: " + TEST_CONFIG);
    }

    @Test
    public void testConstruct() throws Exception {
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        Assert.assertNotNull(service);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructWithNoConfig() throws Exception {
        new DockerDiscoveryService(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithWrongConfig() throws Exception {
        new DockerDiscoveryService(URI.create("bogus://foo"));
    }

    @Test(expected = RejectedPromiseException.class)
    public void testWithNullQuery() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService((URI) null).invokeWithPromise(promise);
        promise.get();
    }

    @Test
    public void testQueryByName() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService("docker:///consul").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryWithBadScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService("bogus://localhost/consul").invokeWithPromise(promise);
        promise.get();
    }

    @Test
    public void testQueryByNameAndContainerPort() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService("docker:///consul?containerPort=8500").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test
    public void testQueryByNameAndContainerPortNoPublic() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService("docker:///consul?containerPort=8300&requirePublicPort=false").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test
    public void testQueryByNameAndContainerPortNotFound() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(5));
        DockerDiscoveryService service = new DockerDiscoveryService(TEST_CONFIG);
        service.lookupService("docker:///consul?containerPort=8080").invokeWithPromise(promise);
        Assert.assertEquals(Collections.emptyList(), promise.get());
    }

}
