package com.redbullmediahouse.platform.discovery.impl;

import com.redbullmediahouse.platform.discovery.DiscoveryService;
import com.redbullmediahouse.platform.discovery.ServiceDefinition;
import com.typesafe.config.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.singletonList;

/**
 * DNS service discovery that uses A records.
 *
 * @author Rick Hightower
 */
public class DnsDiscoveryServiceUsingARecords implements DiscoveryService {

    private final Vertx vertx;
    private final String suffix;
    private final List<String> dnsHosts;
    private final int dnsPort;
    private final Map<String, Integer> nameToPort;
    private final Random random = new Random(System.currentTimeMillis());

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DnsDiscoveryServiceUsingARecords(final Vertx vertx,
                                            final String suffix,
                                            final String dnsHost,
                                            final int dnsPort,
                                            final Map<String, Integer> nameToPort) {
        this.vertx = vertx;
        this.suffix = suffix;
        this.dnsHosts = singletonList(dnsHost);
        this.dnsPort = dnsPort;
        this.nameToPort = nameToPort;

        if (logger.isDebugEnabled()) {
            logger.debug("DnsDiscoveryServiceUsingARecords {} {} {} {}", suffix, dnsHost, dnsPort, nameToPort);
        }
    }

    public DnsDiscoveryServiceUsingARecords(final Vertx vertx,
                                            final Config config) {
        this.vertx = vertx;
        this.suffix = config.getString("suffix");
        this.dnsHosts = config.getStringList("hosts");
        this.dnsPort = config.getInt("port");
        this.nameToPort = new HashMap<>();

        final Config nameToPortConfig = config.getConfig("name-to-port");

        nameToPortConfig.entrySet().forEach(entry -> nameToPort.put(entry.getKey(),
                nameToPortConfig.getInt(entry.getKey())));

        if (logger.isDebugEnabled()) {
            logger.debug("DnsDiscoveryServiceUsingARecords using config {} {} {} {}", suffix, dnsHosts, dnsPort, nameToPort);
        }
    }

    private Optional<String> getNextHost(final Iterator<String> iterator) {
        return !iterator.hasNext() ? Optional.empty() : Optional.of(iterator.next());
    }

    public void doLookupServiceByName(final String name,
                                      final Iterator<String> hosts,
                                      final Handler<AsyncResult<ServiceDefinition>> result,
                                      final Function<List<String>, ServiceDefinition> found) {

        if (logger.isDebugEnabled()) {
            logger.debug("doLookupServiceByName using config {} {} {} {}", name, dnsHosts, dnsPort, nameToPort);
        }

        getNextHost(hosts).ifPresent(dnsHost1 ->
                vertx.createDnsClient(dnsPort, dnsHost1).resolveA(name + suffix, dnsResults -> {
                    if (dnsResults.succeeded()) {
                        ServiceDefinition serviceDefinition = found.apply(dnsResults.result());
                        result.handle(Future.succeededFuture(serviceDefinition));
                    } else {
                        if (!hosts.hasNext()) {
                            result.handle(Future.failedFuture(dnsResults.cause()));
                        } else {
                            doLookupServiceByName(name, hosts, result, found);
                        }
                    }
                })
        );
    }

    @Override
    public void lookupServiceByName(final String name,
                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        doLookupServiceByName(name, dnsHosts.iterator(), result, dnsResults ->
                createServiceDefinition(dnsResults, name));
    }

    protected ServiceDefinition createServiceDefinition(final List<String> dnsResults,
                                                        final String name) {

        final int index = Math.abs(random.nextInt() % (dnsResults.size()));
        final String ipAddress = dnsResults.get(index);

        return new ServiceDefinition(ipAddress, nameToPort.get(name));
    }

    protected ServiceDefinition createServiceDefinitionWithPort(final List<String> dnsResults,
                                                                final int port) {

        final int index = Math.abs(random.nextInt() % (dnsResults.size()));
        final String ipaddress = dnsResults.get(index);

        return new ServiceDefinition(ipaddress, port);
    }

    @Override
    public void lookupServiceByNameAndContainerPort(final String name,
                                                    final int port,
                                                    final Handler<AsyncResult<ServiceDefinition>> result) {

        doLookupServiceByName(name, dnsHosts.iterator(), result, dnsResults ->
                createServiceDefinitionWithPort(dnsResults, port));
    }
}
