package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.UriUtils;
import io.advantageous.reakt.promise.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Consul Service Discovery
 *
 * @author Geoff Chandler
 */
class ConsulDiscoveryService implements DiscoveryService {

    static final String SCHEME = "consul";

    private final Vertx vertx;
    private final int consulPort;
    private final String consulHost;

    ConsulDiscoveryService(final URI config) {
        if (config == null)
            throw new IllegalArgumentException("you must specify a configuration URI for the consul discovery service");
        this.vertx = Vertx.vertx();
        final URI consulConfig = URI.create(config.getSchemeSpecificPart());
        this.consulPort = consulConfig.getPort();
        this.consulHost = consulConfig.getHost();
    }

    @Override
    public Promise<List<URI>> lookupService(final URI query) {
        return invokablePromise(promise -> {
            if (query == null) {
                promise.reject("query was null");
                return;
            }
            if (!SCHEME.equals(query.getScheme())) {
                promise.reject(new IllegalArgumentException("query did not have the scheme " + SCHEME));
                return;
            }

            final Map<String, String> queryMap = UriUtils.splitQuery(query.getQuery());

            vertx.createHttpClient()
                    .request(HttpMethod.GET, consulPort, consulHost, "/v1/catalog/service" + query.getPath())
                    .exceptionHandler(promise::reject)
                    .handler(httpClientResponse -> httpClientResponse
                            .exceptionHandler(promise::reject)
                            .bodyHandler(buffer -> promise.accept(buffer.toJsonArray()
                                    .stream()
                                    .filter(o -> o instanceof JsonObject)
                                    .map(o -> (JsonObject) o)
                                    .filter(queryMap.containsKey("tag") ?
                                            (item) -> item.getJsonArray("ServiceTags").contains(queryMap.get("tag")) :
                                            (item) -> true)
                                    .map(item -> URI.create(RESULT_SCHEME + "://" +
                                            item.getString("Address") + ":" +
                                            item.getInteger("ServicePort") + "?tags=" +
                                            String.join(",", item.getJsonArray("ServiceTags")
                                                    .stream()
                                                    .map(Object::toString)
                                                    .collect(Collectors.toList()))
                                    ))
                                    .filter(o -> o != null)
                                    .collect(Collectors.toList())
                            )))
                    .end();
        });
    }
}
