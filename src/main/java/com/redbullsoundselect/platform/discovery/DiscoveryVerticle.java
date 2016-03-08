package com.redbullsoundselect.platform.discovery;

import com.redbullmediahouse.platform.config.ConfigUtils;
import com.redbullmediahouse.platform.config.ZooKeeperConfig;
import com.redbullsoundselect.platform.discovery.impl.AmazonEc2DiscoveryService;
import com.redbullsoundselect.platform.discovery.impl.DnsDiscoveryServiceUsingARecords;
import com.redbullsoundselect.platform.discovery.impl.DockerDiscoveryService;
import com.redbullsoundselect.platform.discovery.impl.MarathonDiscoveryService;
import com.typesafe.config.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.spi.cluster.impl.zookeeper.ZookeeperClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryVerticle.class);

    private final Map<String, Function<Config, DiscoveryService>> discoveryServiceProviders;

    public DiscoveryVerticle(final Map<String, Function<Config, DiscoveryService>> discoveryServiceProviders) {
        this.discoveryServiceProviders = discoveryServiceProviders;
    }

    public DiscoveryVerticle(final Vertx vertx) {
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

    public static void main(final String[] args) {
        final VertxOptions vertxOptions = readVertxOptions(CONFIG_NAMESPACE);

        if (vertxOptions.isClustered()) {
            final Config config = getConfig(CONFIG_NAMESPACE);
            final ZooKeeperConfig zooKeeperConfig = zookeeperConfig(config.getConfig("zookeeper"));
            final Properties zkProperties = zooKeeperConfig.toVerrxProperties();
            vertxOptions.setClusterManager(new ZookeeperClusterManager(zkProperties));


            Vertx.clusteredVertx(vertxOptions, vertxAsyncResult -> {

                if (vertxAsyncResult.succeeded()) {

                    LOGGER.info("Clustering is working starting discovery verticle");
                    vertxAsyncResult.result().deployVerticle(new DiscoveryVerticle(vertxAsyncResult.result()));
                    addAdminSupport(vertxAsyncResult.result(), "/health/", 9090);
                } else {
                    LOGGER.error("Clustering is not working", vertxAsyncResult.cause());
                }
            });
        } else {

            final Vertx vertx = Vertx.vertx(vertxOptions);
            vertx.deployVerticle(new DiscoveryVerticle(vertx));
        }

    }

    /**
     * Set up the admin support.
     * Defined here so it can be tested independenty of AbstractVerticle, and also so AbstractVerticle
     * does not become a God class.
     *
     * @param vertx     vertx
     * @param healthURI healthURI
     * @param port      port
     */
    public static void addAdminSupport(final Vertx vertx,
                                       final String healthURI,
                                       final int port) {
        final Router router = Router.router(vertx);
        setupMetrics(vertx, router);
        setupHealth(healthURI, router);
        setupAdminEndpoint(vertx, healthURI, port, router);
    }


    private static void setupHealth(String healthURI, Router router) {
        router.route(healthURI).handler(context -> {

//            DiscoveryService discoveryService = ProxyHelper.createProxy(DiscoveryService.class, vertx, SERVICE_ADDRESS);
//
//            discoveryService.checkHealth(result -> {
//
//                if (result.succeeded() && result.result()) {
//                    context.response().setStatusCode(200).end("\"ok\"");
//                } else {
//                    context.response().setStatusCode(500).end("\"bad health\"");
//                }
//            });


            context.response().setStatusCode(200).end("\"ok\"");


        });
    }

    private static void setupMetrics(Vertx vertx, Router router) {
        /*
        * Setup metrics if enabled.
        */
        if (vertx.isMetricsEnabled()) {
            MetricsService metricsService = MetricsService.create(vertx);
            router.route("/metrics/")
                    .handler(context -> {
                        LOGGER.debug("In metrics handler: " + metricsService);
                        String metrics = metricsService.getMetricsSnapshot(vertx).encodePrettily();
                        LOGGER.debug(metrics);
                        context.response().setStatusCode(HttpURLConnection.HTTP_OK).end(metrics);
                    });
            router.route("/metrics/eventbus/")
                    .handler(context -> {
                        String metrics = metricsService.getMetricsSnapshot(vertx.eventBus()).encodePrettily();
                        context.response().setStatusCode(HttpURLConnection.HTTP_OK).end(metrics);
                    });
        } else {
            LOGGER.warn("Metrics are not enabled");
        }
    }

    private static void setupAdminEndpoint(Vertx vertx, String healthURI, int port, Router router) {
        final HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port));

        httpServer.requestHandler(router::accept);

        httpServer.listen(event -> {
            if (event.failed()) {
                LOGGER.error("Unable to startup health endpoint {} {}", port, healthURI);
            } else {
                LOGGER.info("Health endpoint running {} {}", port, healthURI);
            }
        });
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
