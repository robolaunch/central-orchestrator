package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.kubernetes.V1VirtualCluster;
import org.robolaunch.repository.abstracts.AmazonRepository;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.KubernetesRepository;
import org.robolaunch.repository.abstracts.CloudInstanceRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.cert.manager.models.V1alpha2Certificate;
import io.cert.manager.models.V1alpha2CertificateList;
import io.cert.manager.models.V1alpha2Issuer;
import io.cert.manager.models.V1alpha2IssuerList;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AdmissionregistrationV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1MutatingWebhookConfiguration;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1ValidatingWebhookConfiguration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.ModelMapper;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

@ApplicationScoped
public class CloudInstanceRepositoryImpl implements CloudInstanceRepository {
        private DynamicKubernetesApi machineDeploymentApi;
        private CoreV1Api coreV1Api;
        private DynamicKubernetesApi clusterVersionApi;
        private DynamicKubernetesApi virtualClustersApi;
        private DynamicKubernetesApi subnetsApi;
        private AppsV1Api appsApi;

        @ConfigProperty(name = "quarkus.oidc.client.id")
        String clientId;
        @ConfigProperty(name = "quarkus.oidc.client.credentials")
        String clientSecret;
        @ConfigProperty(name = "keycloak.url")
        String keycloakURL;
        @ConfigProperty(name = "oauth.client.id")
        String oauthClientId;
        @ConfigProperty(name = "openssl.str")
        String openSSLString;
        @ConfigProperty(name = "dns.zone")
        String dnsZoneName;
        @ConfigProperty(name = "robolaunch.helm.repo")
        String robolaunchHelmRepo;

        @Inject
        StorageRepository storageRepository;
        @Inject
        KeycloakAdminRepository keycloakAdminRepository;
        @Inject
        CloudInstanceHelperRepository cloudInstanceHelperRepository;
        @Inject
        GroupAdminRepository groupAdminRepository;
        @Inject
        AmazonRepository amazonRepository;
        @Inject
        KubernetesRepository kubernetesRepository;
        @Inject
        GroupAdapter groupAdapter;
        @Inject
        JsonWebToken jwt;

