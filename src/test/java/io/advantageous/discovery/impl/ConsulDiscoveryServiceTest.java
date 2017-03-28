package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.advantageous.test.DockerHostUtils;
import io.advantageous.test.DockerTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Category(DockerTest.class)
public class ConsulDiscoveryServiceTest {

    private static final String CONSUL_HOST = DockerHostUtils.getDockerHost();
    private static final int CONSUL_PORT = 8500;
    private static final URI TEST_CONFIG = URI.create("consul:http://" + CONSUL_HOST + ":" + CONSUL_PORT);
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulDiscoveryServiceTest.class);

    static {
        System.out.println("Test config: " + TEST_CONFIG);
    }

    private static void addTagToService(String serviceName, String tag) {
        Promise<Optional<JsonObject>> requestPromise = Promises.blockingPromise();
        Vertx.vertx().createHttpClient().get(CONSUL_PORT, CONSUL_HOST, "/v1/catalog/service/" + serviceName)
                .exceptionHandler(requestPromise.asHandler()::reject)
                .handler(httpClientResponse -> httpClientResponse
                        .exceptionHandler(requestPromise.asHandler()::reject)
                        .bodyHandler(buffer -> requestPromise.asHandler().accept(buffer.toJsonArray()
                                .stream()
                                .filter(o -> o instanceof JsonObject)
                                .map(o -> (JsonObject) o)
                                .findAny()
                        )))
                .end();
        JsonObject original = requestPromise.asHandler().get().orElseGet(() -> {
            throw new RuntimeException();
        });
        LOGGER.debug("original object: {}", original);

        JsonObject updated = new JsonObject();
        updated.put("Node", original.getString("Node"));
        updated.put("Address", original.getString("Address"));

        JsonObject serviceObject = new JsonObject();
        serviceObject.put("ID", original.getString("ServiceID"));
        serviceObject.put("Service", serviceName);
        serviceObject.put("Address", original.getString("ServiceAddress"));
        serviceObject.put("Port", original.getInteger("ServicePort"));
        serviceObject.put("Tags", new JsonArray(Collections.singletonList(tag)));

        updated.put("Service", serviceObject);
        LOGGER.debug("new object: {}", updated);
        Buffer buffer = Buffer.buffer(updated.toString());

        Promise<HttpClientResponse> updatePromise = Promises.blockingPromise();
        Vertx.vertx().createHttpClient().put(CONSUL_PORT, CONSUL_HOST, "/v1/catalog/register")
                .exceptionHandler((e) -> Assert.fail())
                .handler(updatePromise.asHandler()::accept)
                .putHeader(HttpHeaders.CONTENT_LENGTH, buffer.length() + "")
                .write(buffer)
                .end();

        HttpClientResponse response = updatePromise.asHandler().get();
        Assert.assertEquals(200, response.statusCode());
    }

    @Test
    public void testConstruct() throws Exception {
        DiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        Assert.assertNotNull(service);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructWithNoConfig() throws Exception {
        new ConsulDiscoveryService(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithWrongConfig() throws Exception {
        new ConsulDiscoveryService(URI.create("bogus://foo"));
    }

    @Test(expected = RejectedPromiseException.class)
    public void testWithNullQuery() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService((URI) null).asHandler().invokeWithPromise(promise);
        promise.asHandler().get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryWithBadScheme() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("bogus://localhost/httpd").asHandler().invokeWithPromise(promise);
        promise.asHandler().get();
    }

    @Test
    public void testQueryByName() throws Exception {
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("consul:///consul").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
    }

    @Test
    public void testQueryByNameAndTag() throws Exception {
        addTagToService("consul", "foo");
        Promise<List<URI>> promise = Promises.blockingPromise(Duration.ofSeconds(10));
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("consul:///consul?tag=foo").asHandler().invokeWithPromise(promise);
        List<URI> result = promise.asHandler().get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
        Assert.assertEquals("tags=foo", result.get(0).getQuery());
    }
}
