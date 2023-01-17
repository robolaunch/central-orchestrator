package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RequestBuildManager;
import org.robolaunch.models.request.RequestLaunchManager;
import org.robolaunch.models.request.RequestLaunchManagerSpecLaunch;
import org.robolaunch.models.request.RequestRobot;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotHelperRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class RobotRepositoryImpl implements RobotRepository {
        DynamicGraphQLClient graphqlClient;

        @ConfigProperty(name = "kogito.dataindex.http.url")
        String kogitoDataIndexUrl;
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

        @PostConstruct
        public void initializeServices() throws IOException {
                graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
                                .url(kogitoDataIndexUrl + "/graphql")
                                .build();
        }

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
        public void createRobotBuildManager(RequestBuildManager robotBuildManager, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException,
                        ExecutionException {
                Gson gson = new Gson();
                String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                + robotBuildManager.getRobotProcessId()
                                + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

                Response response = graphqlClient.executeSync(queryStr);
                javax.json.JsonObject data = response.getData();
                JsonArray processInstances = data.getJsonArray("ProcessInstances");

                ObjectMapper mapper = new ObjectMapper();
                JsonArray childProcessInstances = processInstances.getJsonObject(0)
                                .getJsonArray("childProcessInstances");
                JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(0).getString("variables"));
                String bufferName = childNode.get("bufferName").asText();
                String providerName = childNode.get("providerName").asText();
                String regionName = childNode.get("regionName").asText();
                String superClusterName = childNode.get("superClusterName").asText();

                ApiClient robotsApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                providerName, regionName,
                                superClusterName);

                String json = mapper.writeValueAsString(robotBuildManager);
                JsonObject buildManagerObject = gson.fromJson(json, JsonObject.class);

                JsonObject labelsObject = new JsonObject();
                buildManagerObject.get("buildManager").getAsJsonObject().get("metadata").getAsJsonObject().add("labels",
                                labelsObject);

                buildManagerObject.get("buildManager").getAsJsonObject().get("metadata").getAsJsonObject()
                                .get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/target-robot",
                                                robotBuildManager.getTargetRobot());
                com.google.gson.JsonArray steps = buildManagerObject.get("buildManager").getAsJsonObject().get("steps")
                                .getAsJsonArray();
                JsonObject emptySpec = new JsonObject();
                buildManagerObject.get("buildManager").getAsJsonObject().add("spec", emptySpec);

                buildManagerObject.get("buildManager").getAsJsonObject().get("spec").getAsJsonObject().add("steps",
                                steps);
                buildManagerObject.get("buildManager")
                                .getAsJsonObject().remove("steps");

                CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                customObjectsApi.createClusterCustomObject("robot.roboscale.io", "v1alpha1", "buildmanagers",
                                buildManagerObject.get("buildManager"), null, null, null);

        }

        @Override
        public void createRobotLaunchManager(RequestLaunchManager robotLaunchManager, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException,
                        ExecutionException {
                Gson gson = new Gson();
                String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                + robotLaunchManager.getRobotProcessId()
                                + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

                Response response = graphqlClient.executeSync(queryStr);
                javax.json.JsonObject data = response.getData();
                JsonArray processInstances = data.getJsonArray("ProcessInstances");

                ObjectMapper mapper = new ObjectMapper();
                JsonArray childProcessInstances = processInstances.getJsonObject(0)
                                .getJsonArray("childProcessInstances");
                JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(0).getString("variables"));
                String bufferName = childNode.get("bufferName").asText();
                String providerName = childNode.get("providerName").asText();
                String regionName = childNode.get("regionName").asText();
                String superClusterName = childNode.get("superClusterName").asText();

                ApiClient robotsApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                providerName, regionName,
                                superClusterName);

                DynamicKubernetesApi robotBuildManagerApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "launchmanagers", robotsApi);

                String json = mapper.writeValueAsString(robotLaunchManager);
                JsonObject launchManagerObject = gson.fromJson(json, JsonObject.class);

                JsonObject labelsObject = new JsonObject();
                launchManagerObject.get("launchManager").getAsJsonObject().get("metadata").getAsJsonObject().add(
                                "labels",
                                labelsObject);

                launchManagerObject.get("launchManager").getAsJsonObject().get("metadata").getAsJsonObject()
                                .get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/target-robot",
                                                robotLaunchManager.getTargetRobot());

                // TODO: Change this to a dynamic value
                launchManagerObject.get("launchManager").getAsJsonObject().get("metadata").getAsJsonObject()
                                .get("labels")
                                .getAsJsonObject().addProperty("robolaunch.io/target-vdi",
                                                "sample-dev-vdi");

                ArrayList<RequestLaunchManagerSpecLaunch> rlms = robotLaunchManager.getLaunchManager()
                                .getLaunch();

                JsonObject launch = new JsonObject();
                for (RequestLaunchManagerSpecLaunch rlm : rlms) {
                        JsonObject launchInside = new JsonObject();
                        JsonObject selector = new JsonObject();
                        if (rlm.getPhysicalInstance() != null)
                                selector.addProperty("robolaunch.io/physical-instance", rlm.getPhysicalInstance());
                        if (rlm.getCloudInstance() != null)
                                selector.addProperty("robolaunch.io/cloud-instance", rlm.getCloudInstance());
                        launchInside.add("selector", selector);
                        launchInside.addProperty("workspace", rlm.getWorkspace());
                        launchInside.addProperty("repository", rlm.getRepository());
                        launchInside.addProperty("namespacing", rlm.isNamespacing());
                        launchInside.addProperty("launchFilePath", rlm.getLaunchFilePath());
                        launch.add(rlm.getName(), launchInside);
                }

                JsonObject spec = new JsonObject();
                spec.add("launch", launch);

                launchManagerObject.get("launchManager").getAsJsonObject()
                                .remove("launch");
                launchManagerObject.get("launchManager").getAsJsonObject().add("spec", spec);

                robotBuildManagerApi.create(new DynamicKubernetesObject(
                                launchManagerObject.get("launchManager").getAsJsonObject()));

                CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                customObjectsApi.createClusterCustomObject("robot.roboscale.io", "v1alpha1", "launchmanagers",
                                launchManagerObject.get("launchManager"), null, null, null);
        }

        public void createRobot(RequestRobot requestRobot, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                try {
                        System.out.println("Creating robot: " + requestRobot.getFleetProcessId());
                        Gson gson = new Gson();
                        // GET Robotics cloud instance
                        String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                        + requestRobot.getFleetProcessId()
                                        + "\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";
                        Response response;
                        try {
                                response = graphqlClient.executeSync(queryStr);
                        } catch (ExecutionException e) {
                                throw new ApplicationException("Error while executing request on graphql.");
                        }
                        javax.json.JsonObject data = response.getData();
                        JsonArray processInstances = data.getJsonArray("ProcessInstances");
                        ObjectMapper mapper = new ObjectMapper();
                        if (processInstances.size() == 0) {
                                throw new ApplicationException(
                                                "No process instance found with id: "
                                                                + requestRobot.getFleetProcessId());
                        }
                        System.out.println("Process Instances: " + processInstances);
                        JsonNode variables = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
                        System.out.println("Variables: " + variables);
                        JsonNode innerFleetNode = variables.get("requestFleet");

                        // GET Robotics Cloud's variables
                        String roboticsCloud = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                        + innerFleetNode.get("roboticsCloudProcessId").asText()
                                        + "\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";

                        Response responseRoboticsCloud;
                        try {
                                responseRoboticsCloud = graphqlClient.executeSync(roboticsCloud);
                        } catch (ExecutionException e) {
                                throw new ApplicationException("Error while executing request on graphql.");
                        }
                        javax.json.JsonObject responseRC = responseRoboticsCloud.getData();
                        JsonArray roboticsCloudInstances = responseRC.getJsonArray("ProcessInstances");
                        JsonNode roboticsCloudVariables = mapper
                                        .readTree(roboticsCloudInstances.getJsonObject(0).getString("variables"));

                        System.out.println("Robotics Cloud Variables: " + roboticsCloudVariables);
                        String bufferName = roboticsCloudVariables.get("bufferName").asText();
                        String providerName = roboticsCloudVariables.get("providerName").asText();
                        String regionName = roboticsCloudVariables.get("regionName").asText();
                        String superClusterName = roboticsCloudVariables.get("superClusterName").asText();
                        JsonNode organizationNode = roboticsCloudVariables.get("organization");
                        String organization = organizationNode.get("name").asText();
                        String teamId = roboticsCloudVariables.get("teamId").asText();
                        String cloudInstanceName = roboticsCloudVariables.get("cloudInstanceName").asText();

                        String fleetName;
                        System.out.println("innerFleetNode: " + innerFleetNode);
                        if (innerFleetNode.get("fleet").asText() == "null") {
                                JsonNode federatedFleetNode = innerFleetNode.get("federatedFleet");
                                fleetName = federatedFleetNode.get("name").asText();
                        } else if (innerFleetNode.get("federatedFleet").asText() == "null") {
                                JsonNode fleetNode = innerFleetNode.get("fleet");
                                fleetName = fleetNode.get("name").asText();
                        } else {
                                System.out.println("fefe: " + innerFleetNode.get("fleet").asText());
                                throw new ApplicationException("Fleet and federated fleet cannot be both present.");
                        }

                        // Parse Robot Object
                        String json = mapper.writeValueAsString(requestRobot);
                        JsonObject robotObject = gson.fromJson(json, JsonObject.class);
                        JsonObject labelsObject = new JsonObject();
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().add("labels",
                                        labelsObject);
                        JsonObject referenceObject = new JsonObject();
                        referenceObject.addProperty("name", fleetName + "-discovery");
                        referenceObject.addProperty("namespace", fleetName);

                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject().add("reference",
                                        referenceObject);
                        ApiClient superClusterApi = apiClientManager.getAdminApiClient(providerName,
                                        regionName, superClusterName);

                        StorageV1Api storageV1Api = new StorageV1Api(superClusterApi);
                        V1StorageClassList storageClassList = storageV1Api.listStorageClass(null, null, null, null,
                                        null, null,
                                        null, null, null, null);

                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject().get("storage")
                                        .getAsJsonObject().get("storageClassConfig")
                                        .getAsJsonObject()
                                        .addProperty("name",
                                                        storageClassList.getItems().get(0).getMetadata().getName());

                        // Update Labels
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject()
                                        .get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/organization",
                                                        organization);
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/team",
                                                        teamId);
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/region",
                                                        regionName);
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance",
                                                        bufferName);
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance-alias",
                                                        cloudInstanceName);
                        robotObject.get("robot").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                                        .getAsJsonObject().addProperty("robolaunch.io/fleet",
                                                        fleetName);

                        if (requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate().isVdiEnabled()) {
                                Integer sessionCount = requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate()
                                                .getRobotVDITemplate().getSessionCount();
                                String webRTCPorts = robotHelperRepository.getAvailablePortRange(sessionCount,
                                                providerName,
                                                regionName, superClusterName);
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
                        robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("workspaceManagerTemplate")
                                        .getAsJsonObject().get("workspaces").getAsJsonArray().forEach(workspace -> {
                                                JsonObject repositoriesObject = new JsonObject();
                                                workspace.getAsJsonObject().get("repositories").getAsJsonArray()
                                                                .forEach(repo -> {
                                                                        JsonObject cloudRepoInside = new JsonObject();
                                                                        cloudRepoInside.add("url",
                                                                                        repo.getAsJsonObject()
                                                                                                        .get("url"));
                                                                        cloudRepoInside.add("branch",
                                                                                        repo.getAsJsonObject()
                                                                                                        .get("branch"));

                                                                        repositoriesObject.add(
                                                                                        repo.getAsJsonObject()
                                                                                                        .get("name")
                                                                                                        .getAsString(),
                                                                                        cloudRepoInside);
                                                                });
                                                workspace.getAsJsonObject().remove("repositories");
                                                workspace.getAsJsonObject().add("repositories", repositoriesObject);
                                        });
                        JsonObject finalRobotObject = robotObject.get("robot").getAsJsonObject();

                        ApiClient robotsApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(
                                        bufferName,
                                        providerName, regionName, superClusterName);

                        CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                        customObjectsApi.createNamespacedCustomObject("robot.roboscale.io",
                                        "v1alpha1", "default",
                                        "robots", finalRobotObject, null, null, null);
                } catch (ApiException e) {
                        System.out.println("g: " + e.getCode());
                        System.out.println("g: " + e.getResponseBody());
                }

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
                                + " robolaunch.io/cloud-instancee=" + bufferName
                                + " robolaunch.io/cloud-instance-alias=" + cloudInstanceName
                                + " robolaunch.io/physical-instance= " + physicalInstanceName;

                script.concat(label + "\n");

                // String connectionHubOperator = "kubectl apply -f
                // https://github.com/robolaunch/connection-hub-operator/releases/download/v0.1.2/connection_hub_operator.yaml";

                return script;

        }

        public String getRobotStatus(String fleetProcessId, String robotName)
                        throws InterruptedException, JsonMappingException, JsonProcessingException {
                String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                + fleetProcessId
                                + "\"}}, {state: {equal: ACTIVE}}]}){id parentProcessInstanceId state variables}}";

                Response response;
                try {
                        response = graphqlClient.executeSync(queryStr);
                } catch (ExecutionException e) {
                        throw new ApplicationException("Error while executing request on graphql.");
                }
                javax.json.JsonObject data = response.getData();
                JsonArray processInstances = data.getJsonArray("ProcessInstances");
                ObjectMapper mapper = new ObjectMapper();
                if (processInstances.size() == 0) {
                        throw new ApplicationException(
                                        "No process instance found with id: "
                                                        + fleetProcessId);
                }
                System.out.println("milestone 1: " + processInstances);

                JsonNode variables = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));

                // GET Robotics cloud instance
                String roboticsCloudRequest = "query{ProcessInstances(where: {and: [{id: {equal:\""
                                + processInstances.getJsonObject(0).getString("parentProcessInstanceId")
                                + "\"}}, {state: {equal: ACTIVE}}]}){id parentProcessInstanceId state variables}}";
                Response responseRoboticsCloud;
                try {
                        responseRoboticsCloud = graphqlClient.executeSync(roboticsCloudRequest);
                } catch (ExecutionException e) {
                        throw new ApplicationException("Error while executing request on graphql.");
                }
                javax.json.JsonObject dataRoboticsCloud = responseRoboticsCloud.getData();
                JsonArray processInstancesRoboticsCloud = dataRoboticsCloud.getJsonArray("ProcessInstances");
                if (processInstances.size() == 0) {
                        throw new ApplicationException(
                                        "No process instance found with id: "
                                                        + fleetProcessId);
                }
                System.out.println(
                                "milestone 2" + processInstancesRoboticsCloud);

                JsonNode roboticsCloudVariables = mapper
                                .readTree(processInstancesRoboticsCloud.getJsonObject(0).getString("variables"));
                String bufferName = roboticsCloudVariables.get("bufferName").asText();
                String provider = roboticsCloudVariables.get("providerName").asText();
                String region = roboticsCloudVariables.get("regionName").asText();
                String superCluster = roboticsCloudVariables.get("superClusterName").asText();
                System.out.println(
                                "milestone 3" + bufferName);
                ApiClient vcClient;
                try {
                        vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(
                                        bufferName,
                                        provider, region, superCluster);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException
                                | ApiException | MinioException e) {
                        throw new ApplicationException("Error while getting virtual cluster client.");
                }

                DynamicKubernetesApi robotsClient = new DynamicKubernetesApi("robots.roboscale.io", "v1alpha1",
                                "robots",
                                vcClient);
                System.out.println("milestone 4");

                var robotList = robotsClient.list();
                for (var robot : robotList.getObject().getItems()) {
                        System.out.println("Robot: " + robot.getMetadata().getName());
                }

                System.out.println("milestone 4");

                return "pspd";
        }

}
