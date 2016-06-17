package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import io.advantageous.reakt.promise.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static java.lang.Boolean.parseBoolean;

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
    private static final String REQUIRE_PUBLIC_QUERY_KEY = "requirePublicPort";

    private final Vertx vertx;
    private final int defaultDockerPort;
    private final String defaultDockerHost;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    DockerDiscoveryService(final URI config) {
        Objects.requireNonNull(config, "you must specify a configuration URI for the docker discovery service");
        if (!SCHEME.equals(config.getScheme()))
            throw new IllegalArgumentException("scheme for docker service config must be " + SCHEME);
        final URI dockerConfig = URI.create(config.getSchemeSpecificPart());
        this.vertx = Vertx.vertx();
        this.defaultDockerPort = dockerConfig.getPort();
        this.defaultDockerHost = dockerConfig.getHost();
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

            final String dockerHost = query.getHost() != null ? query.getHost() : this.defaultDockerHost;
            final Map<String, String> queryMap = UriUtils.splitQuery(query.getQuery());
            final boolean requirePublicPort = parseBoolean(queryMap.getOrDefault(REQUIRE_PUBLIC_QUERY_KEY, "true"));
            final int findPort = Integer.parseInt(queryMap.getOrDefault(CONTAINER_PORT_QUERY_KEY, "-1"));

            this.vertx.createHttpClient()
                    .request(HttpMethod.GET, query.getPort() != -1 ? query.getPort() :
                            this.defaultDockerPort, dockerHost, "/containers/json")
                    .exceptionHandler(promise::reject)
                    .handler(httpClientResponse -> httpClientResponse
                            .exceptionHandler(promise::reject)
                            .bodyHandler(buffer -> promise.resolve(buffer.toJsonArray()
                                    .stream()
                                    .filter(o -> o instanceof JsonObject)
                                    .map(o -> (JsonObject) o)
                                    .filter(json -> (json.getJsonArray("Names").getString(0)).equals(query.getPath()))
                                    .map(json -> json.getJsonArray("Ports")
                                            .stream()
                                            .filter(o -> o instanceof JsonObject)
                                            .map(o -> (JsonObject) o)
                                            .filter(port -> !requirePublicPort || port.getInteger("PublicPort") != null)
                                            .filter(port -> findPort <= 0 || port.getInteger("PrivatePort") == findPort)
                                            .findAny().orElse(null))
                                    .filter(o -> o != null)
                                    .peek(foundPort -> this.logger.debug("found port: {}", foundPort))
                                    .map(foundPort -> Optional.ofNullable(foundPort.getInteger("PublicPort")))
                                    .map(optional -> URI.create(RESULT_SCHEME + "://" + dockerHost +
                                            (optional.isPresent() ? ":" + optional.get() : "") + "/"))
                                    .peek(uri -> this.logger.debug("found service in docker: {}", uri))
                                    .collect(Collectors.toList())
                            )))
                    .end(); //Send the request
        });
    }
}
