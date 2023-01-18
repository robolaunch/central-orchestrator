package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
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
import org.robolaunch.service.ApiClientManager;
import org.robolaunch.service.KubernetesService;

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
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressRule;
import io.kubernetes.client.openapi.models.V1IngressSpec;
import io.kubernetes.client.openapi.models.V1IngressTLS;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1MutatingWebhookConfiguration;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1PolicyRule;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1Subject;
import io.kubernetes.client.openapi.models.V1ValidatingWebhookConfiguration;
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
        @Inject
        ApiClientManager apiClientManager;
        @Inject
        KubernetesService kubernetesService;

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

        @Override
        public void createMachineDeployment(String bufferName, String instanceType, String provider, String region,
                        String superCluster) {
                DynamicKubernetesApi machineDeploymentApi;
                try {
                        machineDeploymentApi = apiClientManager.getMachineDeploymentApi(provider, region,
                                        superCluster);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | ApiException | InterruptedException | MinioException e) {
                        throw new ApplicationException("Error creating machine deployment api.");
                }
                Artifact artifact = new Artifact();

                artifact.setName(provider + "/" + region + "/" + superCluster + "/" + "machineDeployment.yaml");
                String bucket = "providers";
                JsonObject object;
                try {
                        object = storageRepository.getYamlTemplate(artifact, bucket);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | MinioException
                                | IOException e) {
                        throw new ApplicationException("Error getting machine deployment template.");
                }
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "md-" + bufferName);

                object.get("spec").getAsJsonObject().get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("providerSpec").getAsJsonObject().get("value").getAsJsonObject()
                                .get("cloudProviderSpec").getAsJsonObject()
                                .addProperty("instanceType", instanceType);

                object.get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/cloud-instance", bufferName);

                machineDeploymentApi.create(new DynamicKubernetesObject(object));
        }

        @Override
        public void claimTheSuperClusterNode(String nodeName, String bufferName, String provider, String region,
                        String superCluster) {
                ApiClient adminApiClient;
                try {
                        adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | ApiException | InterruptedException | MinioException e) {
                        throw new ApplicationException("Error creating admin api client.");
                }
                try {
                        Kubectl.label(V1Node.class).apiClient(adminApiClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("robolaunch.io/cloud-instance", bufferName)
                                        .execute();
                        Kubectl.label(V1Node.class).apiClient(adminApiClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("node-role.kubernetes.io/worker", "worker")
                                        .execute();
                } catch (KubectlException e) {
                        throw new ApplicationException(
                                        "Error claiming the super cluster node. Kubectl level: " + e.getMessage());
                }

        }

        @Override
        public void createClusterVersion(String bufferName, String provider, String region, String superCluster) {
                DynamicKubernetesApi clusterVersionApi;
                try {
                        clusterVersionApi = apiClientManager.getClusterVersionApi(provider, region,
                                        superCluster);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | ApiException | InterruptedException | MinioException e) {
                        throw new ApplicationException("Error creating cluster version api." + e.getMessage());
                }
                Artifact artifact = new Artifact();
                artifact.setName("clusterVersion.yaml");
                String bucket = "template-artifacts";
                JsonObject object;
                try {
                        object = storageRepository.getYamlTemplate(artifact, bucket);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | MinioException
                                | IOException e) {
                        throw new ApplicationException("Error getting cluster version template. " + e.getMessage());
                }
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "cv-" + bufferName);
                object.get("spec").getAsJsonObject().get("etcd").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/cloud-instance", bufferName);
                object.get("spec").getAsJsonObject().get("apiServer").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/cloud-instance", bufferName);
                object.get("spec").getAsJsonObject().get("controllerManager").getAsJsonObject().get("statefulset")
                                .getAsJsonObject().get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec")
                                .getAsJsonObject()
                                .get("nodeSelector").getAsJsonObject()
                                .addProperty("robolaunch.io/cloud-instance", bufferName);
                object.get("spec").getAsJsonObject().get("apiServer")
                                .getAsJsonObject().get("statefulset").getAsJsonObject()
                                .get("spec").getAsJsonObject().get("template").getAsJsonObject()
                                .get("spec").getAsJsonObject()
                                .get("containers").getAsJsonArray()
                                .get(0).getAsJsonObject().get("args").getAsJsonArray()
                                .add("--oidc-issuer-url=" + keycloakURL + "/realms/buffer-realm");
                try {
                        clusterVersionApi.create(new DynamicKubernetesObject(object)).throwsApiException();
                } catch (ApiException e) {
                        throw new ApplicationException(
                                        "Error creating cluster version. " + e.getResponseBody() + " " + e.getCode());
                }
        }

        @Override
        public void createVirtualCluster(String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException, InterruptedException {
                DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region,
                                superCluster);
                Artifact artifact = new Artifact();
                artifact.setName("virtualCluster.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
                object.get("metadata").getAsJsonObject().addProperty("name",
                                bufferName);
                object.get("spec").getAsJsonObject().addProperty("clusterDomain",
                                bufferName + ".local");
                object.get("spec").getAsJsonObject().addProperty("clusterVersionName", "cv-" + bufferName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject()
                                .addProperty("robolaunch.io/cloud-instance", bufferName);
                virtualClustersApi.create(new DynamicKubernetesObject(object)).throwsApiException();
        }

        @Override
        public void scaleStatefulSetsDown(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, KubectlException, IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException {
                ApiClient adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName,
                                provider, region, superCluster);

                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName).name("etcd")
                                .replicas(0).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName)
                                .name("controller-manager")
                                .replicas(0).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName).name("apiserver")
                                .replicas(0).execute();
        }

        @Override
        public void drainNode(String nodeName, String provider, String region, String superCluster)
                        throws IOException, KubectlException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException {
                ApiClient adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);

                try {
                        Kubectl.drain().ignoreDaemonSets().gracePeriod(60).name(nodeName).apiClient(adminApiClient)
                                        .execute();
                } catch (KubectlException e) {
                }

        }

        @Override
        public void uncordonNode(String nodeName, String provider, String region, String superCluster)
                        throws IOException, KubectlException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException {
                ApiClient adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                try {
                        Kubectl.uncordon().name(nodeName).apiClient(adminApiClient).execute();
                } catch (Exception e) {
                        System.out.println("uncordon: " + e.getMessage());
                }
        }

        @Override
        public void labelVirtualCluster(String bufferName, Organization organization, String teamId,
                        String cloudInstanceName, Boolean connectionHub, String provider, String region,
                        String superCluster)
                        throws IOException, KubectlException, InterruptedException, ApiException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
                                "virtualclusters", true,
                                V1VirtualCluster.class);
                ApiClient client = cloudInstanceHelperRepository.adminApiClient(provider, region, superCluster);
                if (!connectionHub) {
                        Kubectl.label(V1VirtualCluster.class).apiClient(client)
                                        .skipDiscovery()
                                        .namespace("default")
                                        .name(bufferName)
                                        .addLabel("robolaunch.io/cloud-instance", bufferName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region", region)
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .execute();
                } else {
                        Kubectl.label(V1VirtualCluster.class).apiClient(client)
                                        .skipDiscovery()
                                        .namespace("default")
                                        .name(bufferName)
                                        .addLabel("robolaunch.io/cloud-instance", bufferName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region", region)
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .addLabel("submariner.io/gateway", "4490")
                                        .execute();
                }

        }

        @Override
        public void addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
                        String teamId, Boolean connectionHub, String provider, String region, String superCluster)
                        throws KubectlException, IOException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException {
                ApiClient adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);

                if (!connectionHub) {
                        Kubectl.label(V1Node.class).apiClient(adminApiClient)
                                        .name(nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region", region)
                                        .execute();
                } else {
                        Kubectl.label(V1Node.class).apiClient(adminApiClient)
                                        .name(nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region", region)
                                        .addLabel("submariner.io/gateway", "4490")
                                        .execute();
                }
        }

        @Override
        public void addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
                        String teamId,
                        String cloudInstanceName, String bufferName, String provider, String region,
                        String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException {
                AppsV1Api appsApi = apiClientManager.getAppsApi(provider, region, superCluster);
                V1StatefulSetList statefulsets = appsApi.listNamespacedStatefulSet(
                                namespaceName, null, null, null,
                                null, null, null, null, null, null,
                                null);
                for (V1StatefulSet statefulset : statefulsets.getItems()) {
                        String patchString = "[{\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1organization\",\"value\": \""
                                        + organization.getName()
                                        + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1team\",\"value\": \""
                                        + teamId
                                        + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1cloud-instance-alias\",\"value\": \""
                                        + cloudInstanceName
                                        + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1region\",\"value\": \""
                                        + region
                                        + "\"}, {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector/robolaunch.io~1cloud-instance\",\"value\": \""
                                        + bufferName + "\"}]";
                        Optional<String> statefulSetName = Optional.ofNullable(statefulset)
                                        .map(V1StatefulSet::getMetadata)
                                        .map(V1ObjectMeta::getName);
                        if (statefulSetName.get().equals("apiserver")) {
                                String replacePatch = "[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/args/37\",\"value\": \""
                                                + "--oidc-issuer-url=" + keycloakURL + "/realms/"
                                                + organization.getName()
                                                + "\"}]";

                                appsApi.patchNamespacedStatefulSet(statefulSetName.get(),
                                                namespaceName,
                                                new V1Patch(replacePatch),
                                                null, null,
                                                null, null, null);
                        }

                        appsApi.patchNamespacedStatefulSet(statefulSetName.get(), namespaceName,
                                        new V1Patch(patchString),
                                        null, null,
                                        null, null, null);

                        appsApi.patchNamespacedStatefulSet(statefulSetName.get(), namespaceName,
                                        new V1Patch(patchString),
                                        null, null,
                                        null, null, null);

                        appsApi.patchNamespacedStatefulSet(statefulSetName.get(), namespaceName,
                                        new V1Patch(patchString),
                                        null, null,
                                        null, null, null);
                }

        }

        @Override
        public void scaleStatefulSetsUp(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException {
                ApiClient adminApiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName,
                                provider, region, superCluster);

                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName).name("etcd")
                                .replicas(1).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName)
                                .name("controller-manager")
                                .replicas(1).execute();
                Kubectl.scale(V1StatefulSet.class).apiClient(adminApiClient).namespace(namespaceName).name("apiserver")
                                .replicas(1).execute();
        }

        /* Create subnet for namespace */
        @Override
        public void createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                DynamicKubernetesApi subnetsApi = apiClientManager.getSubnetApi(provider, region, superCluster);
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
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-submariner-k8s-broker");
                object.get("spec").getAsJsonObject().get("namespaces").getAsJsonArray()
                                .add(namespaceName + "-connection-hub-system");
                object.get("metadata").getAsJsonObject().addProperty("name",
                                "subnet-" + namespaceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance-alias",
                                cloudInstanceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/team",
                                teamId);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/organization",
                                organization.getName());
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/region",
                                region);
                String mySubnetIP = cloudInstanceHelperRepository.getAvailableCIDRBlock(provider, region, superCluster);
                String blockId = mySubnetIP.split("\\.")[2];
                object.get("spec").getAsJsonObject().addProperty("cidrBlock", mySubnetIP);
                object.get("spec").getAsJsonObject().get("excludeIps").getAsJsonArray().remove(0);
                object.get("spec").getAsJsonObject().get("excludeIps").getAsJsonArray()
                                .add("10.10." + blockId + ".1");
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
                        String teamId, Organization organization, String bufferName, String provider, String region,
                        String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                DynamicKubernetesApi subnetsApi = apiClientManager.getSubnetApi(provider, region, superCluster);
                DynamicKubernetesApi machineDeploymentApi = apiClientManager.getMachineDeploymentApi(provider, region,
                                superCluster);
                Artifact artifact = new Artifact();
                artifact.setName("virtualLink.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                Object obj = Yaml.load(yaml);
                V1Job job = (V1Job) obj;
                BatchV1Api batchV1Api = new BatchV1Api(vcClient);
                Optional<V1ObjectMeta> metadata = Optional.ofNullable(job.getMetadata());
                metadata.get().setNamespace("default");
                Map<String, String> nodeSelectors = new HashMap<>();
                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                nodeSelectors.put("robolaunch.io/cloud-instance-alias", cloudInstanceName);
                nodeSelectors.put("robolaunch.io/team", teamId);
                nodeSelectors.put("robolaunch.io/region", region);
                Optional<V1PodSpec> podSpec = Optional.ofNullable(job).map(V1Job::getSpec).map(m -> m.getTemplate())
                                .map(m -> m.getSpec());
                podSpec.get().setNodeSelector(nodeSelectors);
                var subnet = subnetsApi.get("subnet-" + namespaceName);
                var machineDeployment = machineDeploymentApi.get("kube-system", "md-" + bufferName);
                String subnetId = machineDeployment.getObject().getRaw().getAsJsonObject().get("spec").getAsJsonObject()
                                .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("providerSpec").getAsJsonObject().get("value")
                                .getAsJsonObject().get("cloudProviderSpec").getAsJsonObject().get("subnetId")
                                .getAsString().substring(0, 9);
                List<String> args = new ArrayList<String>();
                args.add("ip link add "
                                + subnetId
                                + " type dummy; ip addr add "
                                + subnet.getObject().getRaw().get("spec").getAsJsonObject()
                                                .get("cidrBlock").getAsString()
                                                .substring(0, subnet.getObject().getRaw().get("spec").getAsJsonObject()
                                                                .get("cidrBlock").getAsString().lastIndexOf("/"))
                                + "/32 brd + dev " + subnetId
                                + " label " + subnetId + ":0; ip link set dev " + subnetId + " up;");
                podSpec.get().getContainers().get(0).setArgs(args);

                batchV1Api.createNamespacedJob("default", job, null, null, null, null);
        }

        @Override
        public void createOauth2ProxyNamespace(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                CoreV1Api coreV1Api = new CoreV1Api(vcClient);
                V1Namespace namespace = new V1Namespace();
                namespace.setApiVersion("v1");
                namespace.setKind("Namespace");
                namespace.setMetadata(new V1ObjectMeta().name("oauth2-proxy"));
                coreV1Api.createNamespace(namespace, null, null, null, null);
        }

        @Override
        public void createTLSSecrets(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
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
        public void createOAuth2ProxyResources(Organization organization, String teamId,
                        String cloudInstanceName, String namespaceName, String bufferName, String provider,
                        String region, String superCluster) {
                CoreV1Api coreV1Api;
                AppsV1Api appsApi;
                try {
                        coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
                        appsApi = apiClientManager.getAppsApi(provider, region, superCluster);

                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | ApiException | InterruptedException | MinioException e) {
                        throw new ApplicationException("Error creating apis.");
                }
                Artifact artifact = new Artifact();
                artifact.setName("oauth2Proxy.yaml");
                String bucket = "template-artifacts";
                String yaml;
                List<Object> list;
                try {
                        yaml = storageRepository.getContent(artifact, bucket);
                        list = Yaml.loadAll(yaml);

                } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                                | InvalidResponseException | NoSuchAlgorithmException | ServerException
                                | XmlParserException | IllegalArgumentException | IOException e) {
                        throw new ApplicationException("Error getting yaml file.");
                }
                String newNamespace = namespaceName + "-oauth2-proxy";
                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();

                        if (type.equals("V1ServiceAccount")) {
                                try {
                                        Thread.sleep(14000);
                                } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                }
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                try {
                                        coreV1Api.createNamespacedServiceAccount(newNamespace, serviceAccount,
                                                        null, null, null, null);
                                } catch (ApiException e) {
                                        throw new ApplicationException("Error creating service account.");
                                }
                        }
                        if (type.equals("V1Secret")) {
                                V1Secret secret = (V1Secret) obj;
                                Optional<Map<String, byte[]>> secretData = Optional.ofNullable(secret.getData());
                                secretData.get().put("cookie-secret",
                                                openSSLString.getBytes(StandardCharsets.UTF_8));
                                secretData.get().put("client-id",
                                                oauthClientId.getBytes(StandardCharsets.UTF_8));
                                secretData.get().put("client-secret",
                                                keycloakAdminRepository
                                                                .getClientSecret(organization.getName(), oauthClientId)
                                                                .getBytes(StandardCharsets.UTF_8));
                                try {
                                        coreV1Api.createNamespacedSecret(newNamespace, secret, null, null, null, null);
                                } catch (ApiException e) {
                                        throw new ApplicationException("Error creating secret.");
                                }
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
                                try {
                                        coreV1Api.createNamespacedConfigMap(
                                                        newNamespace, configMap, null, null, null,
                                                        null);
                                } catch (ApiException e) {
                                        throw new ApplicationException("Error creating config map.");
                                }

                        }
                        if (type.equals("V1Service")) {
                                V1Service service = (V1Service) obj;
                                try {
                                        coreV1Api.createNamespacedService(newNamespace, service, null, null, null,
                                                        null);
                                } catch (ApiException e) {
                                        throw new ApplicationException("Error creating service.");
                                }
                        }
                        if (type.equals("V1Deployment")) {
                                V1Deployment deployment = (V1Deployment) obj;
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/cloud-instance-alias", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/team", teamId);
                                nodeSelectors.put("robolaunch.io/region", region);
                                nodeSelectors.put("robolaunch.io/cloud-instance", bufferName);
                                Optional<V1PodSpec> podSpec = Optional.ofNullable(deployment.getSpec())
                                                .map(V1DeploymentSpec::getTemplate)
                                                .map(V1PodTemplateSpec::getSpec);
                                podSpec.get().setNodeSelector(nodeSelectors);

                                try {
                                        appsApi.createNamespacedDeployment(newNamespace, deployment, null, null, null,
                                                        null);
                                } catch (ApiException e) {
                                        throw new ApplicationException("Error creating deployment.");
                                }

                        }

                }

        }

        @Override
        public void createIngressRule(Organization organization, String cloudInstanceName,
                        String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, MinioException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                String yamlString = "";
                Artifact artifact = new Artifact();
                artifact.setName("ingress.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                NetworkingV1Api networkingV1Api = new NetworkingV1Api(vcClient);
                Object obj = Yaml.load(yaml);
                V1Ingress ingress = (V1Ingress) obj;
                Optional<V1ObjectMeta> metadata = Optional.ofNullable(ingress.getMetadata());
                Optional<List<V1IngressTLS>> tls = Optional.ofNullable(ingress).map(V1Ingress::getSpec)
                                .map(V1IngressSpec::getTls);
                Optional<List<V1IngressRule>> rules = Optional.ofNullable(ingress).map(V1Ingress::getSpec)
                                .map(m -> m.getRules());
                metadata.get().setName(bufferName + "-oauth2-proxy");
                metadata.get().setNamespace("oauth2-proxy");
                tls.get().get(0)
                                .setHosts(Arrays.asList(organization.getName() + ".robolaunch.dev"));
                tls.get().get(0).setSecretName("tls");
                rules.get().get(0)
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
        public void createCoreDNS(Organization organization, String teamId,
                        String cloudInstanceName, String nodeName, String bufferName, String provider, String region,
                        String superCluster)
                        throws IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, ApiException, MinioException,
                        InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
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
                                                                "    kubernetes " + bufferName
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
                                nodeSelectors.put("robolaunch.io/team", teamId);
                                nodeSelectors.put("robolaunch.io/cloud-instance-alias", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/cloud-instance", bufferName);
                                nodeSelectors.put("robolaunch.io/region", region);

                                V1Deployment deployment = (V1Deployment) obj;
                                Optional<V1PodSpec> podSpec = Optional.ofNullable(deployment).map(V1Deployment::getSpec)
                                                .map(V1DeploymentSpec::getTemplate).map(V1PodTemplateSpec::getSpec);
                                podSpec.get().setNodeSelector(nodeSelectors);
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
                        String teamId, String bufferName, Boolean connectionHub, String provider, String region,
                        String superCluster)
                        throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                if (!connectionHub) {
                        Kubectl.label(V1Node.class).apiClient(vcClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/cloud-instance", bufferName)
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region",
                                                        region)
                                        .execute();
                } else {
                        Kubectl.label(V1Node.class).apiClient(vcClient)
                                        .name(
                                                        nodeName)
                                        .addLabel("robolaunch.io/organization", organization.getName())
                                        .addLabel("robolaunch.io/cloud-instance", bufferName)
                                        .addLabel("robolaunch.io/cloud-instance-alias", cloudInstanceName)
                                        .addLabel("robolaunch.io/team", teamId)
                                        .addLabel("robolaunch.io/region",
                                                        region)
                                        .addLabel("submariner.io/gateway", "4490")
                                        .execute();
                }

        }

        @Override
        public void createCertManager(Organization organization, String teamId,
                        String cloudInstanceName, String bufferName, String provider, String region,
                        String superCluster)
                        throws KubectlException, IOException, ApiException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                String yamlString = "";

                String version = kubernetesRepository.getLatestPlatformVersion();
                String yaml = kubernetesService.readPlatformContent(version, "certManager");
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
                                Optional<V1ObjectMeta> metadata = Optional.ofNullable(role.getMetadata());
                                rbacApi.createNamespacedRole(metadata.get().getNamespace(), role, null, null, null,
                                                null);

                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1RoleBinding")) {
                                V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                Optional<V1ObjectMeta> metadata = Optional.ofNullable(roleBinding.getMetadata());
                                rbacApi.createNamespacedRoleBinding(metadata.get().getNamespace(),
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
                                nodeSelectors.put("robolaunch.io/cloud-instance-alias", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/team", teamId);
                                nodeSelectors.put("robolaunch.io/region", region);
                                nodeSelectors.put("robolaunch.io/cloud-instance", bufferName);
                                Optional<V1PodSpec> podSpec = Optional.ofNullable(deployment).map(V1Deployment::getSpec)
                                                .map(V1DeploymentSpec::getTemplate).map(V1PodTemplateSpec::getSpec);
                                podSpec.get().setNodeSelector(nodeSelectors);
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
        public void createFleetOperator(String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization,
                        String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException,
                        KubectlException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);

                String yamlString = "";

                String version = kubernetesRepository.getLatestPlatformVersion();
                String yaml = kubernetesService.readPlatformContent(version, "fleetOperator");
                String bucket = "template-artifacts";

                ModelMapper.addModelMap("cert-manager.io", "v1", "Certificate", "certificates",
                                V1alpha2Certificate.class,
                                V1alpha2CertificateList.class);
                ModelMapper.addModelMap("cert-manager.io", "v1", "Issuer", "issuers",
                                V1alpha2Issuer.class,
                                V1alpha2IssuerList.class);

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
                                V1CustomResourceDefinition crd = (V1CustomResourceDefinition) obj;
                                Kubectl.apply(V1CustomResourceDefinition.class)
                                                .forceConflict(true)
                                                .resource(crd).apiClient(vcClient).execute();
                        }

                        if (type.equals("V1ServiceAccount")) {
                                V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
                                coreApi.createNamespacedServiceAccount("fleet-system",
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
                                coreApi.createNamespacedConfigMap("fleet-system", configMap,
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
                                rbacApi.createNamespacedRole("fleet-system", role, null, null,
                                                null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }

                        if (type.equals("V1RoleBinding")) {
                                V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                rbacApi.createNamespacedRoleBinding("fleet-system", roleBinding,
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
                                coreApi.createNamespacedService("fleet-system", service, null,
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
                                Optional<Map<String, String>> nodeSelectors = Optional
                                                .ofNullable(deployment).map(V1Deployment::getSpec)
                                                .map(V1DeploymentSpec::getTemplate)
                                                .map(V1PodTemplateSpec::getSpec).map(m -> m.getNodeSelector());
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/cloud-instance",
                                                                bufferName);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/organization",
                                                                organization.getName());
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/cloud-instance-alias",
                                                                cloudInstanceName);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/team",
                                                                teamId);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/region",
                                                                region);
                                appsApi.createNamespacedDeployment("fleet-system", deployment,
                                                null, null,
                                                null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1alpha2Certificate")) {
                                Thread.sleep(12000);
                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "fleet-system", "certificates", obj,
                                                null, null, null);

                        }

                        if (type.equals("V1alpha2Issuer")) {
                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "fleet-system", "issuers",
                                                obj,
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
                artifact3.setName("fleetOperator.yaml");
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact3,
                                organization.getName());
        }

        @Override
        public void createConnectionHubOperator(String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization,
                        String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException,
                        KubectlException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                String yamlString = "";

                String version = kubernetesRepository.getLatestPlatformVersion();
                String yaml = kubernetesService.readPlatformContent(version, "connectionHub");
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
                                coreApi.createNamespacedServiceAccount("connection-hub-system", serviceAccount, null,
                                                null, null,
                                                null);
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
                                Optional<V1ObjectMeta> metadata = Optional.ofNullable(role.getMetadata());
                                rbacApi.createNamespacedRole(metadata.get().getNamespace(), role, null, null, null,
                                                null);

                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1RoleBinding")) {
                                V1RoleBinding roleBinding = (V1RoleBinding) obj;
                                Optional<V1ObjectMeta> metadata = Optional.ofNullable(roleBinding.getMetadata());
                                rbacApi.createNamespacedRoleBinding(metadata.get().getNamespace(),
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
                                V1Deployment deployment = (V1Deployment) obj;
                                Map<String, String> nodeSelectors = new HashMap<>();
                                nodeSelectors.put("robolaunch.io/organization", organization.getName());
                                nodeSelectors.put("robolaunch.io/cloud-instance-alias", cloudInstanceName);
                                nodeSelectors.put("robolaunch.io/team", teamId);
                                Optional<V1PodSpec> podSpec = Optional.ofNullable(deployment).map(V1Deployment::getSpec)
                                                .map(V1DeploymentSpec::getTemplate).map(V1PodTemplateSpec::getSpec);
                                podSpec.get().setNodeSelector(nodeSelectors);
                                appsApi.createNamespacedDeployment(
                                                "connection-hub-system", deployment,
                                                null, null, null, null);
                                String jsonInString = new Gson().toJson(obj);
                                JSONObject mJSONObject = new JSONObject(jsonInString);
                                yamlString += cloudInstanceHelperRepository
                                                .convertJsonStringToYamlString(mJSONObject.toString());
                                yamlString += "---";
                        }
                        if (type.equals("V1alpha2Certificate")) {
                                Thread.sleep(5000);
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
        public void createConnectionHub(String bufferName, Organization organization, String teamId,
                        String cloudInstanceName, String serverIP, String namespaceName, String provider, String region,
                        String superCluster) {
                System.out.println("buffer name: " + bufferName);
                System.out.println("organization name: " + organization.getName());
                System.out.println("team id: " + teamId);
                System.out.println("cloud instance name: " + cloudInstanceName);
                System.out.println("server ip: " + serverIP);
                System.out.println("namespace name: " + namespaceName);
                System.out.println("provider: " + provider);
                System.out.println("region: " + region);
                System.out.println("super cluster: " + superCluster);
                System.out.println("cloud instance name: " + cloudInstanceName);
                ApiClient vcClient;
                String version;
                DynamicKubernetesApi subnetsApi;
                try {
                        vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                        provider, region, superCluster);
                        subnetsApi = apiClientManager.getSubnetApi(provider, region, superCluster);
                        version = kubernetesRepository.getLatestPlatformVersion();
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | InterruptedException | MinioException e) {
                        throw new ApplicationException("Error while creating clients.");
                } catch (ApiException e) {
                        throw new ApplicationException("Error while creating clients ApiException."
                                        + e.getResponseBody() + " " + e.getCode());

                }

                JsonObject object = kubernetesService.readPlatformContentAsJsonObject(version,
                                "connectionHubCloud");

                var subnet = subnetsApi.get("subnet-" + namespaceName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance",
                                bufferName);
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance-alias",
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

                System.out.println("final object connection hub : " + object);
                CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcClient);
                try {
                        customObjectsApi.createClusterCustomObject("connection-hub.roboscale.io",
                                        "v1alpha1",
                                        "connectionhubs", object, null, null, null);
                } catch (ApiException e) {
                        throw new ApplicationException("Error while just creating connection hub."
                                        + e.getResponseBody() + " " + e.getCode());
                }

        }

        @Override
        public void createRobotOperator(Organization organization, String cloudInstanceName,
                        String teamId, String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        MinioException, ApiException, KubectlException, InterruptedException {
                String yamlString = "";
                ApiClient vcClient = cloudInstanceHelperRepository
                                .getVirtualClusterClientWithBufferName(bufferName, provider, region,
                                                superCluster);
                String version = kubernetesRepository.getLatestPlatformVersion();

                String yaml = kubernetesService.readPlatformContent(version,
                                "robotOperator");

                ModelMapper.addModelMap("cert-manager.io", "v1", "Certificate", "certificates",
                                V1alpha2Certificate.class,
                                V1alpha2CertificateList.class);
                ModelMapper.addModelMap("cert-manager.io", "v1", "Issuer", "issuers",
                                V1alpha2Issuer.class,
                                V1alpha2IssuerList.class);

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
                                Optional<Map<String, String>> nodeSelectors = Optional
                                                .ofNullable(deployment).map(V1Deployment::getSpec)
                                                .map(V1DeploymentSpec::getTemplate)
                                                .map(V1PodTemplateSpec::getSpec).map(m -> m.getNodeSelector());
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/cloud-instance",
                                                                bufferName);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/organization",
                                                                organization.getName());
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/cloud-instance-alias",
                                                                cloudInstanceName);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/team",
                                                                teamId);
                                nodeSelectors.get()
                                                .put(
                                                                "robolaunch.io/region",
                                                                region);
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
                                Thread.sleep(12000);
                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "robot-system", "certificates", obj,
                                                null, null, null);

                        }

                        if (type.equals("V1alpha2Issuer")) {
                                customObjectsApi.createNamespacedCustomObject("cert-manager.io",
                                                "v1",
                                                "robot-system", "issuers",
                                                obj,
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
                storageRepository.push(yamlString.getBytes(StandardCharsets.UTF_8), artifact3, organization.getName());

        }

        @Override
        public void createDNSRecord(Organization organization, String nodeName, String provider, String region,
                        String superCluster)
                        throws ApiException, InternalError, ApplicationException, IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException {
                CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);

                /* Get ExternalIP from node */
                String externalIP = "";
                var node = coreV1Api.readNode(nodeName, null);
                Optional<List<V1NodeAddress>> nodeAddresses = Optional.ofNullable(node)
                                .map(V1Node::getStatus).map(V1NodeStatus::getAddresses);

                for (int i = 0; i < nodeAddresses.get().size(); i++) {
                        if (nodeAddresses.get().get(i).getType().equals(
                                        "ExternalIP")) {
                                externalIP = nodeAddresses.get().get(i).getAddress().toString();
                        }
                }
                String requestData = groupAdapter.toCreateDNSRecord(organization, externalIP, dnsZoneName);
                String createDNSRecord = String.format("{\"id\": 0, \"method\": \"dnsrecord_add/1\", \"params\": %s} ",
                                requestData);
                groupAdminRepository.makeRequest(createDNSRecord);

        }

        @Override
        public void addBufferedLabelToVC(String bufferName, String instanceType, String provider, String region,
                        String superCluster)
                        throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
                                "virtualclusters", true,
                                V1VirtualCluster.class);

                Kubectl.label(V1VirtualCluster.class).apiClient(apiClient).name(bufferName).namespace("default")
                                .addLabel("buffered", "true")
                                .execute();

                Kubectl.label(V1VirtualCluster.class).apiClient(apiClient).name(bufferName).namespace("default")
                                .addLabel("robolaunch.io/instance-type", instanceType)
                                .execute();

        }

        @Override
        public void scaleVCWorkloadsDown(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                AppsV1Api appsV1Api = new AppsV1Api(vcClient);
                V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null,
                                null, null, null, null, null);
                for (V1Deployment deployment : deploymentList.getItems()) {
                        Optional<String> deploymentNsName = Optional.ofNullable(deployment)
                                        .map(V1Deployment::getMetadata)
                                        .map(V1ObjectMeta::getNamespace);
                        Optional<String> deploymentName = Optional.ofNullable(deployment).map(V1Deployment::getMetadata)
                                        .map(V1ObjectMeta::getName);
                        if (deploymentNsName.get().equals("kube-system")
                                        && deploymentName.get().equals("coredns")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deploymentNsName.get())
                                                .name(deploymentName.get()).replicas(0).execute();
                        }
                        if (deploymentNsName.get().equals("cert-manager")
                                        && deploymentName.get().equals("cert-manager")
                                        || deploymentName.get().equals("cert-manager-cainjector")
                                        || deploymentName.get().equals("cert-manager-webhook")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deploymentNsName.get())
                                                .name(deploymentName.get()).replicas(0).execute();
                        }
                        if (deploymentNsName.get().equals("robot-system")
                                        && deploymentName.get().equals("robot-controller-manager")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deployment.getMetadata().getNamespace())
                                                .name(deploymentName.get()).replicas(0).execute();
                        }
                }
        }

        @Override
        public void scaleVCWorkloadsUp(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                AppsV1Api appsV1Api = new AppsV1Api(vcClient);
                V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null,
                                null, null, null, null, null);
                for (V1Deployment deployment : deploymentList.getItems()) {
                        Optional<String> deploymentNsName = Optional.ofNullable(deployment)
                                        .map(V1Deployment::getMetadata)
                                        .map(V1ObjectMeta::getNamespace);
                        Optional<String> deploymentName = Optional.ofNullable(deployment).map(V1Deployment::getMetadata)
                                        .map(V1ObjectMeta::getName);
                        if (deploymentNsName.get().equals("kube-system")
                                        && deploymentName.get().equals("coredns")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deploymentNsName.get())
                                                .name(deploymentName.get()).replicas(1).execute();
                        }
                        if (deploymentNsName.get().equals("cert-manager")
                                        && deploymentName.get().equals("cert-manager")
                                        || deploymentName.get().equals("cert-manager-cainjector")
                                        || deploymentName.get().equals("cert-manager-webhook")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deploymentNsName.get())
                                                .name(deploymentName.get()).replicas(1).execute();
                        }
                        if (deploymentNsName.get().equals("robot-system")
                                        && deploymentName.get().equals("robot-controller-manager")) {
                                Kubectl.scale(V1Deployment.class).apiClient(vcClient)
                                                .namespace(deploymentNsName.get())
                                                .name(deploymentName.get()).replicas(1).execute();
                        }
                }
        }

        @Override
        public void scaleOAuth2ProxyDown(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, KubectlException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName,
                                provider, region, superCluster);
                ApiClient apiClient = cloudInstanceHelperRepository.adminApiClient(provider, region, superCluster);
                Kubectl.scale(V1Deployment.class).apiClient(apiClient)
                                .namespace(namespaceName + "-oauth2-proxy")
                                .name("oauth2-proxy").replicas(0).execute();

        }

        @Override
        public void scaleOAuth2ProxyUp(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, KubectlException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName,
                                provider, region, superCluster);
                ApiClient apiClient = cloudInstanceHelperRepository.adminApiClient(provider, region, superCluster);
                Kubectl.scale(V1Deployment.class).apiClient(apiClient).namespace(namespaceName + "-oauth2-proxy")
                                .name("oauth2-proxy").replicas(1).execute();
        }

        @Override
        public void scaleCoreDNSUp(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                try {
                        Kubectl.scale(V1Deployment.class).apiClient(vcClient).namespace("kube-system")
                                        .name("coredns").replicas(1).execute();
                } catch (KubectlException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public void unlabelSuperClusterNode(String nodeName, String provider, String region, String superCluster)
                        throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
                String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1cloud-instance\" }]";
                V1Patch patch = new V1Patch(patchString);
                Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
        }

        @Override
        public void createClusterAdminRole(Organization organization, String bufferName, String username,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException {
                Artifact artifact = new Artifact();
                artifact.setName("clusterAdminRole.yaml");
                String bucket = "template-artifacts";
                String yaml = storageRepository.getContent(artifact, bucket);
                List<Object> list = Yaml.loadAll(yaml);

                ApiClient apiClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);

                RbacAuthorizationV1Api rbacAuthorizationV1Api = new RbacAuthorizationV1Api(apiClient);

                for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String type = obj.getClass().getSimpleName();
                        if (type.equals("V1ClusterRole")) {
                                V1PolicyRule v1PolicyRule = new V1PolicyRule();
                                v1PolicyRule.addApiGroupsItem("*");
                                v1PolicyRule.addResourcesItem("*");
                                v1PolicyRule.addVerbsItem("*");
                                V1ClusterRole clusterRole = (V1ClusterRole) obj;
                                rbacAuthorizationV1Api.createClusterRole(clusterRole, null, null, null, null);
                        }
                        if (type.equals("V1ClusterRoleBinding")) {
                                V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
                                V1Subject v1Subject = new V1Subject();
                                v1Subject.setKind("User");
                                v1Subject.setName(keycloakURL + "/realms/" + organization.getName() + "#" + username);
                                v1Subject.setApiGroup("rbac.authorization.k8s.io");
                                clusterRoleBinding.addSubjectsItem(v1Subject);

                                V1Subject v1SubjectAdmin = new V1Subject();
                                v1SubjectAdmin.setKind("User");
                                v1SubjectAdmin.setName(
                                                keycloakURL + "/realms/" + organization.getName() + "#" + "bigboss");
                                v1SubjectAdmin.setApiGroup("rbac.authorization.k8s.io");
                                clusterRoleBinding.addSubjectsItem(v1SubjectAdmin);
                                rbacAuthorizationV1Api.createClusterRoleBinding(clusterRoleBinding, null, null, null,
                                                null);
                        }
                }
        }
}
