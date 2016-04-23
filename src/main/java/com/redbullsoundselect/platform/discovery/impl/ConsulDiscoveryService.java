package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.UriUtils;
import io.advantageous.reakt.promise.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    ConsulDiscoveryService(final URI config) {
        this.vertx = Vertx.vertx();
        this.consulPort = config.getPort();
        this.consulHost = config.getHost();
        this.logger.debug("ConsulDiscoveryService  {} {}", consulHost, consulPort);
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

            final Predicate<JsonObject> filter;
            final Map<String, String> queryMap = UriUtils.splitQuery(query.getQuery());
            if (queryMap.containsKey("tag")) {
                filter = (item) -> item.getJsonArray("ServiceTags").contains(queryMap.get("tag"));
            } else {
                filter = (item) -> true;
            }
            vertx.createHttpClient()
                    .request(HttpMethod.GET, consulPort, consulHost, "/v1/catalog/service" + query.getPath())
                    .exceptionHandler(promise::reject)
                    .handler(httpClientResponse -> {

                        httpClientResponse.exceptionHandler(promise::reject);

                        if (httpClientResponse.statusCode() != 200) {
                            promise.reject(new IllegalStateException(String.format("consul query returned status %d %s",
                                    httpClientResponse.statusCode(), httpClientResponse.statusMessage())));
                            return;
                        }

                        httpClientResponse.bodyHandler(buffer -> promise.accept(buffer.toJsonArray()
                                .stream()
                                .filter(o -> o instanceof JsonObject)
                                .map(o -> (JsonObject) o)
                                .filter(filter)
                                .map(item -> URI.create(RESULT_SCHEME + "://" + item.getString("ServiceAddress") + ":" +
                                        item.getString("ServicePort") + "?tags=" +
                                        item.getJsonArray("ServiceTags").toString()))
                                .filter(o -> o != null)
                                .collect(Collectors.toList())
                        ));
                    })
                    .end();
        });
    }
}
