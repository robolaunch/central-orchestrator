package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.request.RequestBuildManager;
import org.robolaunch.model.request.RequestLaunchManager;
import org.robolaunch.model.request.RequestRobot;
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

                System.out.println("createRobotBuildManager");
                String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"roboticsCloud\"}}, {state: {equal: ACTIVE}}]}){id state variables childProcessInstances{id state variables}}}";

                Response response = graphqlClient.executeSync(queryStr);
                javax.json.JsonObject data = response.getData();
                JsonArray processInstances = data.getJsonArray("ProcessInstances");
                ObjectMapper mapper = new ObjectMapper();

                for (int i = 0; i < processInstances.size(); i++) {
                        JsonNode variablesRC = mapper
                                        .readTree(processInstances.getJsonObject(i).getString("variables"));
                        JsonNode organizationNode = variablesRC.get("organization");
                        if (organizationNode.get("name").asText().equals(robotBuildManager.getOrganization().getName())
                                        && variablesRC.get("teamId").asText().equals(robotBuildManager.getTeamId())
                                        && variablesRC.get("cloudInstanceName").asText()
                                                        .equals(robotBuildManager.getRoboticsCloudName())) {
                                String bufferName = variablesRC.get("bufferName").asText();
                                String providerName = variablesRC.get("providerName").asText();
                                String regionName = variablesRC.get("regionName").asText();
                                String superClusterName = variablesRC.get("superClusterName").asText();

                                ApiClient vcClient = cloudInstanceHelperRepository
                                                .getVirtualClusterClientWithBufferName(bufferName,
                                                                providerName, regionName,
                                                                superClusterName);

                                JsonObject buildManagerObject = new JsonObject();
                                buildManagerObject.addProperty("apiVersion",
                                                robotBuildManager.getBuildManager().getApiVersion());
                                buildManagerObject.addProperty("kind", robotBuildManager.getBuildManager().getKind());
                                JsonObject metadataObject = new JsonObject();
                                metadataObject.addProperty("name",
                                                robotBuildManager.getBuildManager().getMetadata().getName());
                                JsonObject labelsObject = new JsonObject();
                                labelsObject.addProperty("robolaunch.io/target-robot",
                                                robotBuildManager.getTargetRobot());
                                metadataObject.add("labels", labelsObject);
                                buildManagerObject.add("metadata", metadataObject);
                                // metadata done
                                System.out.println("m 7");

                                JsonObject specObject = new JsonObject();
                                com.google.gson.JsonArray stepsArray = new com.google.gson.JsonArray();

                                robotBuildManager.getBuildManager().getSteps().forEach(step -> {
                                        JsonObject stepObject = new JsonObject();
                                        stepObject.addProperty("name", step.getName());
                                        stepObject.addProperty("workspace", step.getWorkspace());
                                        if (step.getCommand() != null) {
                                                stepObject.addProperty("command", step.getCommand());
                                        } else if (step.getScript() != null) {
                                                stepObject.addProperty("script", step.getScript());
                                        }

                                        stepsArray.add(stepObject);
                                });
                                specObject.add("steps", stepsArray);
                                buildManagerObject.add("spec", specObject);
                                System.out.println("m 8");

                                System.out.println("bmo: " + buildManagerObject);

                                CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcClient);
                                customObjectsApi.createClusterCustomObject("robot.roboscale.io", "v1alpha1",
                                                "buildmanagers",
                                                buildManagerObject, null, null, null);
                                System.out.println("robot build manager created");
                                return;
                        }

                }

        }

        @Override
        public void createFederatedRobotBuildManager(RequestBuildManager robotBuildManager, String token) {

        }

        @Override
        public void createRobotLaunchManager(RequestLaunchManager robotLaunchManager, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException, ApiException, InterruptedException,
                        ExecutionException {
                String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"roboticsCloud\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";

                Response response = graphqlClient.executeSync(queryStr);
                javax.json.JsonObject data = response.getData();
                JsonArray processInstances = data.getJsonArray("ProcessInstances");

                ObjectMapper mapper = new ObjectMapper();
                JsonArray childProcessInstances = processInstances.getJsonObject(0)
                                .getJsonArray("childProcessInstances");
                JsonNode RCVariables = mapper.readTree(childProcessInstances.getJsonObject(0).getString("variables"));

                String bufferName = RCVariables.get("bufferName").asText();
                String providerName = RCVariables.get("providerName").asText();
                String regionName = RCVariables.get("regionName").asText();
                String superClusterName = RCVariables.get("superClusterName").asText();

                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                                providerName, regionName,
                                superClusterName);

                JsonObject launchManagerObject = new JsonObject();

                launchManagerObject.addProperty("apiVersion", robotLaunchManager.getLaunchManager().getApiVersion());
                launchManagerObject.addProperty("kind", robotLaunchManager.getLaunchManager().getKind());

                JsonObject metadataObject = new JsonObject();
                metadataObject.addProperty("name", robotLaunchManager.getLaunchManager().getMetadata().getName());
                JsonObject labelsObject = new JsonObject();
                labelsObject.addProperty("robolaunch.io/target-robot", robotLaunchManager.getTargetRobot());
                labelsObject.addProperty("robolaunch.io/target-vdi", robotLaunchManager.getTargetRobot() + "-dev-vdi");
                metadataObject.add("labels", labelsObject);

                launchManagerObject.add("metadata", metadataObject);
                // metadata done

                JsonObject specObject = new JsonObject();
                specObject.addProperty("display", robotLaunchManager.isVdiEnabled());
                JsonObject specLaunchObject = new JsonObject();
                robotLaunchManager.getLaunchManager().getLaunch().forEach(launchObj -> {
                        JsonObject launchInside = new JsonObject();

                        JsonObject launchInsideLabels = new JsonObject();
                        launchInsideLabels.addProperty("robolaunch.io/cloud-instance",
                                        robotLaunchManager.getBufferName());
                        launchInside.add("selector", launchInsideLabels);
                        launchInside.addProperty("workspace", launchObj.getWorkspace());
                        launchInside.addProperty("repository", launchObj.getRepository());
                        launchInside.addProperty("namespacing", launchObj.isNamespacing());
                        launchInside.addProperty("launchFilePath", launchObj.getLaunchFilePath());
                        specLaunchObject.add(launchObj.getName(), launchInside);
                });
                specObject.add("launch", specLaunchObject);

                launchManagerObject.add("spec", specObject);

                CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcClient);
                customObjectsApi.createClusterCustomObject("robot.roboscale.io", "v1alpha1", "launchmanagers",
                                launchManagerObject, null, null, null);
        }

        @Override
        public void createFederatedRobotLaunchManager(RequestLaunchManager robotLaunchManager, String token) {

        }

        @Override
        public void createRobot(RequestRobot requestRobot, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {
                Gson gson = new Gson();
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
                JsonNode variables = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
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

                String bufferName = roboticsCloudVariables.get("bufferName").asText();
                String providerName = roboticsCloudVariables.get("providerName").asText();
                String regionName = roboticsCloudVariables.get("regionName").asText();
                String superClusterName = roboticsCloudVariables.get("superClusterName").asText();
                JsonNode organizationNode = roboticsCloudVariables.get("organization");
                String organization = organizationNode.get("name").asText();
                String teamId = roboticsCloudVariables.get("teamId").asText();
                String cloudInstanceName = roboticsCloudVariables.get("cloudInstanceName").asText();

                String fleetName;
                if (innerFleetNode.get("fleet").asText() == "null") {
                        JsonNode federatedFleetNode = innerFleetNode.get("federatedFleet");
                        fleetName = federatedFleetNode.get("name").asText();
                } else if (innerFleetNode.get("federatedFleet").asText() == "null") {
                        JsonNode fleetNode = innerFleetNode.get("fleet");
                        fleetName = fleetNode.get("name").asText();
                } else {
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

                robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("discoveryServerTemplate").getAsJsonObject()
                                .add("reference", referenceObject);
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

                if (requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate().isIdeEnabled()) {
                        if (requestRobot.getRobot().getSpec().getRobotDevSuiteTemplate().isVdiEnabled()) {
                                robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("robotIDETemplate").getAsJsonObject()
                                                .addProperty("display", true);
                        } else {
                                robotObject.get("robot").getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("robotIDETemplate").getAsJsonObject()
                                                .addProperty("display", false);
                        }

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

        }

        // CREATE FEDERATED ROBOT
        public void createFederatedRobot(RequestRobot requestRobot, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException {

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
                JsonNode variables = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
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

                // GET ROBOTICS CLOUD VARIABLES
                String bufferName = roboticsCloudVariables.get("bufferName").asText();
                String providerName = roboticsCloudVariables.get("providerName").asText();
                String regionName = roboticsCloudVariables.get("regionName").asText();
                String superClusterName = roboticsCloudVariables.get("superClusterName").asText();
                JsonNode organizationNode = roboticsCloudVariables.get("organization");

                String teamId = roboticsCloudVariables.get("teamId").asText();
                String cloudInstanceName = roboticsCloudVariables.get("cloudInstanceName").asText();

                String fleetName;
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
                // Start Parsing Robot Object
                String json = mapper.writeValueAsString(requestRobot);
                JsonObject robotObject = gson.fromJson(json, JsonObject.class);
                ApiClient superClusterApi = apiClientManager.getAdminApiClient(providerName,
                                regionName, superClusterName);

                StorageV1Api storageV1Api = new StorageV1Api(superClusterApi);
                V1StorageClassList storageClassList = storageV1Api.listStorageClass(null, null, null, null,
                                null, null,
                                null, null, null, null);

                JsonObject specTemplateObject = new JsonObject();
                specTemplateObject.add("metadata", robotObject.get("federatedRobot").getAsJsonObject()
                                .get("spec").getAsJsonObject().get("metadata"));
                specTemplateObject.add("spec", robotObject.get("federatedRobot").getAsJsonObject().get("spec")
                                .getAsJsonObject().get("spec"));
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .add("template", specTemplateObject);

                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject().get("template")
                                .getAsJsonObject().get("metadata")
                                .getAsJsonObject()
                                .addProperty("namespace", fleetName);

                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("template").getAsJsonObject().get("spec").getAsJsonObject().get("storage")
                                .getAsJsonObject().get("storageClassConfig")
                                .getAsJsonObject()
                                .addProperty("name",
                                                storageClassList.getItems().get(0).getMetadata().getName());

                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .remove("metadata");
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .remove("spec");

                if (requestRobot.getFederatedRobot().getSpec().getSpec()
                                .getRobotDevSuiteTemplate().isVdiEnabled()) {
                        Integer sessionCount = requestRobot.getFederatedRobot().getSpec()
                                        .getSpec().getRobotDevSuiteTemplate()
                                        .getRobotVDITemplate().getSessionCount();
                        String webRTCPorts = robotHelperRepository.getAvailablePortRange(sessionCount,
                                        providerName,
                                        regionName, superClusterName);
                        robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .remove("sessionCount");
                        robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .addProperty("webrtcPortRange", webRTCPorts);

                } else {
                        robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                        .get("robotDevSuiteTemplate")
                                        .getAsJsonObject().get("robotVDITemplate").getAsJsonObject()
                                        .remove("sessionCount");
                }

                if (requestRobot.getFederatedRobot().getSpec().getSpec().getRobotDevSuiteTemplate()
                                .isIdeEnabled()) {
                        if (requestRobot.getFederatedRobot().getSpec().getSpec().getRobotDevSuiteTemplate()
                                        .isVdiEnabled()) {
                                robotObject.get("federatedRobot").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject()
                                                .get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("robotIDETemplate").getAsJsonObject()
                                                .addProperty("display", true);
                        } else {
                                robotObject.get("federatedRobot").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject()
                                                .get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("robotIDETemplate").getAsJsonObject()
                                                .addProperty("display", false);
                        }

                }

                // Update Repositories
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("template").getAsJsonObject().get("spec").getAsJsonObject()
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

                // Add Placement
                JsonObject placementObject = new JsonObject();
                com.google.gson.JsonArray clustersArray = new com.google.gson.JsonArray();
                com.google.gson.JsonArray objectClusters = robotObject.get("federatedRobot").getAsJsonObject()
                                .get("spec").getAsJsonObject()
                                .get("clusters")
                                .getAsJsonArray();
                for (int i = 0; i < objectClusters.size(); i++) {
                        JsonObject clusterObject = new JsonObject();
                        clusterObject.addProperty("name", objectClusters.get(i).getAsString());
                        clustersArray.add(clusterObject);
                }
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .add("placement", placementObject);
                placementObject.add("clusters", clustersArray);

                // Add Overrides
                com.google.gson.JsonArray overridesArray = new com.google.gson.JsonArray();

                for (int i = 0; i < objectClusters.size(); i++) {
                        JsonObject overrideObject = new JsonObject();
                        if (objectClusters.get(i).getAsString().startsWith("vc-") || objectClusters
                                        .get(i).getAsString().equals("cluster")) {
                                overrideObject.addProperty("clusterName", objectClusters.get(i).getAsString());
                                com.google.gson.JsonArray clusterOverrides = new com.google.gson.JsonArray();
                                JsonObject clusterOverrideObject = new JsonObject();
                                clusterOverrideObject.addProperty("path", "/metadata/labels");
                                clusterOverrideObject.addProperty("op", "add");
                                JsonObject valueObject = new JsonObject();

                                valueObject
                                                .addProperty("robolaunch.io/organization",
                                                                organizationNode.get("name").asText());
                                valueObject.addProperty("robolaunch.io/team",
                                                teamId);
                                valueObject.addProperty("robolaunch.io/region",
                                                regionName);
                                valueObject.addProperty("robolaunch.io/cloud-instance",
                                                bufferName);
                                valueObject.addProperty("robolaunch.io/cloud-instance-alias",
                                                cloudInstanceName);

                                valueObject.addProperty("robolaunch.io/fleet",
                                                fleetName);
                                clusterOverrideObject.add("value", valueObject);
                                clusterOverrides.add(clusterOverrideObject);

                                JsonObject clusterOverrideObject2 = new JsonObject();
                                clusterOverrideObject2.addProperty("path",
                                                "/spec/storage/storageClassConfig/name");
                                clusterOverrideObject2.addProperty("value",
                                                storageClassList.getItems().get(0).getMetadata().getName());
                                clusterOverrides.add(clusterOverrideObject2);

                                // n. override ...
                                JsonObject clusterOverrideObject3 = new JsonObject();
                                clusterOverrideObject3.addProperty("path",
                                                "/spec/rosBridgeTemplate/ros/enabled");
                                clusterOverrideObject3.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("rosBridgeTemplate")
                                                .getAsJsonObject().get("ros")
                                                .getAsJsonObject().get("enabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject3);

                                JsonObject clusterOverrideObject4 = new JsonObject();
                                clusterOverrideObject4.addProperty("path",
                                                "/spec/rosBridgeTemplate/ros2/enabled");
                                clusterOverrideObject4.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("rosBridgeTemplate")
                                                .getAsJsonObject().get("ros2")
                                                .getAsJsonObject().get("enabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject4);

                                JsonObject clusterOverrideObject5 = new JsonObject();
                                clusterOverrideObject5.addProperty("path",
                                                "/spec/robotDevSuiteTemplate/vdiEnabled");
                                clusterOverrideObject5.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("vdiEnabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject5);

                                JsonObject clusterOverrideObject6 = new JsonObject();
                                clusterOverrideObject6.addProperty("path",
                                                "/spec/robotDevSuiteTemplate/ideEnabled");
                                clusterOverrideObject6.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("ideEnabled")
                                                .getAsBoolean());

                                clusterOverrides.add(clusterOverrideObject6);

                                overrideObject.add("clusterOverrides", clusterOverrides);
                        } else {
                                overrideObject.addProperty("clusterName", objectClusters.get(i).getAsString());
                                com.google.gson.JsonArray clusterOverrides = new com.google.gson.JsonArray();
                                // First Override
                                JsonObject clusterOverrideObject = new JsonObject();
                                clusterOverrideObject.addProperty("path", "/metadata/labels");
                                clusterOverrideObject.addProperty("op", "add");
                                JsonObject valueObject = new JsonObject();
                                valueObject.addProperty("robolaunch.io/organization",
                                                organizationNode.get("name").asText());
                                valueObject.addProperty("robolaunch.io/team",
                                                teamId);
                                valueObject.addProperty("robolaunch.io/region",
                                                regionName);
                                valueObject.addProperty("robolaunch.io/cloud-instance",
                                                bufferName);
                                valueObject.addProperty("robolaunch.io/cloud-instance-alias",
                                                cloudInstanceName);
                                valueObject.addProperty("robolaunch.io/physical-instance",
                                                objectClusters.get(i).getAsString());
                                valueObject.addProperty("robolaunch.io/fleet",
                                                fleetName);
                                clusterOverrideObject.add("value", valueObject);
                                clusterOverrides.add(clusterOverrideObject);

                                JsonObject clusterOverrideObject2 = new JsonObject();
                                clusterOverrideObject2.addProperty("path",
                                                "/spec/storage/storageClassConfig/name");
                                clusterOverrideObject2.addProperty("value", "openebs-hostpath");
                                clusterOverrides.add(clusterOverrideObject2);

                                JsonObject clusterOverrideObject3 = new JsonObject();
                                clusterOverrideObject3.addProperty("path",
                                                "/spec/rosBridgeTemplate/ros/enabled");
                                clusterOverrideObject3.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("rosBridgeTemplate")
                                                .getAsJsonObject().get("ros")
                                                .getAsJsonObject().get("enabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject3);

                                JsonObject clusterOverrideObject4 = new JsonObject();
                                clusterOverrideObject4.addProperty("path",
                                                "/spec/rosBridgeTemplate/ros2/enabled");
                                clusterOverrideObject4.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("rosBridgeTemplate")
                                                .getAsJsonObject().get("ros2")
                                                .getAsJsonObject().get("enabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject4);

                                JsonObject clusterOverrideObject5 = new JsonObject();
                                clusterOverrideObject5.addProperty("path",
                                                "/spec/robotDevSuiteTemplate/vdiEnabled");
                                clusterOverrideObject5.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("vdiEnabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject5);

                                JsonObject clusterOverrideObject6 = new JsonObject();
                                clusterOverrideObject6.addProperty("path",
                                                "/spec/robotDevSuiteTemplate/ideEnabled");
                                clusterOverrideObject6.addProperty("value", robotObject.get("federatedRobot")
                                                .getAsJsonObject().get("spec").getAsJsonObject()
                                                .get("template").getAsJsonObject().get("spec")
                                                .getAsJsonObject().get("robotDevSuiteTemplate")
                                                .getAsJsonObject().get("ideEnabled")
                                                .getAsBoolean());
                                clusterOverrides.add(clusterOverrideObject6);
                                overrideObject.add("clusterOverrides", clusterOverrides);

                        }
                        overridesArray.add(overrideObject);

                }
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .add("overrides", overridesArray);

                JsonObject finalRobotObject = robotObject.get("federatedRobot").getAsJsonObject();

                ApiClient robotsApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(
                                bufferName,
                                providerName, regionName, superClusterName);

                // Remove unnecessary lines.
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .remove("clusters");
                robotObject.get("federatedRobot").getAsJsonObject().get("spec").getAsJsonObject()
                                .get("template").getAsJsonObject().get("spec").getAsJsonObject()
                                .remove("clusters");

                System.out.println("final robot object: " + finalRobotObject);
                CustomObjectsApi customObjectsApi = new CustomObjectsApi(robotsApi);
                customObjectsApi.createNamespacedCustomObject("types.kubefed.io",
                                "v1beta1", fleetName,
                                "federatedrobots", finalRobotObject, null, null, null);

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
