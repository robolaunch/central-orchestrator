package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Provider;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.RoboticsCloudKubernetes;
import org.robolaunch.models.SuperClusterKubernetes;
import org.robolaunch.repository.abstracts.KubernetesRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.minio.errors.MinioException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.graphql.execution.ExecutionException;

@ApplicationScoped
public class KubernetesRepositoryImpl implements KubernetesRepository {
  DynamicGraphQLClient graphqlClient;

  @ConfigProperty(name = "kogito.dataindex.http.url")
  String kogitoDataIndexUrl;

  @Inject
  ApiClientManager apiClientManager;

  @PostConstruct
  public void initializeServices() throws IOException {
    graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(kogitoDataIndexUrl + "/graphql")
        .build();
  }

  public String getProviderId(String provider)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"provider\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    for (int i = 0; i < processInstances.size(); i++) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode;
      try {
        rootNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
        if (rootNode.get("providerName").asText().equals(provider)) {
          return processInstances.getJsonObject(i).getString("id");
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public String getRegionId(String provider, String region)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"region\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    for (int i = 0; i < processInstances.size(); i++) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode;
      try {
        rootNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
        if (rootNode.get("providerName").asText().equals(provider)
            && rootNode.get("regionName").asText().equals(region)) {
          return processInstances.getJsonObject(i).getString("id");
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public String getSuperClusterProcessId(String provider, String region, String superCluster)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"superCluster\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    String processId = "";

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    for (int i = 0; i < processInstances.size(); i++) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode;
      try {
        rootNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
        if (rootNode.get("providerName").asText().equals(provider) && rootNode.get("regionName").asText().equals(region)
            && rootNode.get("superClusterName").asText().equals(superCluster)) {
          processId = processInstances.getJsonObject(i).getString("id");
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    return processId;
  }

  @Override
  public Integer getCurrentBufferingCountOfType(String instanceType, String provider, String region,
      String superCluster)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException, JsonMappingException,
      JsonProcessingException {
    Integer counter = 0;
    String processId = getSuperClusterProcessId(provider, region, superCluster);
    String queryStr = "query{ProcessInstances(where: {id: {equal:\"" + processId
        + "\"}}){id childProcessInstances{id processName variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    ObjectMapper mapper = new ObjectMapper();
    if (processInstances != null) {
      JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

      for (int i = 0; i < childProcessInstances.size(); i++) {
        if (childProcessInstances.getJsonObject(i).getString("processName").equals("bufferCloudInstance")) {
          JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
          if (childNode.get("instanceType").asText().equals(instanceType)
              && childNode.get("status").asText().equals("Creating")) {
            counter++;
          }

        }
      }
    }

    return counter;
  }

  @Override
  public Integer getCurrentBufferedCountOfType(String instanceType, String provider, String region,
      String superCluster) throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
      ApiException, InterruptedException, MinioException {
    DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region, superCluster);

    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector(
        "buffered=true, !robolaunch.io/organization, robolaunch.io/instance-type=" + instanceType);
    List<DynamicKubernetesObject> vcs = virtualClustersApi.list(listOptions).getObject().getItems();
    System.out.println("VCs: " + vcs.size());
    Integer counter = 0;
    for (DynamicKubernetesObject vc : vcs) {
      if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString().equals("Running")) {
        counter++;
      }
    }
    return counter;
  }

  @Override
  public Boolean providerExists(String provider) throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"provider\"}}){id variables}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    // Iterate over processInstances and check if providerName is equal to provider
    for (int i = 0; i < processInstances.size(); i++) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode childNode;
      try {
        childNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
        if (childNode.get("providerName").asText().equals(provider)) {
          return true;
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    return false;
  }

  @Override
  public Boolean regionExists(String provider, String region)
      throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"provider\"}}){id variables childProcessInstances{id processName variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    // Iterate over processInstances and check if providerName is equal to provider
    for (int i = 0; i < processInstances.size(); i++) {
      ObjectMapper mapper = new ObjectMapper();
      JsonArray childProcessInstances = processInstances.getJsonObject(i).getJsonArray("childProcessInstances");
      for (int j = 0; j < childProcessInstances.size(); j++) {
        if (childProcessInstances.getJsonObject(j).getString("processName").equals("region")) {
          JsonNode childNode;
          try {
            childNode = mapper.readTree(childProcessInstances.getJsonObject(j).getString("variables"));
            if (childNode.get("providerName").asText().equals(provider)
                && childNode.get("regionName").asText().equals(region)) {
              System.out.println("region found");
              return true;
            }
          } catch (JsonProcessingException e) {
            return null;
          }
        }
      }
    }
    System.out.println("region not found");
    return false;
  }

  @Override
  public ArrayList<Provider> getProviders() throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"provider\"}}, {state: {equal: ACTIVE}}]}){id state variables}}";
    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();

    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<Provider> providers = new ArrayList<Provider>();
    for (int i = 0; i < processInstances.size(); i++) {
      Provider provider = new Provider();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode childNode;
      try {
        childNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
        provider.setName(childNode.get("providerName").asText());
        provider.setProcessId(processInstances.getJsonObject(i).getString("id"));
        providers.add(provider);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    return providers;
  }

  @Override
  public ArrayList<RegionKubernetes> getRegions(String provider)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String providerId = getProviderId(provider);
    System.out.println("providerId: " + providerId);
    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + providerId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";
    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    System.out.println("data: " + data);
    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    ArrayList<RegionKubernetes> regions = new ArrayList<RegionKubernetes>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");
    for (int j = 0; j < childProcessInstances.size(); j++) {
      if (childProcessInstances.getJsonObject(j).getString("processName").equals("region")) {
        JsonNode childNode;
        try {
          childNode = mapper.readTree(childProcessInstances.getJsonObject(j).getString("variables"));
          if (childNode.get("providerName").asText().equals(provider)) {
            RegionKubernetes region = new RegionKubernetes();
            region.setName(childNode.get("regionName").asText());
            region.setProcessId(childProcessInstances.getJsonObject(j).getString("id"));
            regions.add(region);
          }
        } catch (JsonProcessingException e) {
          return null;
        }
      }
    }
    return regions;
  }

  @Override
  public ArrayList<SuperClusterKubernetes> getSuperClusters(String provider, String region)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String regionId = getRegionId(provider, region);
    System.out.println("regionId: " + regionId);
    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + regionId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<SuperClusterKubernetes> superClusters = new ArrayList<SuperClusterKubernetes>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int j = 0; j < childProcessInstances.size(); j++) {
      if (childProcessInstances.getJsonObject(j).getString("processName").equals("superCluster")) {
        JsonNode childNode;
        try {
          childNode = mapper.readTree(childProcessInstances.getJsonObject(j).getString("variables"));
          if (childNode.get("providerName").asText().equals(provider)
              && childNode.get("regionName").asText().equals(region)) {
            SuperClusterKubernetes superCluster = new SuperClusterKubernetes();
            superCluster.setName(childNode.get("superClusterName").asText());
            superCluster.setProcessId(childProcessInstances.getJsonObject(j).getString("id"));
            superClusters.add(superCluster);
          }
        } catch (JsonProcessingException e) {
          return null;
        }
      }
    }
    return superClusters;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsOrganization(Organization organization)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException, JsonMappingException,
      JsonProcessingException {
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();
    ArrayList<Provider> providers = getProviders();
    for (int i = 0; i < providers.size(); i++) {
      ArrayList<RegionKubernetes> regions = getRegions(providers.get(i).getName());
      for (int j = 0; j < regions.size(); j++) {
        ArrayList<SuperClusterKubernetes> superClusters = getSuperClusters(providers.get(i).getName(),
            regions.get(j).getName());
        for (int k = 0; k < superClusters.size(); k++) {
          ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterOrganization(organization,
              superClusters.get(k).getProcessId());
          System.out.println("rc count: " + rcs.size());
          rcs.forEach(rc -> {
            System.out.println("rc: " + rc.getName());
            roboticsClouds.add(rc);
          });
        }
      }
    }
    return roboticsClouds;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsSuperClusterOrganization(Organization organization,
      String superClusterProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + superClusterProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("roboticsCloud")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName())) {
          RoboticsCloudKubernetes singleRoboticsCloud = new RoboticsCloudKubernetes();
          singleRoboticsCloud.setName(childNode.get("cloudInstanceName").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsSuperClusterTeam(Organization organization, String teamId,
      String superClusterProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + superClusterProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("roboticsCloud")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName()) && childNode.get("teamId").asText()
            .equals(teamId)) {
          RoboticsCloudKubernetes singleRoboticsCloud = new RoboticsCloudKubernetes();
          singleRoboticsCloud.setName(childNode.get("cloudInstanceName").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsSuperClusterUser(Organization organization, String teamId,
      String username,
      String superClusterProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + superClusterProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("roboticsCloud")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName()) && childNode.get("teamId").asText()
            .equals(teamId) && childNode.get("username").asText().equals(username)) {
          RoboticsCloudKubernetes singleRoboticsCloud = new RoboticsCloudKubernetes();
          singleRoboticsCloud.setName(childNode.get("cloudInstanceName").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsTeam(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
      JsonMappingException, JsonProcessingException {
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ArrayList<Provider> providers = getProviders();

    for (int i = 0; i < providers.size(); i++) {
      ArrayList<RegionKubernetes> regions = getRegions(providers.get(i).getName());
      for (int j = 0; j < regions.size(); j++) {
        ArrayList<SuperClusterKubernetes> superClusters = getSuperClusters(providers.get(i).getName(),
            regions.get(j).getName());
        for (int k = 0; k < superClusters.size(); k++) {
          ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterTeam(organization, teamId,
              superClusters.get(k).getProcessId());
          System.out.println("rc count: " + rcs.size());
          rcs.forEach(rc -> {
            System.out.println("rc: " + rc.getName());
            roboticsClouds.add(rc);
          });
        }
      }
    }
    return roboticsClouds;
  }

  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsUser(Organization organization, String teamId,
      String username)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
      JsonMappingException, JsonProcessingException {

    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ArrayList<Provider> providers = getProviders();

    for (int i = 0; i < providers.size(); i++) {
      ArrayList<RegionKubernetes> regions = getRegions(providers.get(i).getName());
      for (int j = 0; j < regions.size(); j++) {
        ArrayList<SuperClusterKubernetes> superClusters = getSuperClusters(providers.get(i).getName(),
            regions.get(j).getName());
        for (int k = 0; k < superClusters.size(); k++) {
          ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterTeam(organization, teamId,
              superClusters.get(k).getProcessId());
          System.out.println("rc count: " + rcs.size());
          rcs.forEach(rc -> {
            System.out.println("rc: " + rc.getName());
            roboticsClouds.add(rc);
          });
        }
      }
    }
    return roboticsClouds;
  }

}
