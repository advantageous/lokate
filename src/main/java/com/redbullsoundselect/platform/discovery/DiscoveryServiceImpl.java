package com.redbullsoundselect.platform.discovery;

import io.advantageous.qbit.reactive.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by gcc on 2/5/16.
 *
 * @author rick and geoff
 */
public class DiscoveryServiceImpl implements DiscoveryService {

    private final List<DiscoveryService> discoveryServices;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DiscoveryServiceImpl(final DiscoveryService... discoveryServices) {
        this.discoveryServices = Arrays.asList(discoveryServices);
        if (logger.isDebugEnabled()) {
            this.discoveryServices.forEach(discoveryService ->
                    logger.debug("Using Discovery Service {}", discoveryService.getClass().getName()));
        }
    }


    private Optional<DiscoveryService> getNextProvider(final Iterator<DiscoveryService> iterator) {
        return !iterator.hasNext() ? Optional.empty() : Optional.of(iterator.next());
    }

    private void queryProviderByName(final Callback<ServiceDefinition> result,
                                     final String name,
                                     final Iterator<DiscoveryService> iterator) {

        if (logger.isDebugEnabled()) {
            logger.debug("queryProviderByName name {} ", name);
        }

        try {
            queryDiscoveryProviders(result, name, iterator,
                    (handler, discoveryService) ->
                            discoveryService.lookupServiceByName(handler, name),
                    () -> queryProviderByName(result, name, iterator));
        } catch (Exception ex) {
            logger.error("Unable to query", ex);
        }
    }

    private void queryProviderByNameAndContainerPort(final Callback<ServiceDefinition> result,
                                                     final String name,
                                                     final int containerPort,
                                                     final Iterator<DiscoveryService> iterator) {

        if (logger.isDebugEnabled()) {
            logger.debug("queryProviderByNameAndContainerPort name {}, port {} ", name, containerPort);
        }


        try {
            queryDiscoveryProviders(result, name, iterator,
                    (handler, discoveryService) ->
                            discoveryService.lookupServiceByNameAndContainerPort(handler, name, containerPort),
                    () -> queryProviderByNameAndContainerPort(result, name, containerPort, iterator));
        } catch (Exception ex) {
            logger.error("Unable to query", ex);
        }
    }

    /** TODO */
    private void queryDiscoveryProviders(final Callback<ServiceDefinition> result,
                                         final String name,
                                         final Iterator<DiscoveryService> iterator,
                                         final LookupCall lookupCall,
                                         final Runnable repeat) {

        getNextProvider(iterator).ifPresent(discoveryProvider ->


                lookupCall.lookup(new Callback<ServiceDefinition>() {
                    @Override
                    public void accept(final ServiceDefinition serviceDefinition) {
                        result.returnThis(serviceDefinition);
                    }

                    @Override
                    public void onError(final Throwable cause) {

                        if (!iterator.hasNext()) {
                            logger.error("Unable to find service {} from any provider ", name);
                            logger.error("Unable to find service from any provider", cause);
                            result.onError(new IllegalStateException("Unable to find service from any provider for name "
                                    + name, cause));
                        }
                        logger.info("Unable to find service {} from provider {}",
                                name, discoveryProvider.getClass().getSimpleName());

                        repeat.run();
                    }

                    @Override
                    public void onTimeout() {
                        onError(new TimeoutException("Call to lookup " + name + " timed out"));
                    }
                }, discoveryProvider)
        );
    }


    @Override
    public void lookupServiceByName(final Callback<ServiceDefinition> result,
                                    final String name) {

        queryProviderByName(result, name, discoveryServices.iterator());
    }

    @Override
    public void lookupServiceByNameAndContainerPort(final Callback<ServiceDefinition> result,
                                                    final String name,
                                                    final int port) {

        queryProviderByNameAndContainerPort(result, name, port, discoveryServices.iterator());
    }

    private interface LookupCall {
        void lookup(Callback<ServiceDefinition> handler, DiscoveryService discoveryService);
    }
}
