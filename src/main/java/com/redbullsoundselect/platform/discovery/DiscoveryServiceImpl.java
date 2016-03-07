package com.redbullsoundselect.platform.discovery;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Created by gcc on 2/5/16.
 *
 * @author rick and geoff
 */
public class DiscoveryServiceImpl implements DiscoveryService {

    private final List<DiscoveryService> discoveryServices;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DiscoveryServiceImpl(DiscoveryService... discoveryServices) {
        this.discoveryServices = Arrays.asList(discoveryServices);
    }

    private Optional<DiscoveryService> getNextProvider(final Iterator<DiscoveryService> iterator) {
        return !iterator.hasNext() ? Optional.empty() : Optional.of(iterator.next());
    }

    private void queryProviderByName(final String name,
                                     final Iterator<DiscoveryService> iterator,
                                     final Handler<AsyncResult<ServiceDefinition>> result) {

        queryDiscoveryProviders(name, iterator, result,
                (discoveryService, handler) ->
                        discoveryService.lookupServiceByName(name, handler),
                () -> queryProviderByName(name, iterator, result));
    }

    private void queryProviderByNameAndContainerPort(final String name,
                                                     final int containerPort,
                                                     final Iterator<DiscoveryService> iterator,
                                                     final Handler<AsyncResult<ServiceDefinition>> result) {

        queryDiscoveryProviders(name, iterator, result,
                (discoveryService, handler) ->
                        discoveryService.lookupServiceByNameAndContainerPort(name, containerPort, handler),
                () -> queryProviderByNameAndContainerPort(name, containerPort, iterator, result));
    }

    private void queryDiscoveryProviders(final String name,
                                         final Iterator<DiscoveryService> iterator,
                                         final Handler<AsyncResult<ServiceDefinition>> result,
                                         final LookupCall lookupCall,
                                         final Runnable repeat) {

        getNextProvider(iterator).ifPresent(discoveryProvider ->
                lookupCall.lookup(discoveryProvider, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        result.handle(asyncResult);
                    } else {
                        if (!iterator.hasNext()) {
                            logger.error("Unable to find service {} from any provider ", name);
                            logger.error("Unable to find service from any provider", asyncResult.cause());
                            result.handle(asyncResult);
                        }
                        logger.info("Unable to find service {} from provider {}",
                                name, discoveryProvider.getClass().getSimpleName());

                        repeat.run();
                    }
                })
        );
    }

    @Override
    public void lookupServiceByName(final String name, final Handler<AsyncResult<ServiceDefinition>> result) {

        queryProviderByName(name, discoveryServices.iterator(), result);
    }

    @Override
    public void lookupServiceByNameAndContainerPort(final String name,
                                                    final int port,
                                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        queryProviderByNameAndContainerPort(name, port, discoveryServices.iterator(), result);
    }

    private interface LookupCall {
        void lookup(DiscoveryService discoveryService, Handler<AsyncResult<ServiceDefinition>> handler);
    }
}
