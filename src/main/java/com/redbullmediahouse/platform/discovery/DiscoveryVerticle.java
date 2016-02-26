package com.redbullmediahouse.platform.discovery;

import com.redbullmediahouse.platform.config.ConfigUtils;
import com.redbullmediahouse.platform.config.ZooKeeperConfig;
import com.redbullmediahouse.platform.discovery.impl.AmazonEc2DiscoveryService;
import com.redbullmediahouse.platform.discovery.impl.DnsDiscoveryServiceUsingARecords;
import com.redbullmediahouse.platform.discovery.impl.DockerDiscoveryService;
import com.redbullmediahouse.platform.discovery.impl.MarathonDiscoveryService;
import com.typesafe.config.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.spi.cluster.impl.zookeeper.ZookeeperClusterManager;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.redbullmediahouse.platform.config.ConfigUtils.getConfig;
import static com.redbullmediahouse.platform.config.VertxFromConfig.readVertxOptions;


import static com.redbullmediahouse.platform.config.ZooKeeperConfig.zookeeperConfig;

/**
 * Verticle for service discovery.
 *
 * @author Geoff Chandler
 */
public class DiscoveryVerticle extends AbstractVerticle {

    public static final String SERVICE_ADDRESS = "v1/discovery-service";
    private static final String CONFIG_NAMESPACE = DiscoveryVerticle.class.getPackage().getName();

    private final Map<String, Function<Config, DiscoveryService>> discoveryServiceProviders;

    public DiscoveryVerticle(final Map<String, Function<Config, DiscoveryService>> discoveryServiceProviders) {
        this.discoveryServiceProviders = discoveryServiceProviders;
    }

    public DiscoveryVerticle(Vertx vertx) {
        this(new HashMap<String, Function<Config, DiscoveryService>>() {
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
        });
    }

    public static void main(String[] args) {
        final VertxOptions vertxOptions = readVertxOptions(CONFIG_NAMESPACE);

        if (vertxOptions.isClustered()) {
            final Config config = getConfig(CONFIG_NAMESPACE);
            final ZooKeeperConfig zooKeeperConfig = zookeeperConfig(config.getConfig("zookeeper"));
            final Properties zkProperties = zooKeeperConfig.toVerrxProperties();
            vertxOptions.setClusterManager(new ZookeeperClusterManager(zkProperties));
        }

        final Vertx vertx = Vertx.vertx(vertxOptions);
        vertx.deployVerticle(new DiscoveryVerticle(vertx));
    }

    @Override
    public void start(final Future<Void> startFuture) throws Exception {

        try {
            final Config config = ConfigUtils.getConfig(CONFIG_NAMESPACE);
            final List<String> providers = config.getStringList("providers");
            final List<DiscoveryService> services = providers.stream().map(providerName ->
                    discoveryServiceProviders.get(providerName)
                            .apply(config.getConfig("provider-config." + providerName)))
                    .collect(Collectors.toList());

            /* Get a service instance. */
            final DiscoveryService service =
                    new DiscoveryServiceImpl(services.toArray(new DiscoveryService[services.size()]));

            /* Register the proxy implementation. */
            ProxyHelper.registerService(DiscoveryService.class, vertx, service, SERVICE_ADDRESS);

            startFuture.complete();
        } catch (Exception e) {
            e.printStackTrace();
            startFuture.fail(e);
        }
    }

}
