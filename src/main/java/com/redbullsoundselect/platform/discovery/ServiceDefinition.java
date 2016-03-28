package com.redbullsoundselect.platform.discovery;


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


    public ServiceDefinition(final String address, final Integer port) {
        this.address = address;
        this.port = port != null ? port : -1;
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
