package com.redbullsoundselect.platform.discovery;

import com.redbullmediahouse.platform.config.ConfigUtils;
import com.redbullsoundselect.platform.discovery.impl.AmazonEc2DiscoveryService;
import com.redbullsoundselect.platform.discovery.impl.DnsDiscoveryServiceUsingARecords;
import com.redbullsoundselect.platform.discovery.impl.DockerDiscoveryService;
import com.redbullsoundselect.platform.discovery.impl.MarathonDiscoveryService;
import com.typesafe.config.Config;
import io.advantageous.qbit.reactive.Callback;
import io.vertx.core.Vertx;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    static DiscoveryService createDiscoveryService(final Vertx vertx) {
        final Config discoveryConfig = ConfigUtils.getConfig("com.redbullsoundselect.platform.discovery");
        final List<String> providers = discoveryConfig.getStringList("providers");
        final Map<String, Function<Config, DiscoveryService>> discoveryServiceProviders =
                new HashMap<String, Function<Config, DiscoveryService>>() {
                    {
                        /* Docker. */
                        this.put("docker", cfg ->
                                new DockerDiscoveryService(vertx,
                                        URI.create(cfg.getString("host")).getHost(),
                                        cfg.getInt("port")));
                        /* DNS */
                        this.put("dns-basic", cfg -> new DnsDiscoveryServiceUsingARecords(vertx, cfg));
                        /* Marathon */
                        this.put("marathon", cfg -> new MarathonDiscoveryService(vertx, cfg));
                        /* Amazon */
                        this.put("amazon-ec2", cfg -> new AmazonEc2DiscoveryService(vertx, cfg));
                    }
                };

        final List<DiscoveryService> services = providers.stream().map(providerName ->
                discoveryServiceProviders.get(providerName)
                        .apply(discoveryConfig.getConfig("provider-config." + providerName)))
                .collect(Collectors.toList());

        return new DiscoveryServiceImpl(services.toArray(new DiscoveryService[services.size()]));
    }

}
