package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.UriUtils;
import io.advantageous.reakt.promise.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;

/**
 * Uses Docker REST API to find services by name.
 * The name is parsed from the Image name (and does not use the Docker name).
 * If more than one container is found, it will randomly pick one.
 * (We should give the option of using the Docker name).
 * <p>
 * If you ask for the container port, it will give you the corresponding public port.
 *
 * @author Geoff Chandler
 * @author Rick Hightower
 */
class DockerDiscoveryService implements DiscoveryService {

    static final String SCHEME = "docker";
    private static final String CONTAINER_PORT_QUERY_KEY = "containerPort";
    private final Vertx vertx;
    private final int defaultDockerPort;
    private final String defaultDockerHost;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    DockerDiscoveryService(final URI... configs) {
        if (configs.length > 1)
            throw new UnsupportedOperationException("the docker discovery service only supports one configuration.");
        if (configs.length == 0)
            throw new IllegalArgumentException("you must specify a configuration URI for the docker discovery service");
        final URI config = URI.create(configs[0].getSchemeSpecificPart());
        this.vertx = Vertx.vertx();
        this.defaultDockerPort = config.getPort();
        this.defaultDockerHost = config.getHost();
        this.logger.debug("DockerDiscoveryService  {} {}", defaultDockerHost, defaultDockerPort);
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
            if (queryMap.containsKey(CONTAINER_PORT_QUERY_KEY)) {
                lookupServiceByNameAndContainerPort(
                        query.getHost() != null ? query.getHost() : defaultDockerHost,
                        query.getPort() != -1 ? query.getPort() : defaultDockerPort,
                        query.getPath(),
                        Integer.parseInt(queryMap.get(CONTAINER_PORT_QUERY_KEY)),
                        promise);
            } else {
                lookupServiceByName(
                        query.getHost() != null ? query.getHost() : defaultDockerHost,
                        query.getPort() != -1 ? query.getPort() : defaultDockerPort,
                        query.getPath(),
                        promise);
            }
        });
    }

    private void lookupServiceByName(final String dockerHost,
                                     final int dockerPort,
                                     final String name,
                                     final Promise<List<URI>> promise) {

        logger.debug("Docker lookupServiceByName  {}", name);
        this.queryDocker(
                dockerHost,
                dockerPort,
                promise,
                json -> (json.getJsonArray("Names").getString(0)).equals(name),
                json -> {
                    final JsonObject jsonObject = (JsonObject) json;
                    final JsonArray ports = jsonObject.getJsonArray("Ports");
                    final JsonObject portInfo = ports.getJsonObject(0);
                    final Integer publicPort = portInfo.getInteger("PublicPort");

                    return URI.create(RESULT_SCHEME + "://" + dockerHost + ":" + publicPort);
                });
    }

    private void lookupServiceByNameAndContainerPort(final String dockerHost,
                                                     final int dockerPort,
                                                     final String name,
                                                     final int containerPort,
                                                     final Promise<List<URI>> promise) {

        logger.debug("Docker lookupServiceByNameAndContainerPort {} {}", name, containerPort);
        this.queryDocker(
                dockerHost,
                dockerPort,
                promise,
                json -> (json.getJsonArray("Names").getString(0)).equals(name),
                json -> {
                    final JsonObject jsonObject = (JsonObject) json;
                    final JsonArray ports = jsonObject.getJsonArray("Ports");
                    for (int i = 0; i < ports.size(); i++) {
                        final JsonObject port = ports.getJsonObject(i);
                        final int foundContainerPort = port.getInteger("PrivatePort");
                        if (containerPort == foundContainerPort) {
                            logger.debug("Docker FOUND {} {} {}", name, containerPort, port.getInteger("PublicPort"));
                            return URI.create(RESULT_SCHEME + "://" + dockerHost + ":" + port.getInteger("PublicPort"));
                        }
                    }

                    logger.error("Docker NOT FOUND {} {}", name, containerPort);
                    return null;
                });
    }

    private void queryDocker(final String dockerHost,
                             final int dockerPort,
                             final Promise<List<URI>> promise,
                             final Predicate<JsonObject> filter,
                             final Function<Object, URI> transformResultToServiceDefinition) {

        vertx.createHttpClient()
                .request(HttpMethod.GET, dockerPort, dockerHost, "/containers/json")
                .exceptionHandler(promise::reject)
                .handler(httpClientResponse -> {

                    httpClientResponse.exceptionHandler(promise::reject);

                    if (httpClientResponse.statusCode() != 200) {
                        promise.reject(new IllegalStateException(String.format("docker query returned status %d %s",
                                httpClientResponse.statusCode(), httpClientResponse.statusMessage())));
                        return;
                    }

                    httpClientResponse.bodyHandler(buffer -> promise.accept(buffer.toJsonArray()
                            .stream()
                            .filter(o -> o instanceof JsonObject)
                            .map(o -> (JsonObject) o)
                            .filter(filter)
                            .map(transformResultToServiceDefinition)
                            .filter(o -> o != null)
                            .collect(Collectors.toList())
                    ));
                })
                .end(); //Send the request
    }

}
