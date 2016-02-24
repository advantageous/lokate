package com.redbullmediahouse.platform.discovery;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Look up a service by name.
 *
 * @author Geoff Chandler.
 */
@ProxyGen
@VertxGen
public interface DiscoveryService {

    void lookupServiceByName(String name, Handler<AsyncResult<ServiceDefinition>> result);

    default void lookupServiceByNameAndContainerPort(final String name,
                                                     final int port,
                                                     final Handler<AsyncResult<ServiceDefinition>> result) {

        throw new UnsupportedOperationException("this service discovery type does not support a container port.");
    }

    @ProxyClose
    default void close() {
    }

}
