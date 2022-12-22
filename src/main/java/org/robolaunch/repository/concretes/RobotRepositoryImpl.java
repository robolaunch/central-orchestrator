package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotBuildManagerStep;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;
import org.robolaunch.models.request.RobotLaunchManagerLaunchItem;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.minio.errors.MinioException;

@ApplicationScoped
public class RobotRepositoryImpl implements RobotRepository {
        private ApiClient adminApiClient;
        private CoreV1Api coreV1Api;
        @Inject
        CloudInstanceHelperRepository cloudInstanceHelperRepository;

        @Inject
        StorageRepository storageRepository;

        @Inject
        JsonWebToken jwt;

        @PostConstruct
        public void initializeApis() throws IOException, ApiException, InterruptedException {
                this.adminApiClient = cloudInstanceHelperRepository.adminApiClient();
                this.coreV1Api = new CoreV1Api(adminApiClient);
        }

        @Override
        public void makeRobotsPassive(String bufferName)
                        throws IOException, ApiException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
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
                        String bufferName)
                        throws IOException, ApiException, InterruptedException {

                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
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
        public void createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token);
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
                        stepObject.addProperty("command", step.getCommand());
                        stepObject.addProperty("script", step.getScript());

                        object.get("spec").getAsJsonObject().get("steps").getAsJsonArray().add(stepObject);
                }

                robotBuildManagerApi.create(new DynamicKubernetesObject(object));

        }

        @Override
        public void createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token);
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
        public void createRobotDevelopmentSuite(RobotDevSuite robotDevSuite, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException {
                ApiClient robotsApi = cloudInstanceHelperRepository.userApiClient(bufferName, token);
                DynamicKubernetesApi robotBuildManagerApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robotdevsuites", robotsApi);

                List<V1Service> services = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null,
                                null, null, null, null).getItems();
                List<Integer> ports = new ArrayList<Integer>();

                for (V1Service service : services) {
                        Optional<String> type = Optional.ofNullable(service).map(V1Service::getSpec)
                                        .map(V1ServiceSpec::getType);
                        Optional<List<V1ServicePort>> servicePorts = Optional.ofNullable(service)
                                        .map(V1Service::getSpec)
                                        .map(V1ServiceSpec::getPorts);
                        // Check the spec -> type -> NodePort
                        if (type.get().equals("NodePort")) {
                                ports.add(servicePorts.get().get(0).getNodePort());
                        }
                }
                Gson gson = new Gson();

                System.out.println("Ports: " + gson.toJson(ports));

                // Get template RobotDevelopmentSuite YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotDevelopmentSuite.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);

                object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                                "robolaunch.io/target-robot",
                                robotDevSuite.getTargetRobot());
                object.get("metadata").getAsJsonObject().addProperty("name", robotDevSuite.getName());

                object.get("spec").getAsJsonObject().addProperty("vdiEnabled", robotDevSuite.getVdiEnabled());
                object.get("spec").getAsJsonObject().addProperty("ideEnabled", robotDevSuite.getIdeEnabled());

                if (robotDevSuite.getVdiEnabled()) {
                        JsonObject vdiObject = new JsonObject();
                        vdiObject.addProperty("serviceType", robotDevSuite.getVdiTemplate().getServiceType());
                        vdiObject.addProperty("ingress", robotDevSuite.getVdiTemplate().getIngress());
                        vdiObject.addProperty("privileged", robotDevSuite.getVdiTemplate().getPrivileged());
                        // Calculate the port range.
                        vdiObject.addProperty("webrtcPortRange", "2132-21321");
                }

                if (robotDevSuite.getIdeEnabled()) {
                        JsonObject ideObject = new JsonObject();
                        ideObject.addProperty("serviceType", robotDevSuite.getIdeTemplate().getServiceType());
                        ideObject.addProperty("ingress", robotDevSuite.getIdeTemplate().getIngress());
                        ideObject.addProperty("privileged", robotDevSuite.getIdeTemplate().getPrivileged());
                }

                robotBuildManagerApi.create(new DynamicKubernetesObject(object));

        }

        public void createRobot(Organization organization, String teamId, String region, String cloudInstance)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException {
                // Get template Robot YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robot.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        }

}
