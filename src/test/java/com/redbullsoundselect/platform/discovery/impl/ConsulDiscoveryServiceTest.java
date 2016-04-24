package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import io.advantageous.reakt.exception.RejectedPromiseException;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ConsulDiscoveryServiceTest {

    private static final String CONSUL_HOST = "192.168.99.100";

    private static final URI TEST_CONFIG = URI.create("consul:http://" + CONSUL_HOST + ":8500");

    private static void addTagToService(String serviceName, String tag) {
        Promise<Optional<JsonObject>> requestPromise = Promises.blockingPromise();
        Vertx.vertx().createHttpClient().get(8500, CONSUL_HOST, "/v1/catalog/service/" + serviceName)
                .exceptionHandler((e) -> Assert.fail())
                .handler(httpClientResponse -> httpClientResponse
                        .exceptionHandler((e) -> Assert.fail())
                        .bodyHandler(buffer -> requestPromise.accept(buffer.toJsonArray()
                                .stream()
                                .filter(o -> o instanceof JsonObject)
                                .map(o -> (JsonObject) o)
                                .findAny()
                        )))
                .end();
        JsonObject original = requestPromise.get().orElseGet(() -> {
            throw new RuntimeException();
        });

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
        Buffer buffer = Buffer.buffer(updated.toString());

        Promise<HttpClientResponse> updatePromise = Promises.blockingPromise();
        Vertx.vertx().createHttpClient().put(8500, CONSUL_HOST, "/v1/catalog/register")
                .exceptionHandler((e) -> Assert.fail())
                .handler(updatePromise::accept)
                .putHeader(HttpHeaders.CONTENT_LENGTH, buffer.length() + "")
                .write(buffer)
                .end();

        HttpClientResponse response = updatePromise.get();
        Assert.assertEquals(200, response.statusCode());
    }

    @Test
    public void testConstruct() throws Exception {
        DiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        Assert.assertNotNull(service);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithNoConfig() throws Exception {
        new ConsulDiscoveryService(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithWrongConfig() throws Exception {
        new ConsulDiscoveryService(URI.create("bogus://foo"));
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

    @Test
    public void testQueryByNameAndTag() throws Exception {
        addTagToService("consul", "foo");
        Promise<List<URI>> promise = Promises.blockingPromise();
        ConsulDiscoveryService service = new ConsulDiscoveryService(TEST_CONFIG);
        service.lookupService("consul:///consul?tag=foo").invokeWithPromise(promise);
        List<URI> result = promise.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertFalse(result.get(0).getHost().isEmpty());
        Assert.assertEquals("tags=foo", result.get(0).getQuery());
    }
}
