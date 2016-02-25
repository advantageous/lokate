# Discovery Service
Vert.x service for service discovery.

In order to run the tests you will need a few things.

You will need docker and kitematic running on your OSX box.

Inside of Docker you will need to run `socat`.

#### Running socat to open up Docker to non-ssl traffic
```
$ docker run -d -p 2375:2375 \
       --volume=/var/run/docker.sock:/var/run/docker.sock \
       --name=docker-http sequenceiq/socat
```


## VPN / DNS known issue

Currently we need DNS from Amazon which is only accessible via VPN.
Our tests are bittle in that they rely on EC2 and the VPN.
Also they rely on api1 being in the DNS server.

Thus if this does not work.

#### Dig that has to work for tests to pass
```
dig ns-171.awsdns-21.com  api1.rbtv.preprod.data.metriculo.us  any

```

If the above is not working then neither will the unit tests.

## Open issue Marathon

Currently the unit tests for Marathon (which are in a branch until we have
a resolution) rely on Marathon running in EC2.
We setup a dev instances of a Mesos / Marathon cluster for testing.

## Amazon Discovery
In order for Amazon EC2 Discovery to work, you need credentials.

The credentials file is stored in the home directory.

#### EC2 Credentials file
```
ls ~/.aws/credentials
/Users/rick/.aws/credentials
```

### Credential file format
```
[default]
aws_access_key_id = ABCDEFGHIJKLMNOP123OWOA
aws_secret_access_key = 42WABCDEFGHIJKLMNOP123OWOAzW1kVFUCK3
#region=us-west-2
```

This should be easy to setup on Jenkins.
