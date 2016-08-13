package io.advantageous.discovery.impl;

import io.advantageous.discovery.DiscoveryService;
import io.advantageous.reakt.promise.Promise;
import io.advantageous.reakt.promise.Promises;
import io.vertx.core.Vertx;
import io.vertx.core.dns.SrvRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.advantageous.reakt.promise.Promises.invokablePromise;
import static io.advantageous.reakt.vertx.ReaktVertx.convertPromise;

/**
 * DNS service discovery
 *
 * @author Geoff Chandler
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
        this.vertx = Vertx.vertx();
        this.dnsHosts = readDnsConf();
        this.dnsHosts.addAll(Arrays.stream(configs)
                .peek(uri -> {
                    if (!SCHEME.equals(uri.getScheme()))
                        throw new IllegalArgumentException("scheme for docker service config must be " + SCHEME);
                })
                .map(uri -> URI.create(uri.getSchemeSpecificPart())).collect(Collectors.toList())
        );
    }

    static List<URI> readDnsConf() {
        final File file = new File("/etc/resolv.conf");
        if (file.exists()) {
            try {
                return Files.lines(file.toPath()).filter(line -> line.startsWith("nameserver"))
                        .map(line -> {
                            final String uriToParse = line.replace("nameserver ", "").trim();
                            final String[] split = uriToParse.split(":");
                            try {
                                if (split.length == 1) {
                                    return new URI("dns", "", split[0], 53, "", "", "");
                                } else if (split.length >= 2) {
                                    return new URI("dns", "", split[0], Integer.parseInt(split[1]), "", "", "");
                                } else {
                                    throw new IllegalStateException("Unable to parse URI from /etc/resolv.conf");
                                }
                            } catch (URISyntaxException e) {
                                throw new IllegalStateException("failed to convert to URI");
                            }

                        })
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IllegalStateException("can not read /etc/resolv.conf", e);
            }
        } else {
            throw new IllegalStateException("" + file + " not found");
        }
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
                    final String portString = UriUtils.splitQuery(dnsQuery.getQuery()).get(PORT_QUERY_KEY);
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

                    resolveA(0, dnsQuery.getPath().substring(1), port, promise);
                    break;

                case SRV_SCHEME:
                    resolveSRV(0, dnsQuery.getPath().substring(1), promise);
                    break;

                default:
                    promise.reject(new IllegalArgumentException("dns queries must have a schema of A or SRV"));
            }
        });
    }

    private void resolveA(final int hostIndex,
                          final String serviceName,
                          final int port,
                          final Promise<List<URI>> promise) {

        if (hostIndex >= this.dnsHosts.size()) {
            promise.resolve(Collections.emptyList());
            return;
        }

        final URI currentHost = this.dnsHosts.get(hostIndex);

        this.vertx.createDnsClient(currentHost.getPort(), currentHost.getHost()).resolveA(serviceName,
                convertPromise(Promises.<List<String>>promise()
                        .then(list -> promise.resolve(list.stream()
                                .map(ipAddress -> URI.create(RESULT_SCHEME + "://" + ipAddress + ":" + port + "/"))
                                .peek(uri -> this.logger.debug("found service in dns A: {}", uri.toString()))
                                .collect(Collectors.toList())
                        ))
                        .catchError(error -> {
                            this.logger.warn("dns lookup failed: ", error);
                            resolveA(hostIndex + 1, serviceName, port, promise);
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
                                .map(srv -> URI.create(RESULT_SCHEME + "://" + srv.target() + ":" + srv.port() + "/" +
                                        srv.name() + "?priority=" + srv.priority() + "&weight=" + srv.weight()))
                                .peek(uri -> this.logger.debug("found service in dns SRV: {}", uri.toString()))
                                .collect(Collectors.toList())
                        ))
                        .catchError(error -> {
                            this.logger.warn("dns lookup failed: ", error);
                            resolveSRV(hostIndex + 1, serviceName, promise);
                        })
                )
        );
    }

}
