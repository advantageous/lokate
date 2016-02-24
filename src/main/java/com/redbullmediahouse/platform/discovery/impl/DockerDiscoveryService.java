package com.redbullmediahouse.platform.discovery.impl;

import com.redbullmediahouse.platform.discovery.DiscoveryService;
import com.redbullmediahouse.platform.discovery.ServiceDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

/**
 * Uses Docker REST API to find services by name.
 * The name is parsed from the Image name (and does not use the Docker name).
 * If more than one container is found, it will randomly pick one.
 * (We should give the option of using the Docker name).
 * <p>
 * If you ask for the container port, it will give you the corresponding public port.
 *
 * @author Rick Hightower
 */
public class DockerDiscoveryService implements DiscoveryService {

    private final Vertx vertx;
    private final int dockerPort;
    private final String dockerHost;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DockerDiscoveryService(final Vertx vertx, final String dockerHost, final int dockerPort) {
        this.vertx = vertx;
        this.dockerPort = dockerPort;
        this.dockerHost = dockerHost;
        logger.debug("DockerDiscoveryService  {} {}", dockerHost, dockerPort);
    }

    @Override
    public void lookupServiceByName(final String name,
                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        this.queryDocker(result,
                json -> Optional.of(json)
                        .map(o -> o.getString("Image"))
                        .map(image -> image.split("/"))
                        .map(array -> array.length > 1 ? array[1] : array[0])
                        .map(image -> image.split(":"))
                        .map(array -> array[0])
                        .get().equals(name),
                json -> {
                    final JsonObject jsonObject = (JsonObject) json;
                    final JsonArray ports = jsonObject.getJsonArray("Ports");
                    final JsonObject portInfo = ports.getJsonObject(0);
                    final Integer publicPort = portInfo.getInteger("PublicPort");

                    return new ServiceDefinition(dockerHost, publicPort);
                });
    }

    @Override
    public void lookupServiceByNameAndContainerPort(final String name,
                                                    final int containerPort,
                                                    final Handler<AsyncResult<ServiceDefinition>> result) {
        this.queryDocker(result,
                json -> Optional.of(json)
                        .map(o -> o.getString("Image"))
                        .map(image -> image.split("/"))
                        .map(array -> array.length > 1 ? array[1] : array[0])
                        .map(image -> image.split(":"))
                        .map(array -> array[0])
                        .get().equals(name),
                json -> {
                    final JsonObject jsonObject = (JsonObject) json;
                    final JsonArray ports = jsonObject.getJsonArray("Ports");
                    for (int i = 0; i < ports.size(); i++) {
                        final JsonObject port = ports.getJsonObject(i);
                        final int foundContainerPort = port.getInteger("PrivatePort");
                        if (containerPort == foundContainerPort) {
                            return new ServiceDefinition(dockerHost, port.getInteger("PublicPort"));
                        }
                    }
                    throw new IllegalStateException("Private port (container port) not found: " + containerPort);
                });
    }

    private void queryDocker(final Handler<AsyncResult<ServiceDefinition>> result,
                             final Predicate<JsonObject> filter,
                             final Function<Object, ServiceDefinition> transformResultToServiceDefinition) {


        final HttpClient httpClient = vertx.createHttpClient();
        final HttpClientRequest request = httpClient.request(HttpMethod.GET, dockerPort,
                dockerHost, "/containers/json");

        request.exceptionHandler(throwable -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to query docker {} {}", dockerHost, dockerPort);
            }
            result.handle(Future.failedFuture(throwable));
        });

        request.handler(httpClientResponse -> {

            if (httpClientResponse.statusCode() >= 200 && httpClientResponse.statusCode() < 300) {
                httpClientResponse.bodyHandler(buffer -> {
                    final JsonArray jsonArray = buffer.toJsonArray();
                    handleResponseBodyFromDocker(result, filter, transformResultToServiceDefinition, jsonArray);
                });
            } else {
                result.handle(Future.failedFuture(String.format("Unable to find %d %s", httpClientResponse.statusCode(),
                        httpClientResponse.statusMessage())));
            }
        });

        request.end(); //Send the request


    }

    private void handleResponseBodyFromDocker(Handler<AsyncResult<ServiceDefinition>> result, Predicate<JsonObject> filter, Function<Object, ServiceDefinition> transformResultToServiceDefinition, JsonArray jsonArray) {
        final Optional<ServiceDefinition> serviceDefinition = jsonArray
                .stream()
                .filter(o -> o instanceof JsonObject)
                .map(o -> (JsonObject) o)
                .filter(filter::test)
                .map(transformResultToServiceDefinition::apply)
                .findAny();
        if (serviceDefinition.isPresent()) {
            result.handle(succeededFuture(serviceDefinition.get()));
        } else {
            result.handle(failedFuture(String.format("Could not find service by predicate %s", filter)));
        }
    }

}
