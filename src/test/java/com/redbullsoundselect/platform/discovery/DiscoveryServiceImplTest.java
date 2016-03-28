package com.redbullsoundselect.platform.discovery;

import com.redbullsoundselect.platform.UnitTests;
import io.advantageous.qbit.reactive.Callback;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;


@Category(UnitTests.class)
public class DiscoveryServiceImplTest {

    DiscoveryService discoveryService;

    @Before
    public void before() {

        discoveryService = new DiscoveryServiceImpl(new DiscoveryService() {
            @Override
            public void lookupServiceByName(Callback<ServiceDefinition> result, String name) {
                if (name.equals("servicea")) {
                    result.returnThis(new ServiceDefinition("", 1));
                } else {
                    result.onError(new Exception("failed"));
                }
            }

            @Override
            public void lookupServiceByNameAndContainerPort(Callback<ServiceDefinition> result, String name, int port) {

            }

        }, new DiscoveryService() {
            @Override
            public void lookupServiceByName(Callback<ServiceDefinition> result, String name) {
                if (name.equals("serviceb")) {
                    result.returnThis(new ServiceDefinition("", 1));
                } else {
                    result.onError(new Exception("failed"));
                }
            }

            @Override
            public void lookupServiceByNameAndContainerPort(Callback<ServiceDefinition> result, String name, int port) {

            }

        }, new DiscoveryService() {
            @Override
            public void lookupServiceByName(Callback<ServiceDefinition> result, String name) {
                if (name.equals("servicec")) {
                    result.returnThis(new ServiceDefinition("", 1));
                } else {
                    result.onError(new Exception("failed"));
                }
            }


            @Override
            public void lookupServiceByNameAndContainerPort(Callback<ServiceDefinition> result, String name, int port) {

            }
        });
    }


    @Test
    public void testFound() {
        final AtomicReference<ServiceDefinition> result = new AtomicReference<>();

        discoveryService.lookupServiceByName(result::set, "servicea");

        assertNotNull(result.get());
        assertNotNull(result.get());

    }


    @Test
    public void testFoundInSecondProvider() {
        final AtomicReference<ServiceDefinition> result = new AtomicReference<>();

        discoveryService.lookupServiceByName(result::set, "serviceb");

        assertNotNull(result.get());

    }


    @Test
    public void testFoundInThirdProvider() {
        final AtomicReference<ServiceDefinition> result = new AtomicReference<>();

        discoveryService.lookupServiceByName(result::set, "servicec");

        assertNotNull(result.get());

    }


    @Test
    public void testNotFound() {
        final AtomicReference<Throwable> result = new AtomicReference<>();

        discoveryService.lookupServiceByName(new Callback<ServiceDefinition>() {
            @Override
            public void accept(ServiceDefinition serviceDefinition) {

            }

            @Override
            public void onError(Throwable cause) {
                result.set(cause);
            }
        }, "notfound");

        assertNotNull(result.get());

    }


}