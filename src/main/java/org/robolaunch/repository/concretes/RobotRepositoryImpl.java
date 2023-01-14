package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Robot;
import org.robolaunch.models.request.RequestRobot;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotBuildManagerStep;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;
import org.robolaunch.models.request.RobotLaunchManagerLaunchItem;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotHelperRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.StorageV1Api;
import io.kubernetes.client.openapi.models.V1StorageClassList;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.minio.errors.MinioException;

@ApplicationScoped
public class RobotRepositoryImpl implements RobotRepository {
        @Inject
        CloudInstanceHelperRepository cloudInstanceHelperRepository;

        @Inject
        StorageRepository storageRepository;

        @Inject
        JsonWebToken jwt;

        @Inject
        RobotHelperRepository robotHelperRepository;

        @Inject
        ApiClientManager apiClientManager;

        @Override
        public void makeRobotsPassive(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);

                DynamicKubernetesApi robotsApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robots",
                                vcClient);
                String patchString = "[{ \"op\": \"replace\", \"path\": \"/spec/robot/state\", \"value\": \"Passive\"}]";

                var robotList = robotsApi.list().getObject().getItems();
                V1Patch patch = new V1Patch(patchString);

                for (var robot : robotList) {
                        robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
                }
        }

        @Override
        public void makeRobotsActive(
                        String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException {

                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                provider, region, superCluster);
                DynamicKubernetesApi robotsApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robots",
                                vcClient);
                String patchString = "[{ \"op\": \"replace\", \"path\": \"/spec/robot/state\", \"value\": \"Launched\"}]";

                var robotList = robotsApi.list().getObject().getItems();
                V1Patch patch = new V1Patch(patchString);

                for (var robot : robotList) {
                        robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
                }

        }

        @Override
        public void createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String token,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token, provider, region,
                                superCluster);
                DynamicKubernetesApi robotBuildManagerApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "buildmanagers", robotsApi);
                // Get template RobotBuildManager YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotBuildManager.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/target-robot",
                                robotBuildManager.getTargetRobot());

                for (RobotBuildManagerStep step : robotBuildManager.getSteps()) {
                        JsonObject stepObject = new JsonObject();
                        stepObject.addProperty("name", step.getName());
                        stepObject.addProperty("workspace", step.getWorkspace());
                        if (step.getCodeType().equals("command")) {
                                stepObject.addProperty("command", step.getCode());
                        }
                        if (step.getCodeType().equals("script")) {
                                stepObject.addProperty("script", step.getCode());
                        }

                        object.get("spec").getAsJsonObject().get("steps").getAsJsonArray().add(stepObject);
                }

                robotBuildManagerApi.create(new DynamicKubernetesObject(object));

        }

        @Override
        public void createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName, String token,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token, provider, region,
                                superCluster);
                DynamicKubernetesApi robotBuildManagerApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "launchmanagers", robotsApi);
                // Get template RobotLaunchManager YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotLaunchManager.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/target-robot",
                                robotLaunchManager.getTargetRobot());

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/target-vdi",
                                robotLaunchManager.getTargetVDI());

                for (RobotLaunchManagerLaunchItem item : robotLaunchManager.getLaunchItems()) {
                        JsonObject itemObject = new JsonObject();
                        JsonObject selectors = new JsonObject();
                        if (item.getCluster().equals("cloud")) {
                                selectors.addProperty("robolaunch.io/cloud-instance", item.getClusterName());
                        } else if (item.getCluster().equals("physical")) {
                                selectors.addProperty("robolaunch.io/physical-instance", item.getClusterName());
                        }
                        itemObject.add("selector", selectors);
                        itemObject.addProperty("workspace", item.getWorkspace());
                        itemObject.addProperty("repository", item.getRepository());
                        itemObject.addProperty("namespacing", item.isNamespacing());
                        itemObject.addProperty("launchFilePath", item.getLaunchFilePath());

                        object.get("spec").getAsJsonObject().get("launch").getAsJsonObject().add(item.getName(),
                                        itemObject);
                }

                robotBuildManagerApi.create(new DynamicKubernetesObject(object));

        }

        @Override
        public void createRobotDevelopmentSuite(RobotDevSuite robotDevSuite, String bufferName, String token,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token, provider, region,
                                superCluster);
                DynamicKubernetesApi robotBuildManagerApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robotdevsuites", robotsApi);

                // Get template RobotDevelopmentSuite YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotDevelopmentSuite.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/target-robot",
                                robotDevSuite.getTargetRobot());
                object.get("metadata").getAsJsonObject().addProperty("name", robotDevSuite.getName());

                object.get("spec").getAsJsonObject().addProperty("vdiEnabled", robotDevSuite.isVdiEnabled());
                object.get("spec").getAsJsonObject().addProperty("ideEnabled", robotDevSuite.isIdeEnabled());

                if (robotDevSuite.isVdiEnabled()) {
                        JsonObject vdiObject = new JsonObject();
                        vdiObject.addProperty("serviceType", robotDevSuite.getVdiTemplate().getServiceType());
                        vdiObject.addProperty("ingress", robotDevSuite.getVdiTemplate().isIngress());
                        vdiObject.addProperty("privileged", robotDevSuite.getVdiTemplate().isPrivileged());

                        String webRTCPorts = robotHelperRepository.getAvailablePortRange(3, provider, region,
                                        superCluster);
                        vdiObject.addProperty("webrtcPortRange", webRTCPorts);
                }

                if (robotDevSuite.isIdeEnabled()) {
                        JsonObject ideObject = new JsonObject();
                        ideObject.addProperty("serviceType", robotDevSuite.getIdeTemplate().getServiceType());
                        ideObject.addProperty("ingress", robotDevSuite.getIdeTemplate().isIngress());
                        ideObject.addProperty("privileged", robotDevSuite.getIdeTemplate().isPrivileged());
                }

                robotBuildManagerApi.create(new DynamicKubernetesObject(object));

        }

        public void createRobot(RequestRobot requestRobot, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                Gson gson = new Gson();
                String bufferName = requestRobot.getBufferName();
                ApiClient robotsApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                requestRobot.getProvider(), requestRobot.getRegion(), requestRobot.getSuperCluster());

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(requestRobot);
                JsonObject robotObject = gson.fromJson(json, JsonObject.class);

                JsonObject labelsObject = new JsonObject();
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().add("labels",
                                labelsObject);

                ApiClient superClusterApi = apiClientManager.getAdminApiClient(requestRobot.getProvider(),
                                requestRobot.getRegion(), requestRobot.getSuperCluster());
                StorageV1Api storageV1Api = new StorageV1Api(superClusterApi);
                V1StorageClassList storageClassList = storageV1Api.listStorageClass(null, null, null, null, null, null,
                                null, null, null, null);

                System.out.println("Storage class name: " + storageClassList.getItems().get(0).getMetadata().getName());
                // Update Storage Class
                robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject().get("storage")
                                .getAsJsonObject().get("storageClassConfig")
                                .getAsJsonObject()
                                .addProperty("name", storageClassList.getItems().get(0).getMetadata().getName());

                // Update Labels
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject()
                                .get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/organization",
                                                requestRobot.getOrganization().getName());
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/team",
                                                requestRobot.getTeamId());
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/region",
                                                requestRobot.getRegion());
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/buffer-instance",
                                                requestRobot.getBufferName());
                robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/cloud-instance",
                                                requestRobot.getCloudInstanceName());

                if (requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate().isVdiEnabled()) {
                        Integer sessionCount = requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate()
                                        .getRobotVDITemplate().getSessionCount();
                        String webRTCPorts = robotHelperRepository.getAvailablePortRange(sessionCount,
                                        requestRobot.getProvider(),
                                        requestRobot.getRegion(), requestRobot.getSuperCluster());
                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .remove("sessionCount");
                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .addProperty("webrtcPortRange", webRTCPorts);
                } else {
                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .remove("sessionCount");
                }

                // Update Repositories
                robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject().get("workspaceManagerTemplate")
                                .getAsJsonObject().get("workspaces").getAsJsonArray().forEach(workspace -> {
                                        JsonObject repositoriesObject = new JsonObject();
                                        workspace.getAsJsonObject().get("repositories").getAsJsonArray()
                                                        .forEach(repo -> {
                                                                JsonObject cloudRepoInside = new JsonObject();
                                                                cloudRepoInside.add("url",
                                                                                repo.getAsJsonObject().get("url"));
                                                                cloudRepoInside.add("branch",
                                                                                repo.getAsJsonObject().get("branch"));

                                                                repositoriesObject.add(
                                                                                repo.getAsJsonObject().get("name")
                                                                                                .getAsString(),
                                                                                cloudRepoInside);
                                                        });
                                        workspace.getAsJsonObject().remove("repositories");
                                        workspace.getAsJsonObject().add("repositories", repositoriesObject);
                                });

                JsonObject finalRobotObject = robotObject.get("robot").getAsJsonObject();
                CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                customObjectsApi.createNamespacedCustomObject("robot.roboscale.io",
                                "v1alpha1", "default",
                                "robots", finalRobotObject, null, null, null);

        }

        public String hybridRobotScript(String provider, String region, String superCluster, Organization organization,
                        String teamId, String bufferName,
                        String cloudInstanceName, String physicalInstanceName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException {

                String machineName = cloudInstanceHelperRepository.getGeneratedMachineName(bufferName, provider, region,
                                superCluster);
                String nodeName = cloudInstanceHelperRepository.getNodeName(machineName, provider, region,
                                superCluster);

                String k3sVersion = "v1.19.16+k3s1";
                String clusterCIDR = "";
                String serviceCIDR = "";
                String clusterDomain = "";
                String script = "";

                String k3s = "curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="
                                + k3sVersion
                                + " K3S_KUBECONFIG_MODE=\"644\" INSTALL_K3S_EXEC=\"--cluster-cidr=" + clusterCIDR
                                + " --service-cidr=" + serviceCIDR
                                + " --cluster-domain=" + clusterDomain
                                + " --disable-network-policy --disable=traefik\" sh -";

                script.concat(k3s + "\n");

                String label = "kubectl label node " + nodeName
                                + " robolaunch.io/organization=" + organization.getName()
                                + " robolaunch.io/team=" + teamId
                                + " robolaunch.io/region=" + region
                                + " robolaunch.io/buffer-instance=" + bufferName
                                + " robolaunch.io/cloud-instance=" + cloudInstanceName
                                + " robolaunch.io/physical-instance= " + physicalInstanceName;

                script.concat(label + "\n");

                String connectionHubOperator = "kubectl apply -f https://github.com/robolaunch/connection-hub-operator/releases/download/v0.1.2/connection_hub_operator.yaml";

                return script;

        }

        public ArrayList<Robot> getRobotsOrganization(Organization organization) {

                return null;
        }

}
