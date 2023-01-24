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
import org.robolaunch.model.request.RequestFleet;
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
            System.out.println("enters fleet not f: " + requestFleet);
            String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\""
                        + requestFleet.getRoboticsCloudProcessId()
                        + "\"}}, {state: {equal: ACTIVE}}]}){id state variables childProcessInstances{id processName state variables}}}";
            Response response = graphqlClient.executeSync(queryStr);
            javax.json.JsonObject data = response.getData();
            javax.json.JsonArray processInstances = data.getJsonArray("ProcessInstances");

            System.out.println("milestone 1");
            ObjectMapper mapper = new ObjectMapper();
            // GET ROBOTICS CLOUD VARIABLES
            if (processInstances.size() == 0) {
                  throw new ApplicationException(
                              "No active process instance found for process id: "
                                          + requestFleet.getRoboticsCloudProcessId());
            }
            JsonNode childNode = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
            String bufferName = childNode.get("bufferName").asText();
            String provider = childNode.get("providerName").asText();
            String region = childNode.get("regionName").asText();
            String superCluster = childNode.get("superClusterName").asText();
            String teamId = childNode.get("teamId").asText();
            String cloudInstanceName = childNode.get("cloudInstanceName").asText();
            JsonNode organizationNode = childNode.get("organization");
            System.out.println("milestone 2");

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
            System.out.println("milestone 3");

            JsonObject referenceObject = new JsonObject();
            referenceObject.addProperty("name",
                        requestFleet.getFleet().getName() + "-discovery");
            referenceObject.addProperty("namespace",
                        requestFleet.getFleet().getName());
            System.out.println("milestone 4");

            fleetObject.add("metadata", metadataObject);

            JsonObject specObject = new JsonObject();
            JsonObject specDiscoveryServerObject = new JsonObject();
            specDiscoveryServerObject.addProperty("cluster", bufferName);
            specDiscoveryServerObject.add("reference", referenceObject);
            specDiscoveryServerObject.addProperty("type", "Server");
            specDiscoveryServerObject.addProperty("hostname", "xxx");
            specDiscoveryServerObject.addProperty("subdomain", "yyy");
            System.out.println("milestone 5");

            specObject.add("discoveryServerTemplate", specDiscoveryServerObject);
            fleetObject.add("spec", specObject);

            // CREATE VIRTUAL CLUSTER API
            ApiClient vcApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                        provider, region, superCluster);
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcApi);
            System.out.println("milestone 6");

            customObjectsApi.createClusterCustomObject("fleet.roboscale.io", "v1alpha1", "fleets",
                        fleetObject,
                        null,
                        null, null);

            System.out.println("milestone 7");

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

            System.out.println("bufferName: " + bufferName);
            System.out.println("provider: " + provider);
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
            JsonObject templateSpecDiscoveryServerReferenceObject = new JsonObject();
            templateSpecDiscoveryServerReferenceObject.addProperty("name",
                        requestFleet.getFederatedFleet().getName() + "-discovery");
            templateSpecDiscoveryServerReferenceObject.addProperty("namespace",
                        requestFleet.getFederatedFleet().getName());

            JsonObject templateSpecDiscoveryServerObject = new JsonObject();
            templateSpecDiscoveryServerObject.addProperty("type", "Server");
            templateSpecDiscoveryServerObject.addProperty("cluster", bufferName);
            templateSpecDiscoveryServerObject.addProperty("hostname", "xxx");
            templateSpecDiscoveryServerObject.addProperty("subdomain", "yyy");

            templateSpecDiscoveryServerObject.add("reference", templateSpecDiscoveryServerReferenceObject);
            templateSpecObject.add("discoveryServerTemplate", templateSpecDiscoveryServerObject);
            // template spec discoveryServerTemplate END

            templateSpecObject.addProperty("hybrid", true);
            JsonArray templateSpecInstancesObject = new JsonArray();
            requestFleet.getFederatedFleet().getClusters().forEach(cluster -> {
                  System.out.println("cl: " + cluster);
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
                  if (cluster.startsWith("vc-") || cluster.equals("cluster")) {
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
                        System.out.println("cluster nameeeeeeee: " + cluster);
                        labelsObject.addProperty("robolaunch.io/physical-instance",
                                    cluster);

                        overrideClusterObject2.add("value", labelsObject);
                        overrideClustersObject.add(overrideClusterObject2);

                        overrideObject.add("clusterOverrides", overrideClustersObject);

                        overridesObject.add(overrideObject);
                  }

            });
            specObject.add("overrides", overridesObject);
            federatedFleetObject.add("spec", specObject);

            System.out.println("federated fleet object: " + federatedFleetObject);
            System.out.println("bf: " + bufferName);
            System.out.println("provider: " + provider);
            System.out.println("region: " + region);
            System.out.println("superCluster: " + superCluster);
            // CREATE VIRTUAL CLUSTER API
            ApiClient vcApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                        provider, region, superCluster);

            System.out.println("df: " + federatedFleetObject);
            System.out.println("bebd");
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcApi);
            customObjectsApi.createClusterCustomObject("types.kubefed.io", "v1beta1", "federatedfleets",
                        federatedFleetObject,
                        null,
                        null, null);
            System.out.println("bebd");

      }
}