package com.redbullsoundselect.platform.discovery.impl;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.redbullsoundselect.platform.discovery.DiscoveryService;
import com.redbullsoundselect.platform.discovery.ServiceDefinition;
import com.typesafe.config.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Looks up a service by its Amazon Ec2 instance Name.
 * The results are limited by using the name of the pem key.
 * We should probably use security group name or VPC id as options.
 * If the keyName is not set (set to "") then we do not use the keyname.
 * If more than one instance is found, under that name, then this randomly picks one.
 *
 * @author Rick Hightower
 */
public class AmazonEc2DiscoveryService implements DiscoveryService {

    private final Map<String, Integer> nameToPort;
    private final Vertx vertx;
    private final boolean usePublicAddress;
    private final String ec2Endpoint;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AmazonEc2DiscoveryService(final Vertx vertx,
                                     final Map<String, Integer> nameToPort,
                                     final boolean usePublicAddress,
                                     final String ec2Endpoint) {
        this.nameToPort = nameToPort;
        this.vertx = vertx;
        this.usePublicAddress = usePublicAddress;
        this.ec2Endpoint = ec2Endpoint;

        if (logger.isInfoEnabled()) {
            logger.info("Created Amazon EC2 Discovery Service {} {} {} {}",
                    usePublicAddress, ec2Endpoint, nameToPort);
        }
    }

    public AmazonEc2DiscoveryService(final Vertx vertx,
                                     final Config config) {

        this.nameToPort = new HashMap<>();

        final Config nameToPortConfig = config.getConfig("name-to-port");

        nameToPortConfig.entrySet().forEach(entry -> nameToPort.put(entry.getKey(),
                nameToPortConfig.getInt(entry.getKey())));

        this.vertx = vertx;
        this.usePublicAddress = config.getBoolean("use-public-address");
        this.ec2Endpoint = config.getString("ec2-endpoint");

        if (logger.isInfoEnabled()) {
            logger.info("Created Amazon EC2 Discovery Service {} {} {} {}",
                    usePublicAddress, ec2Endpoint, nameToPort);
        }
    }

    public List<Instance> lookupServiceByFilter(final String ec2Endpoint,
                                                final Filter... filters) {

        final AmazonEC2Client amazonEC2Client = new AmazonEC2Client();
        amazonEC2Client.setEndpoint(ec2Endpoint);

        final DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setFilters(Arrays.asList(filters));
        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(request);
        final List<Instance> instances = new ArrayList<>();

        while (true) {
            describeInstancesResult.getReservations().stream().forEach(
                    reservation -> reservation.getInstances().stream()
                            .forEach((Consumer<Instance>) instance -> {
                                if (logger.isInfoEnabled()) {
                                    logger.info("Amazon SD Lookup imgid {} instanceid {} life {} keyname {} nic 1 DNS {}" +
                                                    " pub dns {} priv ip {}",
                                            instance.getImageId(),
                                            instance.getInstanceId(),
                                            instance.getInstanceLifecycle(),
                                            instance.getKeyName(),
                                            instance.getNetworkInterfaces().get(0).getPrivateDnsName(),
                                            instance.getPublicDnsName(),
                                            instance.getPrivateIpAddress());
                                    instance.getTags().stream().forEach(tag ->
                                            logger.info("\t Amazon SD Lookup {} Tag {} {} ",
                                                    instance.getInstanceId(),
                                                    tag.getKey(), tag.getValue()));

                                }

                                instances.add(instance);
                            })
            );

            final String token = describeInstancesResult.getNextToken();
            if (token == null) {
                break;
            } else {
                request.setNextToken(token);
                describeInstancesResult = amazonEC2Client.describeInstances(request);
            }
        }
        return instances;
    }

    @Override
    public void lookupServiceByName(final String name,
                                    final Handler<AsyncResult<ServiceDefinition>> result) {
        final int port = this.nameToPort.getOrDefault(name, -1);
        callEc2LookupAsync(name, result, port);
    }


    @Override
    public void lookupServiceByNameAndContainerPort(final String name,
                                                    final int port,
                                                    final Handler<AsyncResult<ServiceDefinition>> result) {
        callEc2LookupAsync(name, result, port);
    }

    private void callEc2LookupAsync(final String name,
                                    final Handler<AsyncResult<ServiceDefinition>> result,
                                    final int port) {

        vertx.executeBlocking(future ->
                lookupServiceAndRespond(name, future, port), false, asyncResult ->
                extractAsyncCallToResult(result, asyncResult)
        );
    }

    private void extractAsyncCallToResult(final Handler<AsyncResult<ServiceDefinition>> result,
                                          final AsyncResult<Object> asyncResult) {
        if (asyncResult.failed()) {
            result.handle(Future.failedFuture(asyncResult.cause()));
        } else {
            result.handle(Future.succeededFuture((ServiceDefinition) asyncResult.result()));
        }
    }


    private void lookupServiceAndRespond(final String name,
                                         final Future<Object> future,
                                         final int port) {

        final List<Instance> instances = doLookupService(name);

        if (instances.size() > 0) {
            Instance instance = instances.get(0);
            ServiceDefinition serviceDefinition = getServiceDefinitionFromEC2Instance(port, instance);
            future.complete(serviceDefinition);
        } else {
            future.fail(new IllegalStateException("Service not found " + name));
        }
    }

    private List<Instance> doLookupService(final String name) {
        return lookupServiceByFilter(this.ec2Endpoint, new Filter("tag:Name", Collections.singletonList(name)));
    }


    private ServiceDefinition getServiceDefinitionFromEC2Instance(final int port,
                                                                  final Instance instance) {
        if (usePublicAddress) {
            return new ServiceDefinition(instance.getPublicIpAddress(), port);
        } else {
            return new ServiceDefinition(instance.getPrivateIpAddress(), port);
        }
    }
}
