package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.ServiceDefinition;
import com.typesafe.config.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Uses Marathon REST API to find services by name.
 * The name is the Marathon appId.
 * If more than one container is running for that appId, it will randomly pick one.
 * <p>
 * If you ask for the service port, it will give you the corresponding public port.
 *
 * @author Rick Hightower
 */
public class MarathonDiscoveryService implements DiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Vertx vertx;
    private final int marathonPort;
    private final String marathonHost;
    private final String GET_TASKS;

    private final Random random = new Random(System.currentTimeMillis());

    public MarathonDiscoveryService(final Vertx vertx,
                                    final int marathonPort,
                                    final String marathonHost,
                                    final String version) {
        this.vertx = vertx;
        this.marathonPort = marathonPort;
        this.marathonHost = marathonHost;
        GET_TASKS = "/" + version + "/" + "tasks";
        if (logger.isInfoEnabled()) {
            logger.info("MarathonDiscoveryService {}, {}, {}, {}",
                    marathonHost,
                    marathonPort,
                    version,
                    GET_TASKS);
        }
    }

    public MarathonDiscoveryService(final Vertx vertx,
                                    final Config config) {
        this.vertx = vertx;
        this.marathonPort = config.getInt("port");
        this.marathonHost = config.getString("host");
        final String version = config.getString("version");
        GET_TASKS = "/" + version + "/" + "tasks";
        if (logger.isInfoEnabled()) {
            logger.info("MarathonDiscoveryService {}, {}, {}, {}",
                    marathonHost,
                    marathonPort,
                    version,
                    GET_TASKS);
        }
    }

    @Override
    public void lookupServiceByName(final String name,
                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        final HttpClientRequest request = createHttpClient().request(HttpMethod.GET, GET_TASKS,
                clientResponseFromAppCall -> {

                    if (clientResponseFromAppCall.statusCode() > 299) {
                        handleErrorCase(clientResponseFromAppCall, result, name);
                    } else {

                        clientResponseFromAppCall.bodyHandler(buffer ->
                                findServiceFromResponsesFirstPort(
                                        name, buffer.toJsonObject().getJsonArray("tasks"), result)
                        );
                    }
                });

        request.exceptionHandler(throwable -> result.handle(Future.failedFuture(throwable)));
        request.end();
    }


    @Override
    public void lookupServiceByNameAndContainerPort(final String name,
                                                    final int port,
                                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        final HttpClientRequest request = createHttpClient().request(HttpMethod.GET, GET_TASKS,
                clientResponseFromAppCall -> {

                    if (clientResponseFromAppCall.statusCode() > 299) {
                        handleErrorCase(clientResponseFromAppCall, result, name);
                    } else {
                        clientResponseFromAppCall.bodyHandler(buffer ->
                                findServiceFromResponsesWithPort(
                                        name, port, buffer.toJsonObject().getJsonArray("tasks"), result
                                )
                        );
                    }
                });

        request.exceptionHandler(throwable -> result.handle(Future.failedFuture(throwable)));
        request.end();
    }

    private void findServiceFromResponsesWithPort(final String name,
                                                  final int port,
                                                  final JsonArray responseFromTaskCall,
                                                  final Handler<AsyncResult<ServiceDefinition>> result) {

        final List<ServiceDefinition> serviceDefinitions = responseFromTaskCall
                .stream()
                .filter(o -> o instanceof JsonObject)
                .map(o -> (JsonObject) o)
                .filter(jsonObject -> isServiceFromTask(name, jsonObject))
                .map(jsonObject -> convertToServiceDefinitionWithPort(jsonObject, port))
                .collect(Collectors.toList());

        extractDefinition(name, result, serviceDefinitions);
    }

    private void extractDefinition(final String name,
                                   final Handler<AsyncResult<ServiceDefinition>> result,
                                   final List<ServiceDefinition> serviceDefinitions) {

        if (serviceDefinitions.size() == 0) {
            logger.error("Service was not found {}, there are 0 services by that name in Marathon.", name);
            result.handle(Future.failedFuture(new IllegalStateException("Service Not Found")));
        } else {
            final int index = Math.abs(random.nextInt() % (serviceDefinitions.size()));
            result.handle(Future.succeededFuture(serviceDefinitions.get(index)));
        }
    }

    private void findServiceFromResponsesFirstPort(final String name,
                                                   final JsonArray responseFromTaskCall,
                                                   final Handler<AsyncResult<ServiceDefinition>> result) {

        if (logger.isInfoEnabled()) {
            logger.info("Finding service {}", name);
        }

        final List<JsonObject> marathonTasksAsJsonObjects = responseFromTaskCall.stream()
                .filter(o -> o instanceof JsonObject)
                .map(o -> (JsonObject) o)
                .collect(Collectors.toList());

        final List<ServiceDefinition> serviceDefinitions = marathonTasksAsJsonObjects.stream()
                .filter(jsonObject -> isServiceFromTask(name, jsonObject))
                .map(this::convertToServiceDefinitionFirstPort)
                .collect(Collectors.toList());

        extractDefinition(name, result, serviceDefinitions);
    }

    private ServiceDefinition convertToServiceDefinitionWithPort(final JsonObject jsonObject,
                                                                 final int port) {

        if (logger.isInfoEnabled()) {
            logger.info("Convert marathon task to Service Definition appId {}, port {}",
                    jsonObject.getString("appId"), port);
        }

        final List<Integer> publicPorts = jsonObject.getJsonArray("ports").stream().map(o -> (Integer) o)
                .collect(Collectors.toList());

        final List<Integer> privatePorts = jsonObject.getJsonArray("servicePorts").stream().map(o -> (Integer) o)
                .collect(Collectors.toList());

        int i = 0;
        for (; i < privatePorts.size(); i++) {
            if (privatePorts.get(i) == port) {
                break;
            }
        }

        return new ServiceDefinition(jsonObject.getString("host"), publicPorts.get(i));
    }

    private ServiceDefinition convertToServiceDefinitionFirstPort(final JsonObject jsonObject) {

        if (logger.isInfoEnabled()) {
            logger.info("Convert marathon task to Service Definition appId {}", jsonObject.getString("appId"));
        }

        final List<Integer> appPorts = jsonObject.getJsonArray("ports")
                .stream()
                .map(o -> (Integer) o)
                .collect(Collectors.toList());

        if (appPorts.size() > 0) {
            return new ServiceDefinition(jsonObject.getString("host"), appPorts.get(0));
        } else {
            logger.error("Unable to find port using default port 80");
            return new ServiceDefinition(jsonObject.getString("host"), 80);
        }
    }

    private boolean isServiceFromTask(final String name, final JsonObject jsonObject) {

        if (logger.isInfoEnabled()) {
            logger.info("Is Service from task {} appId {}", name, jsonObject.getString("appId"));
        }
        return jsonObject.getString("appId").equals("/" + name);
    }

    private void handleErrorCase(final HttpClientResponse httpClientResponse,
                                 final Handler<AsyncResult<ServiceDefinition>> result,
                                 final String name) {

        logger.error("Service not found {}, Marathon returned code {}, {}",
                httpClientResponse.statusCode(),
                httpClientResponse.statusMessage());
        result.handle(Future.failedFuture(
                String.format("Unable to get service %s from %s %d due to %s status code %d",
                        name, marathonHost, marathonPort, httpClientResponse.statusMessage(),
                        httpClientResponse.statusCode())
        ));
    }

    private HttpClient createHttpClient() {
        return vertx.createHttpClient(new HttpClientOptions()
                .setDefaultHost(marathonHost)
                .setDefaultPort(marathonPort)
                .setConnectTimeout(1_000).setVerifyHost(true));
    }

}
