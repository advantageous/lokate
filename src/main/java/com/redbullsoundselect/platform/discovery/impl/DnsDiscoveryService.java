package com.redbullsoundselect.platform.discovery.impl;

import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.UriUtils;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.vertx.core.Vertx;
import io.vertx.core.dns.SrvRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.vertx.ReaktVertx.convertPromise;

/**
 * DNS service discovery that uses A records.
 *
 * @author Rick Hightower
 */
class DnsDiscoveryService implements DiscoveryService {

    static final String SCHEME = "dns";
    private static final String PORT_QUERY_KEY = "port";

    private static final String A_SCHEME = "A";
    private static final String SRV_SCHEME = "SRV";

    private final Vertx vertx;
    private final List<URI> dnsHosts;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    DnsDiscoveryService(final URI... configs) {
        if (configs.length == 0)
            throw new IllegalArgumentException("you must specify a configuration URI for the dns discovery service");
        this.vertx = Vertx.vertx();
        this.dnsHosts = Arrays.stream(configs).map(uri ->
                URI.create(uri.getSchemeSpecificPart())).collect(Collectors.toList());
    }

    @Override
    public Promise<List<URI>> lookupService(final URI query) {

        return invokablePromise(promise -> {

            if (query == null) {
                promise.reject("query was null");
                return;
            }
            if (!SCHEME.equals(query.getScheme())) {
                promise.reject(new IllegalArgumentException("query did not have the scheme " + SCHEME));
                return;
            }

            final URI dnsQuery = URI.create(query.getSchemeSpecificPart());

            switch (dnsQuery.getScheme()) {

                case A_SCHEME:
                    resolveA(dnsQuery, promise);
                    break;

                case SRV_SCHEME:
                    resolveSRV(0, dnsQuery.getPath().substring(1), promise);
                    break;

                default:
                    promise.reject(new IllegalArgumentException("dns queries must have a schema of A or SRV"));
            }
        });
    }

    private void resolveA(final URI query, final Promise<List<URI>> promise) {
        final String portString = UriUtils.splitQuery(query.getQuery()).get(PORT_QUERY_KEY);
        if (portString == null) {
            promise.reject("a port must be set in the query string for a A RECORD query.");
            return;
        }
        final int port;
        try {
            port = Integer.parseInt(portString);
        } catch (final NumberFormatException e) {
            promise.reject("the port in the query string must be an integer", e);
            return;
        }

        doResolveA(0, query.getPath().substring(1), port, promise);
    }

    private void doResolveA(final int hostIndex,
                            final String serviceName,
                            final int port,
                            final Promise<List<URI>> promise) {

        if (hostIndex >= dnsHosts.size()) {
            promise.resolve(Collections.emptyList());
            return;
        }

        final URI currentHost = dnsHosts.get(hostIndex);

        vertx.createDnsClient(currentHost.getPort(), currentHost.getHost()).resolveA(serviceName,
                convertPromise(Promises.<List<String>>promise()
                        .then(list -> promise.resolve(list.stream()
                                .map(ipAddress -> RESULT_SCHEME + "://" + ipAddress + ":" + port + "/")
                                .map(URI::create)
                                .collect(Collectors.toList())
                        ))
                        .catchError(error -> {
                            logger.warn("dns lookup failed: ", error);
                            doResolveA(hostIndex + 1, serviceName, port, promise);
                        })
                )
        );
    }

    private void resolveSRV(final int hostIndex, final String serviceName, final Promise<List<URI>> promise) {

        if (hostIndex >= dnsHosts.size()) {
            promise.resolve(Collections.emptyList());
            return;
        }

        final URI currentHost = dnsHosts.get(hostIndex);

        this.vertx.createDnsClient(currentHost.getPort(), currentHost.getHost()).resolveSRV(serviceName,
                convertPromise(Promises.<List<SrvRecord>>promise()
                        .then(list -> promise.resolve(list.stream()
                                .map(srv -> RESULT_SCHEME + "://" + srv.target() + ":" + srv.port() + "/" +
                                        srv.name() + "?priority=" + srv.priority() + "&weight=" + srv.weight())
                                .map(URI::create)
                                .collect(Collectors.toList())
                        ))
                        .catchError(error -> {
                            logger.warn("dns lookup failed: ", error);
                            resolveSRV(hostIndex + 1, serviceName, promise);
                        })
                )
        );
    }

}
