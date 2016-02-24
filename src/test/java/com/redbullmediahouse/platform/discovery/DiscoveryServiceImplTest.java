package com.redbullmediahouse.platform.discovery;

import com.redbullmediahouse.platform.UnitTests;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Category(UnitTests.class)
public class DiscoveryServiceImplTest {

    DiscoveryService discoveryService;

    @Before
    public void before() {

        discoveryService = new DiscoveryServiceImpl(new DiscoveryService() {
            @Override
            public void lookupServiceByName(String name, Handler<AsyncResult<ServiceDefinition>> result) {
                if (name.equals("servicea")) {
                    result.handle(Future.succeededFuture(new ServiceDefinition("", 1)));
                } else {
                    result.handle(Future.failedFuture(new Exception("failed")));
                }
            }

            @Override
            public void lookupServiceByNameAndContainerPort(String name, int port,
                                                            Handler<AsyncResult<ServiceDefinition>> result) {

            }

        }, new DiscoveryService() {
            @Override
            public void lookupServiceByName(String name, Handler<AsyncResult<ServiceDefinition>> result) {
                if (name.equals("serviceb")) {
                    result.handle(Future.succeededFuture(new ServiceDefinition("", 1)));
                } else {
                    result.handle(Future.failedFuture(new Exception("failed")));
                }
            }

            @Override
            public void lookupServiceByNameAndContainerPort(String name, int port,
                                                            Handler<AsyncResult<ServiceDefinition>> result) {

            }

        }, new DiscoveryService() {
            @Override
            public void lookupServiceByName(String name, Handler<AsyncResult<ServiceDefinition>> result) {
                if (name.equals("servicec")) {
                    result.handle(Future.succeededFuture(new ServiceDefinition("", 1)));
                } else {
                    result.handle(Future.failedFuture(new Exception("failed")));
                }
            }

            @Override
            public void lookupServiceByNameAndContainerPort(String name, int port,
                                                            Handler<AsyncResult<ServiceDefinition>> result) {

            }

        });
    }


    @Test
    public void testFound() {
        final AtomicReference<AsyncResult<ServiceDefinition>> result = new AtomicReference<>();

        discoveryService.lookupServiceByName("servicea", result::set);

        assertNotNull(result.get());
        assertTrue(result.get().succeeded());
        assertNotNull(result.get().result());

    }


    @Test
    public void testFoundInSecondProvider() {
        final AtomicReference<AsyncResult<ServiceDefinition>> result = new AtomicReference<>();

        discoveryService.lookupServiceByName("serviceb", result::set);

        assertNotNull(result.get());
        assertTrue(result.get().succeeded());
        assertNotNull(result.get().result());

    }


    @Test
    public void testFoundInThirdProvider() {
        final AtomicReference<AsyncResult<ServiceDefinition>> result = new AtomicReference<>();

        discoveryService.lookupServiceByName("servicec", result::set);

        assertNotNull(result.get());
        assertTrue(result.get().succeeded());
        assertNotNull(result.get().result());

    }


    @Test
    public void testNotFound() {
        final AtomicReference<AsyncResult<ServiceDefinition>> result = new AtomicReference<>();

        discoveryService.lookupServiceByName("notfound", result::set);

        assertNotNull(result.get());

        assertTrue(result.get().failed());


        assertNotNull(result.get().cause());

    }


}