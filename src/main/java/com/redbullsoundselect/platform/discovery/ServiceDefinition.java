package com.redbullsoundselect.platform.discovery;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.TreeMap;

@DataObject
public class ServiceDefinition {

    private final String address;

    /**
     * The port for the service.  If there is no public port for this service, this is set to -1.
     */
    private final int port;

    public ServiceDefinition() {
        this.address = null;
        this.port = -1;
    }

    public ServiceDefinition(final ServiceDefinition serviceDefinition) {
        this(serviceDefinition.getAddress(), serviceDefinition.getPort());
    }

    public ServiceDefinition(final JsonObject jsonObject) {
        this(jsonObject.getString("address"), jsonObject.getInteger("port"));
    }

    public ServiceDefinition(final String address, final Integer port) {
        this.address = address;
        this.port = port != null ? port : -1;
    }

    public JsonObject toJson() {
        return new JsonObject(new TreeMap<String, Object>() {
            {
                put("address", address);
                put("port", port);
            }
        });
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceDefinition that = (ServiceDefinition) o;

        if (port == that.port) if (address != null ? address.equals(that.address) : that.address == null) return true;
        return false;

    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