        @PostConstruct
        public void initializeApis() throws IOException {
                ApiClient apiClient = ClientBuilder.standard().build();
                this.machineDeploymentApi = new DynamicKubernetesApi("cluster.k8s.io", "v1alpha1",
                                "machinedeployments",
                                apiClient);
                this.coreV1Api = new CoreV1Api(apiClient);
                this.clusterVersionApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
                                "clusterversions", apiClient);
                this.virtualClustersApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
                                "virtualclusters", apiClient);
                this.subnetsApi = new DynamicKubernetesApi("kubeovn.io", "v1",
                                "subnets", apiClient);
                this.appsApi = new AppsV1Api(apiClient);
        }

        @Override
        public void createMachineDeployment(String bufferName, String instanceType)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException {
                String awsType = "";
                Integer diskSize = 50;
                if (instanceType.equals("r1.aws.cpu")) {
                        awsType = "t3a.xlarge";
                } else if (instanceType.equals("r1.aws.gpu")) {
                        awsType = "t2.medium"; // g4dn.xlarge
                } else if (instanceType.equals("r1.hz.cpu")) {
                        awsType = "CPX31"; // CCX31
                }
                Artifact artifact = new Artifact();
                artifact.setName("machineDeployment.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "md-" + bufferName);

                object.get("spec").getAsJsonObject().get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("providerSpec").getAsJsonObject().get("value").getAsJsonObject()
                                .get("cloudProviderSpec").getAsJsonObject()
                                .addProperty("instanceType", awsType);

                object.get("spec").getAsJsonObject().get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("providerSpec").getAsJsonObject().get("value").getAsJsonObject()
                                .get("cloudProviderSpec").getAsJsonObject()
                                .addProperty("diskSize", diskSize);

                object.get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/buffer-instance", bufferName);

                machineDeploymentApi.create(new DynamicKubernetesObject(object));
        }

        @Override
        public void claimTheSuperClusterNode(String nodeName, String bufferName)
                        throws IOException, KubectlException, ApiException {
                ApiClient apiClient = ClientBuilder.standard().build();
                Kubectl.label(V1Node.class).apiClient(apiClient)
                                .name(
                                                nodeName)
                                .addLabel("robolaunch.io/buffer-instance", bufferName)
                                .execute();
                Kubectl.label(V1Node.class).apiClient(apiClient)
                                .name(
                                                nodeName)
                                .addLabel("node-role.kubernetes.io/worker", "worker")
                                .execute();
        }

        @Override
        public void createClusterVersion(String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException {
                Artifact artifact = new Artifact();
                artifact.setName("clusterVersion.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "cv-" + bufferName);
                object.get("spec").getAsJsonObject().get("etcd").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/buffer-instance", bufferName);
                object.get("spec").getAsJsonObject().get("apiServer").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/buffer-instance", bufferName);
                object.get("spec").getAsJsonObject().get("controllerManager").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/buffer-instance", bufferName);
                object.get("spec").getAsJsonObject().get("apiServer")
                                .getAsJsonObject().get("statefulset").getAsJsonObject()
                                .get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec").getAsJsonObject()
                                .get("containers").getAsJsonArray()
                                .get(0).getAsJsonObject().get("args").getAsJsonArray()
                                .add("--oidc-issuer-url=" + keycloakURL + "/realms/buffer-realm");
                clusterVersionApi.create(new DynamicKubernetesObject(object)).throwsApiException();
        }

        @Override
        public void createVirtualCluster(String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException {
                Artifact artifact = new Artifact();
                artifact.setName("virtualCluster.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "vc-" + bufferName);
                object.get("spec").getAsJsonObject().addProperty("clusterDomain",
                                "vc-" + bufferName + ".local");
                object.get("spec").getAsJsonObject().addProperty("clusterVersionName", "cv-" + bufferName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject()
                                .addProperty("robolaunch.io/buffer-instance", bufferName);
                virtualClustersApi.create(new DynamicKubernetesObject(object)).throwsApiException();
        }

        @Override
        public void scaleStatefulSetsDown(String bufferName) throws ApiException, KubectlException, IOException {
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
                ApiClient apiClient = ClientBuilder.standard().build();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName).name("etcd")
                                .replicas(0).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName)
                                .name("controller-manager")
                                .replicas(0).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName).name("apiserver")
                                .replicas(0).execute();
        }

        @Override
        public void drainNode(String nodeName) throws IOException, KubectlException {

                try {
                        ApiClient apiClient = ClientBuilder.standard().build();
                        Kubectl.drain().ignoreDaemonSets().gracePeriod(60).name(nodeName).apiClient(apiClient)
                                        .execute();
                } catch (KubectlException e) {
                        // TODO: handle exception
                }

        }

        @Override
        public void uncordonNode(String nodeName) throws IOException, KubectlException {
                ApiClient apiClient = ClientBuilder.standard().build();
                Kubectl.uncordon().name(nodeName).apiClient(apiClient).execute();
        }

        @Override
        public void labelVirtualCluster(String bufferName, Organization organization, String departmentName,
                        String superClusterName, String cloudInstanceName, Boolean connectionHub)
                        throws IOException, KubectlException, InterruptedException {
                ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
                                "virtualclusters", true,
                                V1VirtualCluster.class);
                ApiClient client = Config.defaultClient();

                if (!connectionHub) {
                        Kubectl.label(V1VirtualCluster.class).apiClient(client)
                                        .skipDiscovery()
                                        .namespace("default")
                                        .name("vc-" + bufferName)
                                        .addLabel("robolaunch.io/buffer-instance", bufferName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/department", departmentName)
                                        .addLabel("robolaunch.io/super-cluster", superClusterName)
                                        .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                        .execute();
                } else {
                        Kubectl.label(V1VirtualCluster.class).apiClient(client)
                                        .skipDiscovery()
                                        .namespace("default")
                                        .name("vc-" + bufferName)
                                        .addLabel("robolaunch.io/buffer-instance", bufferName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/department", departmentName)
                                        .addLabel("robolaunch.io/super-cluster", superClusterName)
                                        .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                        .addLabel("submariner.io/gateway", "4490")
                                        .execute();
                }

        }

        @Override
        public void addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
                        String departmentName, String superClusterName, Boolean connectionHub)
                        throws KubectlException, IOException {
                try {
                        ApiClient apiClient = ClientBuilder.standard().build();
                        if (!connectionHub) {
                                Kubectl.label(V1Node.class).apiClient(apiClient)
                                                .name(nodeName)
                                                .addLabel("robolaunch.io/organization", organization.getName())
                                                .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                                .addLabel("robolaunch.io/department", departmentName)
                                                .addLabel("robolaunch.io/super-cluster", superClusterName)
                                                .execute();
                        } else {
                                Kubectl.label(V1Node.class).apiClient(apiClient)
                                                .name(nodeName)
                                                .addLabel("robolaunch.io/organization", organization.getName())
                                                .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                                .addLabel("robolaunch.io/department", departmentName)
                                                .addLabel("robolaunch.io/super-cluster", superClusterName)
                                                .addLabel("submariner.io/gateway", "4490")
                                                .execute();
                        }

                } catch (KubectlException e) {
                }

        }

        @Override
        public void addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
                        String departmentName,
                        String cloudInstanceName, String superClusterName, String bufferName) throws ApiException {
                try {
                        V1StatefulSetList statefulsets = appsApi.listNamespacedStatefulSet(
                                        namespaceName, null, null, null,
                                        null, null, null, null, null, null,
                                        null);
                        for (V1StatefulSet statefulset : statefulsets.getItems()) {
                                String patchString = "[{\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1organization\",\"value\": \""
                                                + organization.getName()
                                                + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1department\",\"value\": \""
                                                + departmentName
                                                + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1cloud-instance\",\"value\": \""
                                                + cloudInstanceName
                                                + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1super-cluster\",\"value\": \""
                                                + superClusterName
                                                + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1buffer-instance\",\"value\": \""
                                                + bufferName + "\"}]";

                                if (statefulset.getMetadata().getName().equals("apiserver")) {
                                        String replacePatch = "[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/args/37\",\"value\": \""
                                                        + "--oidc-issuer-url=" + keycloakURL + "/realms/"
                                                        + organization.getName()
                                                        + "\"}]";

                                        appsApi.patchNamespacedStatefulSet(statefulset.getMetadata().getName(),
                                                        namespaceName,
                                                        new V1Patch(replacePatch),
                                                        null, null,
                                                        null, null, null);
                                }

                                appsApi.patchNamespacedStatefulSet(statefulset.getMetadata().getName(), namespaceName,
                                                new V1Patch(patchString),
                                                null, null,
                                                null, null, null);

                                appsApi.patchNamespacedStatefulSet(statefulset.getMetadata().getName(), namespaceName,
                                                new V1Patch(patchString),
                                                null, null,
                                                null, null, null);

                                appsApi.patchNamespacedStatefulSet(statefulset.getMetadata().getName(), namespaceName,
                                                new V1Patch(patchString),
                                                null, null,
                                                null, null, null);
                        }

                } catch (ApiException e) {
                        System.out.println(e.getCode());
                        System.out.println(e.getResponseBody());
                }

        }

        @Override
        public void scaleStatefulSetsUp(String bufferName) throws ApiException, IOException, KubectlException {
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
                ApiClient apiClient = ClientBuilder.standard().build();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName).name("etcd")
                                .replicas(1).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName)
                                .name("controller-manager")
                                .replicas(1).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(apiClient).namespace(namespaceName).name("apiserver")
                                .replicas(1).execute();
        }

        /* Create subnet for namespace */
        @Override
        public void createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException {
                Artifact artifact = new Artifact();
                artifact.setName("subnet.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-default");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-kube-node-lease");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-kube-public");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-kube-system");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-oauth2-proxy");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-robot-system");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-cert-manager");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-fleet-system");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-submariner-operator");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-kube-federation-system");
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "subnet-" + namespaceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance",
                                cloudInstanceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/department",
                                departmentName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/organization",
                                organization.getName());
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/super-cluster",
                                superClusterName);
                var subnets = subnetsApi.list();
                int counter = 1;
                while (true) {
                        String subnetIP = "10.10." + counter + ".0/24";
                        if (subnets.getObject().getItems().stream()
                                        .anyMatch(subnet -> subnet.getRaw().get("spec").getAsJsonObject()
                                                        .get("cidrBlock").getAsString().equals(subnetIP))) {
                                counter++;
                        } else {
                                break;
                        }
                }

                String mySubnetIP = "10.10." + counter + ".0/24";
                object.get("spec").getAsJsonObject().addProperty("cidrBlock", mySubnetIP);
                object.get("spec").getAsJsonObject().get("excludeIps").getAsJsonArray().remove(0);
                object.get("spec").getAsJsonObject().get("excludeIps").getAsJsonArray()
                                .add("10.10." + counter + ".1");
                object.get("spec").getAsJsonObject().get("acls").getAsJsonArray().get(0).getAsJsonObject().addProperty(
                                "match",
                                "ip4.src==" + mySubnetIP + " && " + "ip4.dst==" + mySubnetIP);

                object.get("spec").getAsJsonObject().get("acls").getAsJsonArray().get(1).getAsJsonObject().addProperty(
                                "match",
                                "ip4.src==10.10.0.0/16 && ip4.dst==10.10.0.0/16");

                object.get("spec").getAsJsonObject().get("acls").getAsJsonArray().get(2).getAsJsonObject().addProperty(
                                "match",
                                " ip4.src==0.0.0.0/0 && ip4.dst==" + mySubnetIP);

                object.get("spec").getAsJsonObject().get("acls").getAsJsonArray().get(3).getAsJsonObject().addProperty(
                                "match",
                                "ip4.src==" + mySubnetIP + " && ip4.dst==0.0.0.0/0");

                subnetsApi.create(new DynamicKubernetesObject(object)).throwsApiException();

                String yamlString = cloudInstanceHelperRepository.convertJsonStringToYamlString(object.toString());
                Artifact artifact2 = new Artifact();
                artifact2.setClusterName(cloudInstanceName);
                artifact2.setName("subnet.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact2, organization.getName());

        }

        @Override
        public void createVirtualLink(String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName, String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                System.out.println("Created the client");
                Artifact artifact = new Artifact();
                artifact.setName("virtualLink.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                Object obj = Yaml.load(yaml);
                V1Job job = (V1Job) obj;
                BatchV1Api batchV1Api = new BatchV1Api(vcClient);
                job.getMetadata().setNamespace("default");
                Map<String, String> nodeSelectors = new HashMap<>();
                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                nodeSelectors.put("robolaunch.io/cloud-instance", cloudInstanceName);
                nodeSelectors.put("robolaunch.io/department", departmentName);
                nodeSelectors.put("robolaunch.io/super-cluster", superClusterName);
                job.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectors);
                var subnet = subnetsApi.get("subnet-" + namespaceName);
                var machineDeployment = machineDeploymentApi.get("kube-system", "md-" + bufferName);
                String subnetId = machineDeployment.getObject().getRaw().getAsJsonObject().get("spec").getAsJsonObject()
                                .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("providerSpec").getAsJsonObject().get("value")
                                .getAsJsonObject().get("cloudProviderSpec").getAsJsonObject().get("subnetId")
                                .getAsString().substring(0, 9);
                System.out.println("got subnetid");
                List<String> args = new ArrayList();
                args.add("ip link add "
                                + subnetId
                                + " type dummy; ip addr add "
                                + subnet.getObject().getRaw().get("spec").getAsJsonObject()
                                                .get("cidrBlock").getAsString()
                                + "/32 brd + dev " + subnetId
                                + " label " + subnetId + ":0; ip link set dev " + subnetId + " up;");
                job.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(args);

                System.out.println("creating job");
                batchV1Api.createNamespacedJob("default", job, null, null, null, null);
        }

        @Override
        public void createOauth2ProxyNamespace(String bufferName)
                        throws ApiException, IOException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                CoreV1Api coreV1Api = new CoreV1Api(vcClient);
                V1Namespace namespace = new V1Namespace();
                namespace.setApiVersion("v1");
                namespace.setKind("Namespace");
                namespace.setMetadata(new V1ObjectMeta().name("oauth2-proxy"));
                coreV1Api.createNamespace(namespace, null, null, null, null);
        }

        @Override
        public void createTLSSecrets(String bufferName)
                        throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                Artifact artifactCrt = new Artifact();
                artifactCrt.setName("server.crt");
                Artifact artifactKey = new Artifact();
                artifactKey.setName("server.key");
                String bucket = "secrets";
                String contentCrt = storageRepository.getContent(artifactCrt, bucket);
                String contentKey = storageRepository.getContent(artifactKey, bucket);
                CoreV1Api coreV1Api = new CoreV1Api(vcClient);
                V1Secret secret = new V1Secret();
                secret.setApiVersion("v1");
                secret.setKind("Secret");
                secret.setMetadata(new V1ObjectMeta().name("tls"));
                secret.setType("kubernetes.io/tls");
                Map<String, byte[]> data = new HashMap<>();
                data.put("tls.crt", contentCrt.getBytes());
                data.put("tls.key", contentKey.getBytes());
                secret.setData(data);
                coreV1Api.createNamespacedSecret("default", secret, null, null, null,
                                null);
                coreV1Api.createNamespacedSecret("oauth2-proxy", secret, null, null, null,
                                null);
        }

        @Override
        public void createOAuth2ProxyResources(Organization organization, String departmentName,
                        String cloudInstanceName, String superClusterName, String namespaceName, String bufferName)
                        throws IllegalArgumentException, IOException, ApiException, JSONException,
                        GeneralSecurityException, MinioException, InterruptedException {
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("oauth2Proxy.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);
                String newNamespace = namespaceName + "-oauth2-proxy";
                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();

                        if (type.equals("V1ServiceAccount")) {
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                coreV1Api.createNamespacedServiceAccount(newNamespace, serviceAccount,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Secret")) {
                                V1Secret secret = (V1Secret) obj;

                                secret.getData().put("cookie-secret",
                                                openSSLString.getBytes(StandardCharsets.UTF_8));
                                secret.getData().put("client-id",
                                                oauthClientId.getBytes(StandardCharsets.UTF_8));
                                secret.getData().put("client-secret",
                                                keycloakAdminRepository
                                                                .getClientSecret(organization.getName(), oauthClientId)
                                                                .getBytes(StandardCharsets.UTF_8));
                                coreV1Api.createNamespacedSecret(newNamespace, secret, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ConfigMap")) {
                                V1ObjectMetaBuilder metadataBuilder = new V1ObjectMetaBuilder();
                                Map<String, String> labels = new HashMap<>();
                                labels.put("app", "oauth2-proxy");
                                labels.put("app.kubernetes.io/component", "authentication-proxy");
                                labels.put("app.kubernetes.io/instance", "oauth2-proxy");
                                labels.put("app.kubernetes.io/managed-by", "Helm");
                                labels.put("app.kubernetes.io/name", "oauth2-proxy");
                                labels.put("app.kubernetes.io/part-of", "oauth2-proxy");
                                labels.put("app.kubernetes.io/version", "7.3.0");
                                labels.put("helm.sh/chart", "oauth2-proxy-6.2.7");

                                V1ObjectMeta metadata = metadataBuilder.withName("oauth2-proxy")
                                                .withNamespace(newNamespace)
                                                .withLabels(labels)
                                                .build();

                                V1ConfigMapBuilder configMapBuilder = new V1ConfigMapBuilder();
                                Map<String, String> data = new HashMap<>();
                                data.put("oauth2_proxy.cfg",
                                                "provider = \"oidc\"\nprovider_display_name = \"Keycloak\"\noidc_issuer_url = \"https://keycloak.robolaunch.dev/auth/realms/"
                                                                + organization.getName()
                                                                + "\"\nemail_domains = [\"*\"]\nscope = \"openid profile email\"\nwhitelist_domains = \".robolaunch.dev\"\ncookie_domains= \".robolaunch.dev\"\npass_authorization_header = true\npass_access_token = true\npass_user_headers = true\nset_authorization_header = true\nset_xauthrequest = true\ncookie_refresh = \"1s\"\ncookie_expire = \"30m\"\nredirect_url= \"https://"
                                                                + organization.getName()
                                                                + ".robolaunch.dev/oauth2/callback\"");
                                V1ConfigMap configMap = configMapBuilder.withApiVersion("v1")
                                                .withKind("ConfigMap")
                                                .withMetadata(
                                                                metadata)
                                                .withData(data).build();
                                coreV1Api.createNamespacedConfigMap(
                                                newNamespace, configMap, null, null, null,
                                                null);

                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Service")) {
                                V1Service service = (V1Service) obj;
                                coreV1Api.createNamespacedService(newNamespace, service, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Deployment")) {
                                V1Deployment deployment = (V1Deployment) obj;
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/cloud-instance", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/department", departmentName);
                                nodeSelectors.put("robolaunch.io/super-cluster", superClusterName);
                                nodeSelectors.put("robolaunch.io/buffer-instance", bufferName);
                                deployment.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectors);

                                appsApi.createNamespacedDeployment(newNamespace, deployment, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                }
                Artifact artifact2 = new Artifact();
                artifact2.setClusterName(cloudInstanceName);
                artifact2.setName("oauth2Proxy.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact2,
                                organization.getName());

        }

        @Override
        public void createIngressRule(Organization organization, String cloudInstanceName,
                        String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, MinioException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("ingress.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                NetworkingV1Api networkingV1Api = new NetworkingV1Api(vcClient);
                Object obj = Yaml.load(yaml);
                V1Ingress ingress = (V1Ingress) obj;
                ingress.getMetadata().setName(bufferName + "-oauth2-proxy");
                ingress.getMetadata().setNamespace("oauth2-proxy");
                ingress.getSpec().getTls().get(0)
                                .setHosts(Arrays.asList(organization.getName() + ".robolaunch.dev"));
                ingress.getSpec().getTls().get(0).setSecretName("tls");
                ingress.getSpec().getRules().get(0)
                                .setHost(organization.getName() + ".robolaunch.dev");
                String jsonInString = new Gson().toJson(obj);
                JSONObject mJSONObject = new JSONObject(jsonInString);
                yamlString += cloudInstanceHelperRepository.convertJsonStringToYamlString(mJSONObject.toString());
                yamlString += "---";
                networkingV1Api.createNamespacedIngress("oauth2-proxy", ingress, null, null, null, null);
                Artifact artifact2 = new Artifact();
                artifact2.setClusterName(cloudInstanceName);
                artifact2.setName("ingress.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact2, organization.getName());
        }

        @Override
        public void createCoreDNS(Organization organization, String departmentName,
                        String cloudInstanceName, String nodeName, String bufferName, String superClusterName)
                        throws IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, ApiException, MinioException,
                        InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("coreDNS.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);
                CoreV1Api coreApi = new CoreV1Api(vcClient);
                RbacAuthorizationV1Api rbacAuthorizationV1Api = new RbacAuthorizationV1Api(vcClient);
                AppsV1Api appsV1Api = new AppsV1Api(vcClient);
                /* Get all resources and deploy them one by one */
                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();
                        if (type.equals("V1ServiceAccount")) {
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                coreApi.createNamespacedServiceAccount("kube-system", serviceAccount,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ClusterRole")) {
                                V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                rbacAuthorizationV1Api.createClusterRole(clusterRole, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";

                        }
                        if (type.equals("V1ClusterRoleBinding")) {
                                V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                rbacAuthorizationV1Api.createClusterRoleBinding(clusterRoleBinding, null,
                                                null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ConfigMap")) {
                                V1ObjectMetaBuilder metadataBuilder = new V1ObjectMetaBuilder();
                                V1ObjectMeta metadata = metadataBuilder.withName("coredns").withNamespace("kube-system")
                                                .build();

                                V1ConfigMapBuilder configMapBuilder = new V1ConfigMapBuilder();
                                Map<String, String> data = new HashMap<>();
                                data.put("Corefile",
                                                ".:53 {\n" +
                                                                "    log\n" +
                                                                "    errors\n" +
                                                                "    health {\n" +
                                                                "      lameduck 5s\n" +
                                                                "    }\n" +
                                                                "    ready\n" +
                                                                "    kubernetes " + "vc-" + bufferName
                                                                + ".local in-addr.arpa ip6.arpa {\n" +
                                                                "      fallthrough in-addr.arpa ip6.arpa\n" +
                                                                "    }\n" +
                                                                "    prometheus :9153\n" +
                                                                "    forward . 10.96.0.10\n" +
                                                                "    cache 30\n" +
                                                                "    loop\n" +
                                                                "    reload\n" +
                                                                "    loadbalance\n" +
                                                                " }");
                                V1ConfigMap configMap = configMapBuilder.withApiVersion("v1").withKind("ConfigMap")
                                                .withMetadata(
                                                                metadata)
                                                .withData(data).build();

                                coreApi.createNamespacedConfigMap("kube-system", configMap, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Service")) {
                                V1Service service = (V1Service) obj;
                                coreApi.createNamespacedService("kube-system", service, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "\n---";
                        }
                        if (type.equals("V1Deployment")) {
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/department", departmentName);
                                nodeSelectors.put("robolaunch.io/cloud-instance", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/buffer-instance", bufferName);
                                nodeSelectors.put("robolaunch.io/super-cluster", superClusterName);

                                V1Deployment deployment = (V1Deployment) obj;
                                deployment.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectors);
                                appsV1Api.createNamespacedDeployment("kube-system", deployment, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());

                        }
                }
                Artifact artifact2 = new Artifact();
                artifact2.setClusterName(cloudInstanceName);
                artifact2.setName("coreDNS.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact2, organization.getName());
        }

        @Override
        public void addLabelsToVirtualClusterNode(Organization organization, String nodeName, String cloudInstanceName,
                        String departmentName, String bufferName, String superClusterName, Boolean connectionHub)
                        throws KubectlException, IOException, ApiException, InterruptedException {
                System.out.println("Node name: " + nodeName);
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                if (!connectionHub) {
                        Kubectl.label(V1Node.class).apiClient(vcClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/buffer-instance", bufferName)
                                        .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                        .addLabel("robolaunch.io/department", departmentName)
                                        .addLabel("robolaunch.io/super-cluster",
                                                        superClusterName)
                                        .execute();
                } else {
                        Kubectl.label(V1Node.class).apiClient(vcClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/buffer-instance", bufferName)
                                        .addLabel("robolaunch.io/cloud-instance", cloudInstanceName)
                                        .addLabel("robolaunch.io/department", departmentName)
                                        .addLabel("robolaunch.io/super-cluster",
                                                        superClusterName)
                                        .addLabel("submariner.io/gateway", "4490")
                                        .execute();
                }

        }

        @Override
        public void createCertManager(Organization organization, String departmentName,
                        String cloudInstanceName, String bufferName, String superClusterName)
                        throws KubectlException, IOException, ApiException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("certManager.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);
                CoreV1Api coreApi = new CoreV1Api(vcClient);
                AppsV1Api appsApi = new AppsV1Api(vcClient);
                RbacAuthorizationV1Api rbacApi = new RbacAuthorizationV1Api(vcClient);
                AdmissionregistrationV1Api admissionApi = new AdmissionregistrationV1Api(vcClient);
                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();
                        if (type.equals("V1Namespace")) {
                                V1Namespace namespace = (V1Namespace) obj;
                                coreApi.createNamespace(namespace, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1CustomResourceDefinition")) {
                                V1CustomResourceDefinition customResource = (V1CustomResourceDefinition) obj;
                                Kubectl.apply(V1CustomResourceDefinition.class).forceConflict(true)
                                                .resource(customResource).apiClient(vcClient).execute();
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ServiceAccount")) {
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                coreApi.createNamespacedServiceAccount("cert-manager", serviceAccount, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ConfigMap")) {
                                V1ConfigMap configMap = (V1ConfigMap) obj;
                                coreApi.createNamespacedConfigMap("cert-manager", configMap, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ClusterRole")) {
                                V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                rbacApi.createClusterRole(clusterRole, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1ClusterRoleBinding")) {
                                V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                rbacApi.createClusterRoleBinding(clusterRoleBinding, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1Role")) {
                                V1Role role = (V1Role) obj;
                                rbacApi.createNamespacedRole(role.getMetadata().getNamespace(), role, null, null, null,
                                                null);

                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1RoleBinding")) {
                                V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                rbacApi.createNamespacedRoleBinding(roleBinding.getMetadata().getNamespace(),
                                                roleBinding,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1Service")) {
                                V1Service service = (V1Service) obj;
                                coreApi.createNamespacedService("cert-manager", service, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1Deployment")) {
                                V1Deployment deployment = (V1Deployment) obj;
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/cloud-instance", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/department", departmentName);
                                nodeSelectors.put("robolaunch.io/super-cluster", superClusterName);
                                nodeSelectors.put("robolaunch.io/buffer-instance", bufferName);

                                deployment.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectors);
                                appsApi.createNamespacedDeployment(
                                                "cert-manager", deployment,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1MutatingWebhookConfiguration")) {
                                V1MutatingWebhookConfiguration mutatingWebhookConf = (V1MutatingWebhookConfiguration) obj;
                                admissionApi.createMutatingWebhookConfiguration(mutatingWebhookConf, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1ValidatingWebhookConfiguration")) {
                                V1ValidatingWebhookConfiguration validatingWebhookConf = (V1ValidatingWebhookConfiguration) obj;
                                admissionApi.createValidatingWebhookConfiguration(validatingWebhookConf, null, null,
                                                null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                }
                Artifact artifact2 = new Artifact();
                artifact2.setClusterName(cloudInstanceName);
                artifact2.setName("certManager.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact2, organization.getName());
        }

        @Override
        public void createConnectionHubOperator(String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName,
                        String bufferName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException,
                        KubectlException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("connectionHubOperator.yaml");
                String bucket = "template-artifacts";
                Artifact artifact2 = new Artifact();
                artifact2.setName("certificateConnectionHub.yaml");
                JsonObject object = storageRepository.getYamlTemplate(artifact2, bucket);
                Artifact artifact4 = new Artifact();
                artifact4.setName("issuerConnectionHub.yaml");
                JsonObject objectIssuer = storageRepository.getYamlTemplate(artifact4, bucket);
                ModelMapper.addModelMap("cert-manager.io", "v1", "Certificate", "certificates",
                                V1alpha2Certificate.class,
                                V1alpha2CertificateList.class);
                ModelMapper.addModelMap("cert-manager.io", "v1", "Issuer", "issuers",
                                V1alpha2Issuer.class,
                                V1alpha2IssuerList.class);
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);
                CoreV1Api coreApi = new CoreV1Api(vcClient);
                AppsV1Api appsApi = new AppsV1Api(vcClient);
                RbacAuthorizationV1Api rbacApi = new RbacAuthorizationV1Api(vcClient);
                AdmissionregistrationV1Api admissionApi = new AdmissionregistrationV1Api(vcClient);
                CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcClient);
                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();
                        if (type.equals("V1Namespace")) {
                                V1Namespace namespace = (V1Namespace) obj;
                                coreApi.createNamespace(namespace, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1CustomResourceDefinition")) {
                                V1CustomResourceDefinition customResource = (V1CustomResourceDefinition) obj;
                                Kubectl.apply(V1CustomResourceDefinition.class).forceConflict(true)
                                                .resource(customResource).apiClient(vcClient).execute();
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ServiceAccount")) {
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                System.out.println("creating svca");
                                coreApi.createNamespacedServiceAccount("connection-hub-system", serviceAccount, null,
                                                null, null,
                                                null);
                                System.out.println("svca created");
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ClusterRole")) {
                                V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                rbacApi.createClusterRole(clusterRole, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ClusterRoleBinding")) {
                                V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                rbacApi.createClusterRoleBinding(clusterRoleBinding, null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Role")) {
                                V1Role role = (V1Role) obj;
                                rbacApi.createNamespacedRole(role.getMetadata().getNamespace(), role, null, null, null,
                                                null);

                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1RoleBinding")) {
                                V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                rbacApi.createNamespacedRoleBinding(roleBinding.getMetadata().getNamespace(),
                                                roleBinding,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Service")) {
                                V1Service service = (V1Service) obj;
                                coreApi.createNamespacedService("connection-hub-system", service, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1Deployment")) {
                                System.out.println("creating dep");

                                V1Deployment deployment = (V1Deployment) obj;
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/cloud-instance", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/department", departmentName);

                                deployment.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectors);
                                appsApi.createNamespacedDeployment(
                                                "connection-hub-system", deployment,
                                                null, null, null, null);
                                System.out.println("dep created");
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1alpha2Certificate")) {

                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "connection-hub-system", "certificates", object,
                                                null, null, null);

                        }
                        if (type.equals("V1alpha2Issuer")) {
                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "connection-hub-system", "issuers",
                                                objectIssuer,
                                                null, null, null);

                        }
                        if (type.equals("V1MutatingWebhookConfiguration")) {
                                V1MutatingWebhookConfiguration mutatingWebhookConf = (V1MutatingWebhookConfiguration) obj;
                                admissionApi.createMutatingWebhookConfiguration(mutatingWebhookConf, null, null, null,
                                                null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1ValidatingWebhookConfiguration")) {
                                V1ValidatingWebhookConfiguration validatingWebhookConf = (V1ValidatingWebhookConfiguration) obj;
                                admissionApi.createValidatingWebhookConfiguration(validatingWebhookConf, null, null,
                                                null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                }

        }

        @Override
        public void createConnectionHub(String bufferName, Organization organization, String departmentName,
                        String cloudInstanceName, String serverIP, String namespaceName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("connectionHubCloudInstance.yaml");
                String bucket = "template-artifacts";
                DynamicKubernetesApi connectionHubApi = new DynamicKubernetesApi(
                                "connection-hub.roboscale.io", "v1alpha1",
                                "connectionhubs",
                                vcClient);
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                Map<String, String> labels = new HashMap<>();
                labels.put("robolaunch.io/cloud-instance", cloudInstanceName);
                var subnet = subnetsApi.get("subnet-" + namespaceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance",
                                cloudInstanceName);
                object.get("spec").getAsJsonObject().get("federationSpec").getAsJsonObject().get("helmRepository")
                                .getAsJsonObject()
                                .addProperty("url", robolaunchHelmRepo);

                object.get("spec").getAsJsonObject().get("submarinerSpec").getAsJsonObject().get("helmRepository")
                                .getAsJsonObject().addProperty("url", robolaunchHelmRepo);
                object.get("spec").getAsJsonObject().get("submarinerSpec").getAsJsonObject().addProperty("apiServerURL",
                                serverIP);
                object.get("spec").getAsJsonObject().get("submarinerSpec").getAsJsonObject().addProperty("clusterCIDR",
                                subnet.getObject().getRaw().getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("cidrBlock").getAsString());
                connectionHubApi.create(new DynamicKubernetesObject(object));

        }

        @Override
        public void createRobotOperator(Organization organization, String cloudInstanceName,
                        String departmentName, String superClusterName, String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        MinioException, ApiException, KubectlException, InterruptedException {
                try {
                        ApiClient vcClient = cloudInstanceHelperRepository
                                        .getVirtualClusterClientWithBufferName(bufferName);
                        String yamlString = "";
                        Artifact artifact = new Artifact();
                        artifact.setName("robotOperator.yaml");
                        String bucket = "template-artifacts";
                        String yaml = storageRepository.getContent(artifact, bucket);
                        ModelMapper.addModelMap("cert-manager.io", "v1", "Certificate", "certificates",
                                        V1alpha2Certificate.class,
                                        V1alpha2CertificateList.class);
                        ModelMapper.addModelMap("cert-manager.io", "v1", "Issuer", "issuers",
                                        V1alpha2Issuer.class,
                                        V1alpha2IssuerList.class);

                        Artifact artifact2 = new Artifact();
                        artifact2.setName("certificate.yaml");
                        JsonObject object = storageRepository.getYamlTemplate(artifact2, bucket);

                        Artifact artifact4 = new Artifact();
                        artifact4.setName("issuer.yaml");
                        JsonObject objectIssuer = storageRepository.getYamlTemplate(artifact4, bucket);
                        List<Object> list = Yaml.loadAll(yaml);
                        CoreV1Api coreApi = new CoreV1Api(vcClient);
                        RbacAuthorizationV1Api rbacApi = new RbacAuthorizationV1Api(vcClient);
                        AdmissionregistrationV1Api admissionApi = new AdmissionregistrationV1Api(
                                        vcClient);

                        CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcClient);
                        AppsV1Api appsApi = new AppsV1Api(vcClient);
                        for (int i = 0; i < list.size(); i++) {
                                Object obj = list.get(i);
                                String type = obj.getClass().getSimpleName();

                                if (type.equals("V1Namespace")) {
                                        V1Namespace namespace = (V1Namespace) obj;
                                        coreApi.createNamespace(namespace, null, null, null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }

                                if (type.equals("V1CustomResourceDefinition")) {
                                        V1CustomResourceDefinition crd = (V1CustomResourceDefinition) obj;
                                        Kubectl.apply(V1CustomResourceDefinition.class)
                                                        .forceConflict(true)
                                                        .resource(crd).apiClient(vcClient).execute();
                                }

                                if (type.equals("V1ServiceAccount")) {
                                        V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                        coreApi.createNamespacedServiceAccount("robot-system",
                                                        serviceAccount, null,
                                                        null, null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                                if (type.equals("V1ConfigMap")) {
                                        V1ConfigMap configMap = (V1ConfigMap) obj;
                                        coreApi.createNamespacedConfigMap("robot-system", configMap,
                                                        null, null,
                                                        null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                                if (type.equals("V1ClusterRole")) {
                                        V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                        rbacApi.createClusterRole(clusterRole, null, null, null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }

                                if (type.equals("V1ClusterRoleBinding")) {
                                        V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                        rbacApi.createClusterRoleBinding(clusterRoleBinding, null, null,
                                                        null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                                if (type.equals("V1Role")) {
                                        V1Role role = (V1Role) obj;
                                        rbacApi.createNamespacedRole("robot-system", role, null, null,
                                                        null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }

                                if (type.equals("V1RoleBinding")) {
                                        V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                        rbacApi.createNamespacedRoleBinding("robot-system", roleBinding,
                                                        null, null,
                                                        null, null);

                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";

                                }

                                if (type.equals("V1Service")) {
                                        V1Service service = (V1Service) obj;
                                        coreApi.createNamespacedService("robot-system", service, null,
                                                        null, null,
                                                        null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                                if (type.equals("V1Deployment")) {
                                        V1Deployment deployment = (V1Deployment) obj;
                                        deployment.getSpec().getTemplate().getSpec().getNodeSelector()
                                                        .put(
                                                                        "robolaunch.io/organization",
                                                                        organization.getName());
                                        deployment.getSpec().getTemplate().getSpec().getNodeSelector()
                                                        .put(
                                                                        "robolaunch.io/cloud-instance",
                                                                        cloudInstanceName);
                                        deployment.getSpec().getTemplate().getSpec().getNodeSelector()
                                                        .put(
                                                                        "robolaunch.io/department",
                                                                        departmentName);
                                        deployment.getSpec().getTemplate().getSpec().getNodeSelector()
                                                        .put(
                                                                        "robolaunch.io/super-cluster",
                                                                        superClusterName);
                                        appsApi.createNamespacedDeployment("robot-system", deployment,
                                                        null, null,
                                                        null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                                if (type.equals("V1alpha2Certificate")) {
                                        customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                        "v1",
                                                        "robot-system", "certificates", object,
                                                        null, null, null);

                                }

                                if (type.equals("V1alpha2Issuer")) {
                                        customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                        "v1",
                                                        "robot-system", "issuers",
                                                        objectIssuer,
                                                        null, null, null);

                                }

                                if (type.equals("V1MutatingWebhookConfiguration")) {
                                        V1MutatingWebhookConfiguration mutatingWebhookConf = (V1MutatingWebhookConfiguration) obj;
                                        admissionApi.createMutatingWebhookConfiguration(
                                                        mutatingWebhookConf, null,
                                                        null, null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }

                                if (type.equals("V1ValidatingWebhookConfiguration")) {
                                        V1ValidatingWebhookConfiguration validatingWebhookConf = (V1ValidatingWebhookConfiguration) obj;
                                        admissionApi.createValidatingWebhookConfiguration(
                                                        validatingWebhookConf,
                                                        null, null, null, null);
                                        String jsonInString = new Gson().toJson(obj);
                                        JSONObject mJSONObject = new JSONObject(jsonInString);
                                        yamlString += cloudInstanceHelperRepository
                                                        .convertJsonStringToYamlString(mJSONObject.toString());
                                        yamlString += "---";
                                }
                        }

                        Artifact artifact3 = new Artifact();
                        artifact3.setClusterName(cloudInstanceName);
                        artifact3.setName("robotOperator.yaml");
                        storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact3,
                                        organization.getName());
                } catch (ApiException e) {
                        System.out.println(e.getResponseBody());
                        System.out.println(e.getCode());
                }

        }

        @Override
        public void createDNSRecord(Organization organization, String nodeName)
                        throws ApiException, InternalError, ApplicationException, IOException {
                /* Get ExternalIP from node */
                String externalIP = "";
                var node = coreV1Api.readNode(nodeName, null);
                var addresses = node.getStatus().getAddresses();
                for (int i = 0; i < addresses.size(); i++) {
                        if (addresses.get(i).getType().equals(
                                        "ExternalIP")) {
                                externalIP = addresses.get(i).getAddress().toString();
                        }
                }
                String requestData = groupAdapter.toCreateDNSRecord(organization, externalIP, dnsZoneName);
                String createDNSRecord = String.format("{\"id\": 0, \"method\": \"dnsrecord_add/1\", \"params\": %s} ",
                                requestData);
                groupAdminRepository.makeRequest(createDNSRecord);

        }

        @Override
        public void addBufferedLabelToVC(String bufferName, String instanceType) throws KubectlException, IOException {
                ApiClient apiClient = Config.defaultClient();
                ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
                                "virtualclusters", true,
                                V1VirtualCluster.class);

                Kubectl.label(V1VirtualCluster.class).apiClient(apiClient).name("vc-" + bufferName).namespace("default")
                                .addLabel("buffered", "true")
                                .execute();

                Kubectl.label(V1VirtualCluster.class).apiClient(apiClient).name("vc-" + bufferName).namespace("default")
                                .addLabel("robolaunch.io/instance-type", instanceType)
                                .execute();

        }

        @Override
        public void scaleVCWorkloadsDown(String bufferName)
                        throws IOException, ApiException, InterruptedException, KubectlException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                AppsV1Api appsV1Api = new AppsV1Api(vcClient);
                V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null,
                                null, null, null, null, null);
                for (V1Deployment deployment : deploymentList.getItems()) {
                        if (deployment.getMetadata().getNamespace().equals("kube-system")
                                        && deployment.getMetadata().getName().equals("coredns")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(0).execute();
                        }
                        if (deployment.getMetadata().getNamespace().equals("cert-manager")
                                        && deployment.getMetadata().getName().equals("cert-manager")
                                        || deployment
                                                        .getMetadata().getName().equals("cert-manager-cainjector")
                                        || deployment
                                                        .getMetadata().getName().equals("cert-manager-webhook")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(0).execute();
                        }
                        if (deployment.getMetadata().getNamespace().equals("robot-system")
                                        && deployment.getMetadata().getName().equals("robot-controller-manager")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(0).execute();
                        }
                }
        }

        @Override
        public void scaleVCWorkloadsUp(String bufferName)
                        throws IOException, ApiException, InterruptedException, KubectlException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                AppsV1Api appsV1Api = new AppsV1Api(vcClient);
                V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null,
                                null, null, null, null, null);
                for (V1Deployment deployment : deploymentList.getItems()) {
                        if (deployment.getMetadata().getNamespace().equals("kube-system")
                                        && deployment.getMetadata().getName().equals("coredns")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(1).execute();
                        }
                        if (deployment.getMetadata().getNamespace().equals("cert-manager")
                                        && deployment.getMetadata().getName().equals("cert-manager")
                                        || deployment
                                                        .getMetadata().getName().equals("cert-manager-cainjector")
                                        || deployment
                                                        .getMetadata().getName().equals("cert-manager-webhook")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(1).execute();
                        }
                        if (deployment.getMetadata().getNamespace().equals("robot-system")
                                        && deployment.getMetadata().getName().equals("robot-controller-manager")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deployment.getMetadata().getName()).replicas(1).execute();
                        }
                }
        }

        @Override
        public void scaleOAuth2ProxyDown(String bufferName) throws ApiException, IOException, KubectlException {
                try {
                        String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
                        ApiClient apiClient = Config.defaultClient();
                        Kubectl.scale(V1Deployment.class).apiClient(apiClient)
                                        .namespace(namespaceName + "-oauth2-proxy")
                                        .name("oauth2-proxy").replicas(0).execute();
                } catch (KubectlException e) {
                        System.out.println(e.getCause());
                        System.out.println(e.getMessage());
                }

        }

        @Override
        public void scaleOAuth2ProxyUp(String bufferName) throws ApiException, IOException, KubectlException {
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
                ApiClient apiClient = Config.defaultClient();
                Kubectl.scale(V1Deployment.class).apiClient(apiClient).namespace(namespaceName + "-oauth2-proxy")
                                .name("oauth2-proxy").replicas(1).execute();
        }

        @Override
        public void scaleCoreDNSUp(String bufferName) throws ApiException, IOException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                try {
                        Kubectl.scale(V1Deployment.class).apiClient(vcClient).namespace("kube-system")
                                        .name("coredns").replicas(1).execute();
                } catch (KubectlException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public void unlabelSuperClusterNode(String nodeName) throws IOException, KubectlException {
                ApiClient apiClient = Config.defaultClient();
                String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1buffer-instance\" }]";
                V1Patch patch = new V1Patch(patchString);
                Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
        }

        @Override
        public void createClusterAdminRole(Organization organization, String bufferName, String username)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, ApiException, InterruptedException {
                Artifact artifact = new Artifact();
                artifact.setName("clusterAdminRole.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);

                ApiClient apiClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);

                RbacAuthorizationV1Api rbacAuthorizationV1Api = new RbacAuthorizationV1Api(apiClient);

                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();
                        if (type.equals("V1ClusterRole")) {
                                V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                rbacAuthorizationV1Api.createClusterRole(clusterRole, null, null, null, null);
                        }
                        if (type.equals("V1ClusterRoleBinding")) {
                                V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                System.out.println("Username: " + username);
                                clusterRoleBinding.getSubjects().get(0).setName(username);
                                rbacAuthorizationV1Api.createClusterRoleBinding(clusterRoleBinding, null, null, null,
                                                null);
                        }
                }
        }
}
