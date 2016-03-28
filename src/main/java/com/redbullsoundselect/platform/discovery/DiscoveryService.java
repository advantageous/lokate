package com.redbullsoundselect.platform.discovery;

import io.advantageous.qbit.reactive.Callback;

/**
 * Look up a service by name.
 *
 * @author Geoff Chandler.
 */
public interface DiscoveryService {


    void lookupServiceByName(Callback<ServiceDefinition> result, String name);

    default void lookupServiceByNameAndContainerPort(final Callback<ServiceDefinition> result,
                                                     final String name,
                                                     final int port) {

        result.onError(new UnsupportedOperationException("this service discovery type does not support a container port."));
    }

    default void close() {
    }

}
