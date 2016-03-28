package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.ServiceDefinition;
import io.advantageous.qbit.reactive.Callback;
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
    public void lookupServiceByName(final Callback<ServiceDefinition> result,
                                    final String name) {


        logger.debug("Docker lookupServiceByName  {}", name);
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
    public void lookupServiceByNameAndContainerPort(final Callback<ServiceDefinition> result,
                                                    final String name,
                                                    final int containerPort) {


        logger.debug("Docker lookupServiceByNameAndContainerPort  {} {}", name, containerPort);
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

                            logger.debug("Docker FOUND {} {} {}", name, containerPort, port.getInteger("PublicPort"));
                            return new ServiceDefinition(dockerHost, port.getInteger("PublicPort"));
                        }
                    }

                    logger.error("Docker NOT FOUND {} {}", name, containerPort);
                    throw new IllegalStateException("Private port (container port) not found: " + containerPort);
                });
    }

    private void queryDocker(final Callback<ServiceDefinition> result,
                             final Predicate<JsonObject> filter,
                             final Function<Object, ServiceDefinition> transformResultToServiceDefinition) {


        final HttpClient httpClient = vertx.createHttpClient();
        final HttpClientRequest request = httpClient.request(HttpMethod.GET, dockerPort,
                dockerHost, "/containers/json");


        if (logger.isInfoEnabled()) {
            final String message = "curl -H \"Content-type: application/json\" -H \"Accept: application/json\" " +
                    "http://" + dockerHost + ":" + dockerPort + "/containers/json";
            logger.info("About to make REST call  \n{}\n", message);
        }

        request.exceptionHandler(throwable -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to query docker {} {}", dockerHost, dockerPort);
            }
            result.onError(throwable);
        });

        request.handler(httpClientResponse -> {

            if (httpClientResponse.statusCode() >= 200 && httpClientResponse.statusCode() < 300) {
                httpClientResponse.bodyHandler(buffer -> {
                    final JsonArray jsonArray = buffer.toJsonArray();
                    handleResponseBodyFromDocker(result, filter,
                            transformResultToServiceDefinition, jsonArray);
                });
            } else {
                result.onError(new IllegalStateException(String.format("Unable to find %d %s", httpClientResponse.statusCode(),
                        httpClientResponse.statusMessage())));
            }
        });

        request.end(); //Send the request


    }

    private void handleResponseBodyFromDocker(final Callback<ServiceDefinition> result,
                                              final Predicate<JsonObject> filter,
                                              final Function<Object, ServiceDefinition> transformResultToServiceDefinition,
                                              final JsonArray jsonArray) {
        final Optional<ServiceDefinition> serviceDefinition = jsonArray
                .stream()
                .filter(o -> o instanceof JsonObject)
                .map(o -> (JsonObject) o)
                .filter(filter::test)
                .map(transformResultToServiceDefinition::apply)
                .findAny();
        if (serviceDefinition.isPresent()) {
            result.returnThis(serviceDefinition.get());
        } else {
            result.onError(new IllegalStateException(String.format("Could not find service by predicate %s", filter)));
        }
    }

}
