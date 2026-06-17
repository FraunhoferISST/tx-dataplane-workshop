# No Hype, Just Apps: Build your own Data Plane

In this workshop, we want to explore the new Data Plane Signaling protocol and try out the `Siglet` as an interface
between the control plane and our backend application.

## Prerequisites

The following prerequisites are required to run the workshop setup:

- Java 17+
- [Docker](https://docs.docker.com/engine/install/)
- [KinD](https://kind.sigs.k8s.io/docs/user/quick-start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/) (for Traefik installation)
- a POSIX compliant shell (bash, zsh, ...)
- [Node.js](https://nodejs.org/en/download)/[npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) (for running npx/the Bruno CLI)
- [Bruno](https://www.usebruno.com/downloads)
- [optional] a Kubernetes monitoring tool like [k9s](https://k9scli.io/topics/install/) or [Lens](https://docs.k8slens.dev/k8slens/getting-started/install-lens/)

## The setup

In this workshop, we want to run an end-to-end data transfer using the new Data Plane Signaling protocol and the
Siglet. Therefore, we will deploy the following components:

- an [issuer](./launchers/issuerservice), which will issue required Catena-X credentials
- an [identity hub](./launchers/identityhub), which will act as the wallet for both provider and consumer
- 2 [control planes](./launchers/controlplane), one for provider and one for consumer
- 2 Siglets, one for provider and one for consumer
- 2 [data planes/backend applications](./launchers/dataplane), one for provider and one for consumer

If you are interested in the components in detail, follow the links to the respective directories. To give you an
overview:
- `Issuer` and `Identity Hub` build on `BOMs` from upstream EDC, bringing all basic functionality for their respective roles
- `Control Plane` builds on the Tractus-X EDC controlplane, but integrates the new Data Plane Signaling protocol (all changes made to the runtime are commented in the [build file](./launchers/controlplane/build.gradle.kts)
- `Data Plane`: a simple EDC-based runtime that exposes a dummy data endpoint and uses the Siglet for verifying received auth tokens
- `Siglet`: is taken as a pre-built image from the [Data Plane Core project](https://github.com/eclipse-dataplane-core/dsdk-facet-rs)

Additionally, some components are deployed for monitoring and telemetry. After the deployment is done, the following
services will be available:

| Service    | URL                           | Remark                               |
|------------|-------------------------------|--------------------------------------|
| Grafana    | `http://grafana.localhost`    |                                      |
| Prometheus | `http://prometheus.localhost` | can also be accessed through Grafana |
| Jaeger     | `http://jaeger.localhost`     | can also be accessed through Grafana |
| Loki       | `http://loki.localhost`       | can also be accessed through Grafana |


## Prepare the setup

As this workshop is focused on the data plane, we don't want to concern ourselves with the other components like, e.g.
the identity hub. But as other components are required to perform an end-to-end data transfer, we will set them up and 
initialize them via prepared scripts.

All scripts are located in the `./scripts/` directory. To ensure that all scripts are executable, run the following
command:

```shell
chmod +x -R ./scripts/
```

**Note, that all commands in this file should be run from the root directory, unless specifically stated otherwise.**

### Build launchers

Before deploying our setup in Kubernetes, we need to build and dockerize all components. This is done via the 
following script:

```shell
./scripts/build-launchers.sh
```

### Prepare KinD cluster

Now, we can create our KinD cluster, deploy `Traefik` as the ingress controller and install custom resource definitions
for `Gateway API`. This will allow us to call all services via localhost with a single port-forward (as shown later). 
To create and prepare the cluster, run the following script:

```shell
./scripts/init-cluster.sh
```

### Deployment

Once the cluster is prepared, we can deploy everything. For an in-depth view on everything we will deploy, check out the
`deployment/` directory. Next to the components mentioned above, this includes gateway, telemetry components, vault and
database. To deploy everything, run the following command:

```shell
kubectl apply -k deployment/
```

Since we are deploying quite a few things, it will take a few minutes until everything is ready. You can watch the
deployment by using a tool like `k9s`. If you are just using `kubectl`, you can check the status by running the
following command:

```shell
kubectl get pods -n edc
```

When all pods in the `edc` namespace are marked as ready, we're good to go. The output of the above command should
then look similar to this:

```shell
NAME                                    READY   STATUS      RESTARTS      AGE
consumer-controlplane-94d87879b-kzq5l   1/1     Running     2 (78s ago)   3m9s
consumer-dataplane-67999bd8d6-zff86     1/1     Running     0             3m9s
consumer-siglet-649f4bbf78-9t9wm        2/2     Running     0             3m9s
grafana-85449bccf4-82vmd                1/1     Running     0             3m9s
identityhub-9dd696d7-fk6xg              1/1     Running     2 (73s ago)   3m9s
issuerservice-57d5c9fbcb-dgz5k          1/1     Running     2 (73s ago)   3m9s
jaeger-76869d7b8c-xs2vl                 1/1     Running     0             3m9s
loki-76db898f9d-twn66                   1/1     Running     0             3m9s
postgres-6f6f5665bd-wtqhr               1/1     Running     0             3m9s
prometheus-7857b8484f-vp69f             1/1     Running     0             3m8s
provider-controlplane-f6c955c65-dfl8p   1/1     Running     2 (72s ago)   3m8s
provider-dataplane-77d49bb7c8-gfqrm     1/1     Running     0             3m8s
provider-siglet-7bb6bf4447-7fcq2        2/2     Running     0             3m8s
vault-84574456c6-55vjw                  1/1     Running     0             3m8s
vault-bootstrap-g295c                   0/1     Completed   0             3m9s
```

In order to run the requests in the following step, we need to forward the `Traefik` service to our localhost. This
will give us access to all routes configured in `Gateway API`. To forward the service port, run the following command:

```shell
sudo -E kubectl -n traefik port-forward svc/traefik 80:8000
```

**Keep the terminal, in which you started the port-forward, open, as closing it will remove the port-forward!
Alternatively, you can also start the port-forward using a tool like `k9s`.**

### Prepare transfer

Once all the pods are ready, we can start using the setup. However, some preparation of the running components is still
required, like issuing VCs so that the control planes can communicate. Since we want to focus on the data transfer,
you can run all preparatory requests via a script. These requests include:
- configuring the issuer so that it can issue our required credentials
- configuring the identity hub
- requesting `MembershipCredential`, `BpnCredential` and `DataExchangeGovernanceCredential` for both provider and consumer
- registering the data planes at their respective control planes
- creating an offer on the provider side

```shell
./scripts/prepare-transfer.sh
```

## Workshop tasks

Now that all preparation is complete, we can get started with the workshop. The main goal of this workshop is for you
to gather some first experiences with the Data Plane Signaling protocol and the Siglet. Therefore, feel free to explore
the setup in any way you want, e.g. by going through the configuration of control plane, data plane and Siglet, and
see where things may be unclear, or where you have questions. In the following, 3 tasks are listed that can help
guide you in exploring the setup, from running a simple, pre-configured data transfer to modifying the backend request
and related configurations and requests.

As long as you have port-forwarding active, you can use the [Bruno collection](./bruno-collection) to interact with
all components. Simply open the collection directory in Bruno and all requests as well as the environment are
ready to be used.

### Run a simple data transfer

The first thing we want to do is running a simple PULL transfer using the setup as-is. For this, open the Bruno
collection and run the requests in `03_consumer_negotiation` and `04_consumer_transfer` sequentially. In response to
the last request, you should receive the dummy data returned by our backend: `this is the dummy data :-)`

Notes:
- `03_consumer_negotiation/02_get-negotiation`: the `state` in the response should be `FINALIZED` before continuing
- `04_consumer_transfer/01_get-transfer`: the `state` in the response should be `STARTED` before continuing

You will notice that the request flow for a PULL transfer is very similar to how it was with the old implementation,
just using slightly different endpoints and formats.

### Use different backend endpoints for assets

In the pre-configured data transfer, we used a static endpoint for the transfer type `HttpData-PULL`. While this may
be sufficient for some cases, usually we want to provide different endpoints for different assets, even when they are
all made available via the `HttpData-PULL` transfer type. The Siglet provides support for this, too, which we will
try out in this task. The backend's
[DataController](./launchers/dataplane/src/main/java/de/fraunhofer/isst/tx/dataplane/DataController.java) provides two
endpoints: the first has a static path `/download` (this is the one we used in the previous task), the second
has a parametrized path `/download/{id}`. In this task, we want to use the second endpoint to simulate different
backend URLs for different assets, all served via the data addresses provided by the Siglet. Therefore, we will need
to do the following things:

#### 1. Modify the Siglet configuration

If you take a look at the [Siglet configuration](./deployment/runtimes/00-siglet-config.yaml) for provider and consumer,
you will see that for the transfer type `HttpData-PULL`, we configured a static endpoint pointing to our backend's
`/download` endpoint. We now need to adjust this configuration as shown in the
[Siglet documentation](https://github.com/eclipse-dataplane-core/dsdk-facet-rs/blob/main/docs/siglet.md#complete-configuration-reference).
In the shown configuration example, we see entries for both static and dynamic endpoints, the latter using
`endpoint_mappings`.

For this task, we want to replace our current configuration for static endpoints with one for dynamic endpoints.
Therefore, replace the current configuration for `[[transfer_types]]` with one matching the structure for dynamic
endpoints from the example. Create two `[[transfer_types.endpoint_mappings]]` entries, in which you can define an
arbitrary key-value pair for each mapping - we'll see how they relate to different assets in the next step. For the
endpoint, use a different variation of `http://provider.localhost/api/dp/data/download/{id}` for each mapping, replacing
`{id}` with an arbitrary string.

#### 2. Create assets

For this task, we cannot re-use the asset from the first tasks, as this is missing one important thing: the 
`dataplaneMetadata`. This is a collection of additional information in relation to the data plane. If you take
a look at the request `02_provider/00_create-asset`, you will see that the `dataplaneMetadata` field is already prepared
in the request body, but contains as of now only empty fields. The metadata encompasses the following information:
- `properties`: any additional information that is required by & forwarded to the data plane to facilitate the transfer, e.g. an internal ID of the data set to be transferred
- `labels`: used by the control plane to select a data plane for a given transfer, as data planes are registered with labels
- `profiles`: defines under which transfer types an asset is available (must match the transfer types the data plane is registered with)

For our assets, we only want to focus on the `properties` of the metadata, so we will leave the other fields empty. This
does not cause any issues, as
- empty `labels` match all registered data planes disregarding of the labels they're tagged with
- empty `profiles` mean all transfer types supported by the registered data plane are used

#### 3. Run a transfer for both assets

To verify that different endpoints are used for different assets, run the following steps twice (once for each
`[[transfer_types.endpoint_mappings]]` entry you defined in the Siglet configuration):
- change the `asset-id` in the Bruno environment to a new value
- update the request body for the create-asset-request: add the key-value pair defined in the [[transfer_types.endpoint_mappings]] entry to the `properties` of the `dataplaneMetadata`
- run the subsequent requests from the Bruno collection in order (keep everything else as-is, the asset ID will automatically be updated as defined in the Bruno environment)
- after running the request `04_consumer_transfer/02_fetch-token`, the endpoint in the response should match the one defined in the [[transfer_types.endpoint_mappings]] of which the key-value pair as added to the asset

You should now have received different data for your two assets! In the same way we now configured endpoints on the
Siglet and defined matching key-value pairs in our assets, you can expose different backends for a given transfer type
through a single instance of the Siglet, leaving all token handling and data address generation to the Siglet.

### Use a different application as backend

> Now, the third task is a bit of a bigger and more abstract one, that likely will require more time to complete than is
available in this workshop. Still, it may be interesting to start thinking about solutions and discussing them 
within the workshop!

For the last task, we want to take a look at how we can connect other, existing applications with the Siglet. Maybe you
are using a or working on a use-case-app or specific backend in your dataspace use cases. If you have an application
available to be deployed, you can try modifying and integrating it directly, or, if you are familiar with an
application's code, you can draft the steps necessary to connect it via the Siglet. Thus, let's take a look at what
is required for an application to be connected via the Siglet.

Generally, the coupling between the Siglet and the application is loose. The application's endpoint is configured in
the Siglet as seen in the previous task and the Siglet will return this endpoint as part of a data address when a
transfer is requested; but the Siglet never talks to the application directly. The Siglet will generate the tokens for
accessing the backend though, therefore the application needs to accept tokens issued by the Siglet. Thus, the
only area in your application that needs to be aware of the Siglet is the authentication/authorization layer.

So, let's take a look at the tokens issued and verification methods offered by the Siglet!

#### Siglet-issued tokens

The Siglet generates and signs JWTs, which, next to the standard claims, contain some dataspace-specific claims like
the `datasetId`, `agreementId` and `counterPartyId`, which can be used to perform authorization checks. In the future,
it's planned to also add the counter-party's VC claims here to allow for a more fine-grained access control. You can
find an overview of all claims in the
[Siglet documentation](https://github.com/eclipse-dataplane-core/dsdk-facet-rs/blob/main/docs/siglet.md#token-shape).

#### Siglet token verification

Tokens issued by the Siglet can be verified in two ways: calling the Siglet's JWKS endpoint and using the public key
information to verify the token's signature locally, or using the Siglet's verification endpoint in cases where
verification needs to be revocation-aware. You can find detailed information on both verification methods in the
[Siglet documentation](https://github.com/eclipse-dataplane-core/dsdk-facet-rs/blob/main/docs/siglet.md#token-verification).

#### Integration into your application

Depending on your application, it may be more or less complex to modify your auth layer to accept tokens issued by the
Siglet. Which steps would be necessary for your application to do so? Gather some ideas, draft an implementation plan
or let's simply discuss!

## Cleanup

Once you are done with the workshop, you can simply delete the cluster by running:

```shell
kind delete cluster
```

This will also remove all deployed resources and will leave your system in a clean state.
