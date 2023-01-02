package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Repository;
import org.robolaunch.models.Workspace;
import org.robolaunch.models.request.RequestCreateRobot;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotBuildManagerStep;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;
import org.robolaunch.models.request.RobotLaunchManagerLaunchItem;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotHelperRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
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

        public void createRobot(RequestCreateRobot requestCreateRobot, String token, String provider, String region,
                        String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                Gson gson = new Gson();

                String bufferName = requestCreateRobot.getBufferName();

                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token, provider, region,
                                superCluster);
                // ApiClient roApi =
                // cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                // provider, region, superCluster);
                // DynamicKubernetesApi robotBuildManagerApi = new
                // DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                // "robots", robotsApi);

                CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                // Get template Robot YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robot.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/organization",
                                requestCreateRobot.getOrganization().getName());

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/team", requestCreateRobot.getTeamId());
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/region", requestCreateRobot.getRegion());
                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/cloud-instance", requestCreateRobot.getCloudInstance());

                //// NEED FIX - DISTRO
                object.get("spec").getAsJsonObject().addProperty("distro",
                                requestCreateRobot.getRobotWorkspaces().get(0).getDistro());

                // Converting GB to MB
                object.get("spec").getAsJsonObject().get("storage").getAsJsonObject().addProperty("amount",
                                requestCreateRobot.getRobotInfo().getRobotStorage() * 1000);

                object.get("spec").getAsJsonObject().get("discoveryServerTemplate").getAsJsonObject().addProperty(
                                "cluster",
                                requestCreateRobot.getBufferName());

                //// NEED FIX - ROS BRIDGE
                if (requestCreateRobot.getRobotInfo().isRobotRos1Bridge()) {
                        JsonObject rosBridgeObject = new JsonObject();
                        JsonObject rosBridgeObjectROS = new JsonObject();
                        JsonObject rosBridgeObjectROS2 = new JsonObject();
                        rosBridgeObjectROS.addProperty("enabled", false);
                        rosBridgeObjectROS.addProperty("distro", "noetic");
                        rosBridgeObject.add("ros", rosBridgeObjectROS);

                        rosBridgeObjectROS2.addProperty("enabled", true);
                        //// FIX DISTRO
                        rosBridgeObjectROS2.addProperty("distro",
                                        requestCreateRobot.getRobotWorkspaces().get(0).getDistro());
                        rosBridgeObject.add("ros2", rosBridgeObjectROS2);

                        rosBridgeObject.addProperty("image",
                                        "robolaunchio/foxy-noetic-bridge:v0.0.3");

                        object.get("spec").getAsJsonObject().add("rosBridgeTemplate",
                                        rosBridgeObject);
                }

                object.get("spec").getAsJsonObject().get("robotDevSuiteTemplate").getAsJsonObject()
                                .addProperty("vdiEnabled", requestCreateRobot.getRobotInfo().isRobotVDI());

                object.get("spec").getAsJsonObject().get("robotDevSuiteTemplate").getAsJsonObject()
                                .addProperty("ideEnabled", requestCreateRobot.getRobotInfo().isRobotIDE());

                if (requestCreateRobot.getRobotInfo().isRobotVDI()) {
                        JsonObject vdiObject = new JsonObject();
                        vdiObject.addProperty("serviceType", "NodePort");
                        vdiObject.addProperty("ingress", false);
                        vdiObject.addProperty("privileged", false);
                        String webRTCPortRange = robotHelperRepository.getAvailablePortRange(2, provider, region,
                                        superCluster);
                        vdiObject.addProperty("webrtcPortRange", webRTCPortRange);

                        object.get("spec").getAsJsonObject().get("robotDevSuiteTemplate").getAsJsonObject()
                                        .add("robotVDITemplate", vdiObject);
                }

                if (requestCreateRobot.getRobotInfo().isRobotIDE()) {
                        JsonObject ideObject = new JsonObject();
                        ideObject.addProperty("serviceType", "NodePort");
                        ideObject.addProperty("ingress", false);
                        ideObject.addProperty("privileged", false);
                        object.get("spec").getAsJsonObject().get("robotDevSuiteTemplate").getAsJsonObject()
                                        .add("robotIDETemplate", ideObject);
                }

                // JsonArray buildManagerObject = new JsonArray();
                // for (var step : requestCreateRobot.getRobotBuildSteps()) {
                // JsonObject stepObject = new JsonObject();
                // stepObject.addProperty("name", step.getName());
                // stepObject.addProperty("workspace", step.getWorkspace());
                // if (step.getCodeType().equals("command")) {
                // stepObject.addProperty("command", step.getCode());

                // } else {
                // stepObject.addProperty("script", step.getCode());
                // }
                // buildManagerObject.add(stepObject);
                // }

                // object.get("spec").getAsJsonObject().get("buildManagerTemplate").getAsJsonObject().add("steps",
                // buildManagerObject);

                // for (RobotLaunchManagerLaunchItem item :
                // requestCreateRobot.getLaunchManagerLaunchItems()) {
                // JsonObject itemObject = new JsonObject();
                // JsonObject selectors = new JsonObject();
                // if (item.getCluster().equals("cloud")) {
                // selectors.addProperty("robolaunch.io/cloud-instance", item.getClusterName());
                // } else if (item.getCluster().equals("physical")) {
                // selectors.addProperty("robolaunch.io/physical-instance",
                // item.getClusterName());
                // }
                // itemObject.add("selector", selectors);
                // itemObject.addProperty("workspace", item.getWorkspace());
                // itemObject.addProperty("repository", item.getRepository());
                // itemObject.addProperty("namespacing", item.isNamespacing());
                // itemObject.addProperty("launchFilePath", item.getLaunchFilePath());

                // object.get("spec").getAsJsonObject().get("launchManagerTemplates").getAsJsonArray().get(0)
                // .getAsJsonObject().add(item.getName(),
                // itemObject);
                // }

                // FIX - workspace path - gokhan does not give me.
                object.get("spec").getAsJsonObject().addProperty("workspacesPath",
                                "/root/workspaces");
                JsonArray workspacesArray = new JsonArray();
                for (Workspace workspace : requestCreateRobot.getRobotWorkspaces()) {
                        JsonObject workspaceObject = new JsonObject();
                        JsonObject repositories = new JsonObject();
                        for (Repository repository : workspace.getRepositories()) {
                                JsonObject repositoryObject = new JsonObject();
                                repositoryObject.addProperty("url", repository.getUrl());
                                repositoryObject.addProperty("branch", repository.getBranch());

                                //// FIX - repository name.
                                repositories.add(repository.getName(), repositoryObject);
                        }
                        workspaceObject.addProperty("name", workspace.getName());
                        workspaceObject.add("repositories", repositories);
                        workspacesArray.add(workspaceObject);
                }
                object.get("spec").getAsJsonObject().add("workspaces", workspacesArray);

                customObjectsApi.createNamespacedCustomObject("robot.roboscale.io", "v1alpha1", "robot-system",
                                "robots", object, null, null, null);

        }

}
