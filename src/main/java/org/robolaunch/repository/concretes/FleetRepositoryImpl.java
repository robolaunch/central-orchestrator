package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.request.RequestFleet;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.FleetRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.minio.errors.MinioException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class FleetRepositoryImpl implements FleetRepository {
      DynamicGraphQLClient graphqlClient;

      @Inject
      CloudInstanceHelperRepository cloudInstanceHelperRepository;

      @Inject
      ApiClientManager apiClientManager;

      @ConfigProperty(name = "kogito.dataindex.http.url")
      String kogitoDataIndexUrl;

      @PostConstruct
      public void initializeServices() throws IOException {
            graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
                        .url(kogitoDataIndexUrl + "/graphql")
                        .build();
      }

      @Override
      public void createFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException, InterruptedException, MinioException, ExecutionException {
            System.out.println("createFleet rc pid: " + requestFleet.getRoboticsCloudProcessId());
            String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                        + requestFleet.getRoboticsCloudProcessId()
                        + "\"}}, {state: {equal: ACTIVE}}]}){id state variables childProcessInstances{id processName state variables}}}";
            Response response = graphqlClient.executeSync(queryStr);
            javax.json.JsonObject data = response.getData();
            javax.json.JsonArray processInstances = data.getJsonArray("ProcessInstances");
            ObjectMapper mapper = new ObjectMapper();
            // GET ROBOTICS CLOUD VARIABLES
            if (processInstances.size() == 0) {
                  throw new ApplicationException(
                              "No active process instance found for process id: "
                                          + requestFleet.getRoboticsCloudProcessId());
            }
            JsonNode childNode = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
            System.out.println("childNode: " + childNode);
            String bufferName = childNode.get("bufferName").asText();
            String provider = childNode.get("providerName").asText();
            String region = childNode.get("regionName").asText();
            String superCluster = childNode.get("superClusterName").asText();
            String teamId = childNode.get("teamId").asText();
            String cloudInstanceName = childNode.get("cloudInstanceName").asText();
            JsonNode organizationNode = childNode.get("organization");

            JsonObject fleetObject = new JsonObject();
            fleetObject.addProperty("apiVersion", "fleet.roboscale.io/v1alpha1");
            fleetObject.addProperty("kind", "Fleet");
            JsonObject metadataObject = new JsonObject();
            metadataObject.addProperty("name", requestFleet.getFleet().getName());

            JsonObject labelsObject = new JsonObject();
            labelsObject
                        .getAsJsonObject().addProperty("robolaunch.io/organization",
                                    organizationNode.get("name").asText());
            labelsObject
                        .getAsJsonObject().addProperty("robolaunch.io/team",
                                    teamId);
            labelsObject
                        .getAsJsonObject().addProperty("robolaunch.io/region",
                                    region);
            labelsObject
                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance",
                                    bufferName);
            labelsObject
                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance-alias",
                                    cloudInstanceName);
            metadataObject.add("labels", labelsObject);

            fleetObject.add("metadata", metadataObject);
            System.out.println("metadata 1.");

            JsonObject specObject = new JsonObject();
            JsonObject specDiscoveryServerObject = new JsonObject();
            specDiscoveryServerObject.addProperty("cluster", bufferName);
            specObject.addProperty("type", "Server");
            specObject.addProperty("hostname", "xxx");
            specObject.addProperty("subdomain", "yyy");
            specObject.add("discoveryServerTemplate", specDiscoveryServerObject);
            fleetObject.add("spec", specObject);
            System.out.println("metadata 2.");

            // CREATE VIRTUAL CLUSTER API
            ApiClient vcApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                        provider, region, superCluster);
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcApi);

            customObjectsApi.createClusterCustomObject("fleet.roboscale.io", "v1alpha1", "fleets",
                        fleetObject,
                        null,
                        null, null);

      }

      @Override
      public void createFederatedFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException,
                  InterruptedException, MinioException, ExecutionException {
            System.out.println("createFederatedFleet rc pid: " + requestFleet.getRoboticsCloudProcessId());
            // GET ROBOTICS CLOUD PROCESS INSTANCE VARIABLE
            String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                        + requestFleet.getRoboticsCloudProcessId()
                        + "\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";
            Response response = graphqlClient.executeSync(queryStr);
            javax.json.JsonObject data = response.getData();
            javax.json.JsonArray processInstances = data.getJsonArray("ProcessInstances");
            ObjectMapper mapper = new ObjectMapper();
            // GET ROBOTICS CLOUD VARIABLES
            JsonNode childNode = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
            String bufferName = childNode.get("bufferName").asText();
            String provider = childNode.get("providerName").asText();
            String region = childNode.get("regionName").asText();
            String superCluster = childNode.get("superClusterName").asText();
            String teamId = childNode.get("teamId").asText();
            String cloudInstanceName = childNode.get("cloudInstanceName").asText();
            JsonNode organizationNode = childNode.get("organization");
            System.out.println("childnode: " + childNode);
            // CREATE OBJECT
            JsonObject federatedFleetObject = new JsonObject();
            federatedFleetObject.addProperty("apiVersion", "types.kubefed.io/v1beta1");
            federatedFleetObject.addProperty("kind", "FederatedFleet");
            // metadata
            JsonObject metadataObject = new JsonObject();
            metadataObject.addProperty("name", requestFleet.getFederatedFleet().getName());
            federatedFleetObject.add("metadata", metadataObject);

            // spec
            JsonObject specObject = new JsonObject();
            // template
            JsonObject templateObject = new JsonObject();
            // template metadata
            JsonObject templateMetadataObject = new JsonObject();
            templateMetadataObject.addProperty("name", requestFleet.getFederatedFleet().getName());
            templateObject.add("metadata", templateMetadataObject);
            // template metadata END
            // template spec
            JsonObject templateSpecObject = new JsonObject();
            // template spec discoveryServerTemplate
            JsonObject templateSpecDiscoveryServerObject = new JsonObject();
            templateSpecDiscoveryServerObject.addProperty("type", "Server");
            templateSpecDiscoveryServerObject.addProperty("cluster", bufferName);
            JsonObject templateSpecDiscoveryServerReferenceObject = new JsonObject();
            templateSpecDiscoveryServerReferenceObject.addProperty("name",
                        requestFleet.getFederatedFleet().getName() + "-discovery");
            templateSpecDiscoveryServerReferenceObject.addProperty("namespace",
                        requestFleet.getFederatedFleet().getName());
            templateSpecDiscoveryServerObject.add("reference", templateSpecDiscoveryServerReferenceObject);
            templateSpecDiscoveryServerObject.addProperty("hostname", "xxx");
            templateSpecDiscoveryServerObject.addProperty("subdomain", "yyy");
            templateSpecObject.add("discoveryServerTemplate", templateSpecDiscoveryServerObject);
            // template spec discoveryServerTemplate END

            templateSpecObject.addProperty("hybrid", true);
            JsonArray templateSpecInstancesObject = new JsonArray();
            requestFleet.getFederatedFleet().getClusters().forEach(cluster -> {
                  templateSpecInstancesObject.add(cluster);
            });
            templateSpecObject.add("instances", templateSpecInstancesObject);
            templateObject.add("spec", templateSpecObject);
            specObject.add("template", templateObject);

            // template END
            // placement
            JsonObject placementObject = new JsonObject();
            JsonArray placementClustersObject = new JsonArray();
            requestFleet.getFederatedFleet().getClusters().forEach(cluster -> {
                  JsonObject placementClusterObject = new JsonObject();
                  placementClusterObject.addProperty("name", cluster);
                  placementClustersObject.add(placementClusterObject);
            });
            placementObject.add("clusters", placementClustersObject);
            specObject.add("placement", placementObject);

            // placement END
            // overrides
            JsonArray overridesObject = new JsonArray();
            requestFleet.getFederatedFleet().getClusters().forEach(cluster -> {
                  JsonObject overrideObject = new JsonObject();
                  overrideObject.addProperty("clusterName", cluster);
                  JsonArray overrideClustersObject = new JsonArray();
                  if (cluster.startsWith("vc-")) {
                        JsonObject overrideClusterObject = new JsonObject();
                        overrideClusterObject.addProperty("path", "/spec/discoveryServerTemplate/type");
                        overrideClusterObject.addProperty("value", "Server");
                        overrideClustersObject.add(overrideClusterObject);

                        JsonObject overrideClusterObject2 = new JsonObject();
                        overrideClusterObject2.addProperty("path", "/metadata/labels");
                        overrideClusterObject2.addProperty("op", "add");

                        // ADD LABELS TO FLEET
                        JsonObject labelsObject = new JsonObject();

                        labelsObject
                                    .addProperty("robolaunch.io/organization",
                                                organizationNode.get("name").asText());
                        labelsObject.addProperty("robolaunch.io/team",
                                    teamId);

                        labelsObject.addProperty("robolaunch.io/region",
                                    region);
                        labelsObject.addProperty("robolaunch.io/cloud-instance",
                                    bufferName);
                        labelsObject.addProperty("robolaunch.io/cloud-instance-alias",
                                    cloudInstanceName);
                        overrideClusterObject2.add("value", labelsObject);
                        overrideClustersObject.add(overrideClusterObject2);

                        overrideObject.add("clusterOverrides", overrideClustersObject);

                        overridesObject.add(overrideObject);
                  } else {
                        JsonObject overrideClusterObject = new JsonObject();
                        overrideClusterObject.addProperty("path", "/spec/discoveryServerTemplate/type");
                        overrideClusterObject.addProperty("value", "Client");
                        overrideClustersObject.add(overrideClusterObject);

                        JsonObject overrideClusterObject2 = new JsonObject();
                        overrideClusterObject2.addProperty("path", "/metadata/labels");
                        overrideClusterObject2.addProperty("op", "add");

                        // ADD LABELS TO FLEET
                        JsonObject labelsObject = new JsonObject();
                        labelsObject.add("labels",
                                    labelsObject);

                        labelsObject
                                    .addProperty("robolaunch.io/organization",
                                                organizationNode.get("name").asText());
                        labelsObject.addProperty("robolaunch.io/team",
                                    teamId);
                        labelsObject.addProperty("robolaunch.io/region",
                                    region);
                        labelsObject.addProperty("robolaunch.io/cloud-instance",
                                    bufferName);
                        labelsObject.addProperty("robolaunch.io/cloud-instance-alias",
                                    cloudInstanceName);
                        // labelsObject.addProperty("robolaunch.io/physical-instance",
                        // requestFederatedFleet.getName());

                        overrideClusterObject2.add("value", labelsObject);
                        overrideClustersObject.add(overrideClusterObject2);

                        overrideObject.add("clusterOverrides", overrideClustersObject);

                        overridesObject.add(overrideObject);
                  }

            });
            specObject.add("overrides", overridesObject);
            federatedFleetObject.add("spec", specObject);

            System.out.println("federated fleet object: " + federatedFleetObject);

            // CREATE VIRTUAL CLUSTER API
            ApiClient vcApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                        provider, region, superCluster);

            CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcApi);
            customObjectsApi.createClusterCustomObject("types.kubefed.io", "v1beta1", "federatedfleets",
                        federatedFleetObject,
                        null,
                        null, null);
            System.out.println("yehu");

      }
}