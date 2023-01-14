package org.robolaunch.repository.concretes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
import org.robolaunch.models.Fleet;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Provider;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.Robot;
import org.robolaunch.models.RoboticsCloudKubernetes;
import org.robolaunch.models.SuperClusterKubernetes;
import org.robolaunch.repository.abstracts.KubernetesRepository;
import org.robolaunch.service.ApiClientManager;
import org.robolaunch.service.DepartmentService;
import org.robolaunch.service.OrganizationService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;

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
  @Inject
  DepartmentService departmentService;
  @Inject
  OrganizationService organizationService;

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
      if (processInstances.size() == 0) {
        return 0;
      }
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
    Integer counter = 0;
    for (DynamicKubernetesObject vc : vcs) {
      if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString().equals("Running")) {
        counter++;
      }
    }
    return counter;
  }

  @Override
  public Boolean providerExists(String provider) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {
    System.out.println("in");
    String queryStr = "query{ProcessInstances(where: {and: [{processName: {equal:\"provider\"}}, {state: {equal: ACTIVE}}]}){id variables}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    System.out.println("response: " + data);

    JsonArray processInstances = data.getJsonArray("ProcessInstances");

    // Iterate over processInstances and check if providerName is equal to provider
    for (int i = 0; i < processInstances.size(); i++) {
      System.out.println("INNNN");
      ObjectMapper mapper = new ObjectMapper();
      JsonNode childNode = mapper.readTree(processInstances.getJsonObject(i).getString("variables"));
      System.out.println("childnode: " + childNode);
      if (childNode.get("providerName").asText().equals(provider)) {
        System.out.println("out2");
        return true;
      }
    }
    System.out.println("ou2t");

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
              return true;
            }
          } catch (JsonProcessingException e) {
            return null;
          }
        }
      }
    }
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
    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + providerId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";
    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
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
  public ArrayList<String> getSuperClusterProcesses()
      throws java.util.concurrent.ExecutionException, InterruptedException {
    ArrayList<String> scs = new ArrayList<String>();
    ArrayList<Provider> providers = getProviders();
    for (int i = 0; i < providers.size(); i++) {
      ArrayList<RegionKubernetes> regions = getRegions(providers.get(i).getName());
      for (int j = 0; j < regions.size(); j++) {
        ArrayList<SuperClusterKubernetes> superClusters = getSuperClusters(providers.get(i).getName(),
            regions.get(j).getName());
        for (int k = 0; k < superClusters.size(); k++) {
          scs.add(superClusters.get(k).getProcessId());
        }
      }
    }
    return scs;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsOrganization(Organization organization)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException, JsonMappingException,
      JsonProcessingException {
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();

    ArrayList<String> superClusters = getSuperClusterProcesses();
    for (int i = 0; i < superClusters.size(); i++) {
      ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterOrganization(organization,
          superClusters.get(i));
      rcs.forEach(rc -> {
        roboticsClouds.add(rc);
      });
    }
    return roboticsClouds;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsTeam(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
      InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      MinioException {
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();
    ArrayList<String> superClusters = getSuperClusterProcesses();

    for (int k = 0; k < superClusters.size(); k++) {
      ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterTeam(organization, teamId,
          superClusters.get(k));
      rcs.forEach(rc -> {
        roboticsClouds.add(rc);
      });
    }
    return roboticsClouds;
  }

  @Override
  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsUser(Organization organization, String teamId,
      String username)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
      JsonMappingException, JsonProcessingException {
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();
    ArrayList<String> superClusters = getSuperClusterProcesses();

    for (int k = 0; k < superClusters.size(); k++) {
      ArrayList<RoboticsCloudKubernetes> rcs = getRoboticsCloudsSuperClusterUser(organization, teamId, username,
          superClusters.get(k));
      rcs.forEach(rc -> {
        roboticsClouds.add(rc);
      });
    }
    return roboticsClouds;
  }

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
          singleRoboticsCloud.setInstanceType(childNode.get("instanceType").asText());
          if (childNode.get("bufferName") != null) {
            singleRoboticsCloud.setBufferName(childNode.get("bufferName").asText());
          }
          if (childNode.get("teamName") != null) {
            singleRoboticsCloud.setTeamName(childNode.get("teamName").asText());
          }
          singleRoboticsCloud.setOrganization(organizationNode.get("name").asText());
          singleRoboticsCloud.setRegionName(childNode.get("regionName").asText());
          singleRoboticsCloud.setUserStage(childNode.get("userStage").asText());
          singleRoboticsCloud.setTeam(childNode.get("teamId").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

  public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsSuperClusterTeam(Organization organization, String teamId,
      String superClusterProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      MinioException {

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + superClusterProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<RoboticsCloudKubernetes> roboticsClouds = new ArrayList<RoboticsCloudKubernetes>();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode childNodeSC = mapper.readTree(processInstances.getJsonObject(0).getString("variables"));
    String providerName = childNodeSC.get("providerName").asText();
    String regionName = childNodeSC.get("regionName").asText();
    String superClusterName = childNodeSC.get("superClusterName").asText();

    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("roboticsCloud")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }

        DynamicKubernetesApi machineDeploymentApi = apiClientManager.getMachineDeploymentApi(providerName, regionName,
            superClusterName);

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName()) && childNode.get("teamId").asText()
            .equals(teamId)) {
          RoboticsCloudKubernetes singleRoboticsCloud = new RoboticsCloudKubernetes();
          singleRoboticsCloud.setName(childNode.get("cloudInstanceName").asText());
          singleRoboticsCloud.setInstanceType(childNode.get("instanceType").asText());
          if (childNode.get("bufferName") != null) {
            singleRoboticsCloud.setBufferName(childNode.get("bufferName").asText());
          }
          singleRoboticsCloud.setTeamName(childNode.get("teamName").asText());
          singleRoboticsCloud.setOrganization(organizationNode.get("name").asText());
          singleRoboticsCloud.setRegionName(childNode.get("regionName").asText());
          singleRoboticsCloud.setUserStage(childNode.get("userStage").asText());
          singleRoboticsCloud.setTeam(childNode.get("teamId").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          var machineDeployment = machineDeploymentApi.get("kube-system", "md-" + childNode.get("bufferName").asText());

          Integer diskSize = machineDeployment.getObject().getRaw().getAsJsonObject().get("spec").getAsJsonObject()
              .get("template").getAsJsonObject().get("spec").getAsJsonObject()
              .get("providerSpec").getAsJsonObject().get("value")
              .getAsJsonObject().get("cloudProviderSpec").getAsJsonObject().get("diskSize").getAsInt();

          System.out.println("disk size found: " + diskSize);

          // Set CPU and MEMORY
          if (childNode.get("instanceType").asText().equals("t3a.xlarge")
              && childNode.get("providerName").asText().equals("aws")) {
            singleRoboticsCloud.setVCPU(4);
            singleRoboticsCloud.setMemory(16);
            singleRoboticsCloud.setGPU(0);
          } else if (childNode.get("instanceType").asText().equals("t2.medium")
              && childNode.get("providerName").asText().equals("aws")) {
            singleRoboticsCloud.setVCPU(2);
            singleRoboticsCloud.setMemory(4);
            singleRoboticsCloud.setGPU(0);
          }
          singleRoboticsCloud.setDiskSize(diskSize);
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

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
          singleRoboticsCloud.setInstanceType(childNode.get("instanceType").asText());
          if (childNode.get("bufferName") != null) {
            singleRoboticsCloud.setBufferName(childNode.get("bufferName").asText());
          }
          singleRoboticsCloud.setTeamName(childNode.get("teamName").asText());
          singleRoboticsCloud.setOrganization(organizationNode.get("name").asText());
          singleRoboticsCloud.setRegionName(childNode.get("regionName").asText());
          singleRoboticsCloud.setTeam(childNode.get("teamId").asText());
          singleRoboticsCloud.setUserStage(childNode.get("userStage").asText());
          singleRoboticsCloud.setProcessId(childProcessInstances.getJsonObject(i).getString("id"));
          roboticsClouds.add(singleRoboticsCloud);
        }
      }
    }

    return roboticsClouds;
  }

  // Get Robots Organization
  @Override
  public ArrayList<Robot> getRobotsOrganization(Organization organization)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException, ExecutionException {
    ArrayList<Robot> robots = new ArrayList<Robot>();
    ArrayList<String> superClusters = getSuperClusterProcesses();

    for (int k = 0; k < superClusters.size(); k++) {
      ArrayList<Robot> scRobots = getRobotsSuperClusterOrganization(organization,
          superClusters.get(k));
      scRobots.forEach(rc -> {
        robots.add(rc);
      });
    }
    return robots;
  }

  public ArrayList<Robot> getRobotsSuperClusterOrganization(Organization organization, String superClusterProcessId)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException, ExecutionException {
    ArrayList<RoboticsCloudKubernetes> rcks = getRoboticsCloudsOrganization(organization);
    ArrayList<Robot> robots = new ArrayList<Robot>();

    for (int i = 0; i < rcks.size(); i++) {
      String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + rcks.get(i).getProcessId()
          + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";
      Response response = graphqlClient.executeSync(queryStr);
      JsonObject data = response.getData();
      JsonArray processInstances = data.getJsonArray("ProcessInstances");
      ObjectMapper mapper = new ObjectMapper();
      if (processInstances.getJsonObject(0).getJsonArray("childProcessInstances").size() == 0) {
        continue;
      }
      JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");
      for (int k = 0; k < childProcessInstances.size(); k++) {
        if (childProcessInstances.getJsonObject(k).getString("processName").equals("robot")) {
          JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(k).getString("variables"));
          // check if childnode has organization node
          if (childNode.get("organization") == null) {
            continue;
          }

          JsonNode requestRobotNode = childNode.get("requestRobot");
          JsonNode organizationNode = requestRobotNode.get("organization");
          JsonNode robotNode = requestRobotNode.get("robot");
          if (organizationNode.get("name").asText().equals(organization.getName())) {
            Robot singleRobot = new Robot();
            singleRobot.setProcessId(childProcessInstances.getJsonObject(k).getString("id"));
            // Set Name
            JsonNode metadataNode = robotNode.get("metadata");
            singleRobot.setName(metadataNode.get("name").asText());

            JsonNode specNode = robotNode.get("spec");
            // Set Distributions
            JsonNode distNode = specNode.get("distributions");
            List<String> distros = new ArrayList<String>();
            distNode.forEach(dist -> {
              distros.add(dist.asText());
            });
            singleRobot.setDistributions(distros);

            // Set Storage
            JsonNode storageNode = specNode.get("storage");
            singleRobot.setStorage(storageNode.get("amount").asInt() / 1000);

            // Set Bridge Enabled
            JsonNode bridgeNode = specNode.get("rosBridgeTemplate");
            JsonNode bridgeRosNode = bridgeNode.get("ros");
            JsonNode bridgeRos2Node = bridgeNode.get("ros2");
            singleRobot.setBridgeEnabled(bridgeRosNode.get("enabled").asBoolean()
                || bridgeRos2Node.get("enabled").asBoolean());

            // Set Image
            singleRobot.setImage(bridgeNode.get("image").asText());

            // Set VDI Enabled && IDE Enabled
            JsonNode devSuiteNode = specNode.get("robotDevSuiteTemplate");
            singleRobot.setVdiEnabled(devSuiteNode.get("vdiEnabled").asBoolean());
            singleRobot.setIdeEnabled(devSuiteNode.get("ideEnabled").asBoolean());

            // Set VDI Session count
            if (devSuiteNode.get("vdiEnabled").asBoolean()) {
              JsonNode vdiNode = devSuiteNode.get("robotVDITemplate");
              singleRobot.setVdiSessionCount(vdiNode.get("sessionCount").asInt());
            }

            // Set Workspaces Path
            JsonNode workspacesNode = specNode.get("workspaceManagerTemplate");
            singleRobot.setWorkspacesPath(workspacesNode.get("workspacesPath").asText());

            robots.add(singleRobot);
          }
        }
      }

    }

    return robots;
  }

  // Get Robots Team
  @Override
  public ArrayList<Robot> getRobotsTeam(Organization organization, String teamId)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException, ExecutionException {
    ArrayList<Robot> robots = new ArrayList<Robot>();
    ArrayList<String> superClusters = getSuperClusterProcesses();

    for (int k = 0; k < superClusters.size(); k++) {
      ArrayList<Robot> scRobots = getRobotsSuperClusterTeam(organization, teamId,
          superClusters.get(k));
      scRobots.forEach(rc -> {
        robots.add(rc);
      });
    }
    return robots;
  }

  public ArrayList<Robot> getRobotsSuperClusterTeam(Organization organization, String teamId,
      String superClusterProcessId)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException, ExecutionException {
    ArrayList<RoboticsCloudKubernetes> rcks = getRoboticsCloudsOrganization(organization);
    ArrayList<Robot> robots = new ArrayList<Robot>();

    for (int i = 0; i < rcks.size(); i++) {
      String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + rcks.get(i).getProcessId()
          + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";
      Response response = graphqlClient.executeSync(queryStr);
      JsonObject data = response.getData();
      JsonArray processInstances = data.getJsonArray("ProcessInstances");
      ObjectMapper mapper = new ObjectMapper();
      if (processInstances.getJsonObject(0).getJsonArray("childProcessInstances").size() == 0) {
        continue;
      }
      JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");
      for (int k = 0; k < childProcessInstances.size(); k++) {
        if (childProcessInstances.getJsonObject(k).getString("processName").equals("robot")) {
          JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(k).getString("variables"));
          // check if childnode has organization node
          if (childNode.get("organization") == null) {
            continue;
          }

          JsonNode requestRobotNode = childNode.get("requestRobot");
          JsonNode organizationNode = requestRobotNode.get("organization");
          JsonNode robotNode = requestRobotNode.get("robot");
          if (organizationNode.get("name").asText().equals(organization.getName())
              && requestRobotNode.get("teamId").asText().equals(teamId)) {
            Robot singleRobot = new Robot();

            // Set Name
            JsonNode metadataNode = robotNode.get("metadata");
            singleRobot.setName(metadataNode.get("name").asText());

            JsonNode specNode = robotNode.get("spec");
            // Set Distributions
            JsonNode distNode = specNode.get("distributions");
            List<String> distros = new ArrayList<String>();
            distNode.forEach(dist -> {
              distros.add(dist.asText());
            });
            singleRobot.setDistributions(distros);

            // Set Storage
            JsonNode storageNode = specNode.get("storage");
            singleRobot.setStorage(storageNode.get("amount").asInt() / 1000);

            // Set Bridge Enabled
            JsonNode bridgeNode = specNode.get("rosBridgeTemplate");
            JsonNode bridgeRosNode = bridgeNode.get("ros");
            JsonNode bridgeRos2Node = bridgeNode.get("ros2");
            singleRobot.setBridgeEnabled(bridgeRosNode.get("enabled").asBoolean()
                || bridgeRos2Node.get("enabled").asBoolean());

            // Set Image
            singleRobot.setImage(bridgeNode.get("image").asText());

            // Set VDI Enabled && IDE Enabled
            JsonNode devSuiteNode = specNode.get("robotDevSuiteTemplate");
            singleRobot.setVdiEnabled(devSuiteNode.get("vdiEnabled").asBoolean());
            singleRobot.setIdeEnabled(devSuiteNode.get("ideEnabled").asBoolean());

            // Set VDI Session count
            if (devSuiteNode.get("vdiEnabled").asBoolean()) {
              JsonNode vdiNode = devSuiteNode.get("robotVDITemplate");
              singleRobot.setVdiSessionCount(vdiNode.get("sessionCount").asInt());
            }

            // Set Workspaces Path
            JsonNode workspacesNode = specNode.get("workspaceManagerTemplate");
            singleRobot.setWorkspacesPath(workspacesNode.get("workspacesPath").asText());

            robots.add(singleRobot);
          }
        }
      }

    }

    return robots;
  }

  // Get Robots Robotics Clouds
  @Override
  public ArrayList<Robot> getRobotsRoboticsCloud(String roboticsCloudProcessId)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException {
    ArrayList<Robot> robots = new ArrayList<Robot>();

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + roboticsCloudProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";
    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ObjectMapper mapper = new ObjectMapper();
    if (processInstances.getJsonObject(0).getJsonArray("childProcessInstances").size() == 0) {
      return robots;
    }
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");
    for (int k = 0; k < childProcessInstances.size(); k++) {
      if (childProcessInstances.getJsonObject(k).getString("processName").equals("robot")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(k).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }

        JsonNode requestRobotNode = childNode.get("requestRobot");
        JsonNode robotNode = requestRobotNode.get("robot");

        Robot singleRobot = new Robot();

        // Set Name
        JsonNode metadataNode = robotNode.get("metadata");
        singleRobot.setName(metadataNode.get("name").asText());

        JsonNode specNode = robotNode.get("spec");
        // Set Distributions
        JsonNode distNode = specNode.get("distributions");
        List<String> distros = new ArrayList<String>();
        distNode.forEach(dist -> {
          distros.add(dist.asText());
        });
        singleRobot.setDistributions(distros);

        // Set Storage
        JsonNode storageNode = specNode.get("storage");
        singleRobot.setStorage(storageNode.get("amount").asInt() / 1000);

        // Set Bridge Enabled
        JsonNode bridgeNode = specNode.get("rosBridgeTemplate");
        JsonNode bridgeRosNode = bridgeNode.get("ros");
        JsonNode bridgeRos2Node = bridgeNode.get("ros2");
        singleRobot.setBridgeEnabled(bridgeRosNode.get("enabled").asBoolean()
            || bridgeRos2Node.get("enabled").asBoolean());

        // Set Image
        singleRobot.setImage(bridgeNode.get("image").asText());

        // Set VDI Enabled && IDE Enabled
        JsonNode devSuiteNode = specNode.get("robotDevSuiteTemplate");
        singleRobot.setVdiEnabled(devSuiteNode.get("vdiEnabled").asBoolean());
        singleRobot.setIdeEnabled(devSuiteNode.get("ideEnabled").asBoolean());

        // Set VDI Session count
        if (devSuiteNode.get("vdiEnabled").asBoolean()) {
          JsonNode vdiNode = devSuiteNode.get("robotVDITemplate");
          singleRobot.setVdiSessionCount(vdiNode.get("sessionCount").asInt());
        }

        // Set Workspaces Path
        JsonNode workspacesNode = specNode.get("workspaceManagerTemplate");
        singleRobot.setWorkspacesPath(workspacesNode.get("workspacesPath").asText());

        robots.add(singleRobot);
      }
    }
    return robots;

  }

  @Override
  public ArrayList<Fleet> getFleetsOrganization(Organization organization)
      throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
      JsonProcessingException, ExecutionException {
    ArrayList<RoboticsCloudKubernetes> rcso = getRoboticsCloudsOrganization(organization);

    ArrayList<Fleet> fleets = new ArrayList<Fleet>();
    for (RoboticsCloudKubernetes rcs : rcso) {
      ArrayList<Fleet> insideFleets = getFleetsRoboticsCloudOrganization(organization, rcs.getProcessId());
      for (Fleet fleet : insideFleets) {
        fleets.add(fleet);
      }
    }
    return fleets;
  }

  public ArrayList<Fleet> getFleetsRoboticsCloudOrganization(Organization organization,
      String roboticsCloudProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {
    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + roboticsCloudProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<Fleet> fleets = new ArrayList<Fleet>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("fleet")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }
        JsonNode requestFleetNode = childNode.get("requestFleet");
        JsonNode fleetNode = requestFleetNode.get("fleet");

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName())) {
          Fleet fleet = new Fleet();

          // Set Name
          JsonNode metadataNode = fleetNode.get("metadata");
          fleet.setName(metadataNode.get("name").asText());
          fleets.add(fleet);
        }
      }
    }

    return fleets;
  }

  @Override
  public ArrayList<Fleet> getFleetsTeam(Organization organization, String teamId)
      throws java.util.concurrent.ExecutionException, InterruptedException, ExecutionException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException, MinioException {
    ArrayList<RoboticsCloudKubernetes> rcso = getRoboticsCloudsTeam(organization, teamId);

    ArrayList<Fleet> fleets = new ArrayList<Fleet>();
    for (RoboticsCloudKubernetes rcs : rcso) {
      ArrayList<Fleet> insideFleets = getFleetsRoboticsCloudTeam(organization, teamId, rcs.getProcessId());
      for (Fleet fleet : insideFleets) {
        fleets.add(fleet);
      }
    }
    return fleets;
  }

  public ArrayList<Fleet> getFleetsRoboticsCloudTeam(Organization organization, String teamId,
      String roboticsCloudProcessId) throws java.util.concurrent.ExecutionException, InterruptedException,
      JsonMappingException, JsonProcessingException {
    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + roboticsCloudProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<Fleet> fleets = new ArrayList<Fleet>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("fleet")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));
        // check if childnode has organization node
        if (childNode.get("organization") == null) {
          continue;
        }
        JsonNode requestFleetNode = childNode.get("requestFleet");
        JsonNode fleetNode = requestFleetNode.get("fleet");

        JsonNode organizationNode = childNode.get("organization");
        if (organizationNode.get("name").asText().equals(organization.getName())
            && childNode.get("teamId").asText().equals(teamId)) {
          Fleet fleet = new Fleet();

          // Set Name
          JsonNode metadataNode = fleetNode.get("metadata");
          fleet.setName(metadataNode.get("name").asText());
          fleets.add(fleet);
        }
      }
    }

    return fleets;
  }

  @Override
  public ArrayList<Fleet> getFleetsRoboticsCloud(String roboticsCloudProcessId)
      throws java.util.concurrent.ExecutionException, InterruptedException, ExecutionException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException, MinioException {

    String queryStr = "query{ProcessInstances(where: {and: [{id: {equal:\"" + roboticsCloudProcessId
        + "\"}}, {state: {equal: ACTIVE}}]}){id state childProcessInstances{id processName state variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    JsonArray processInstances = data.getJsonArray("ProcessInstances");
    ArrayList<Fleet> fleets = new ArrayList<Fleet>();

    ObjectMapper mapper = new ObjectMapper();
    JsonArray childProcessInstances = processInstances.getJsonObject(0).getJsonArray("childProcessInstances");

    for (int i = 0; i < childProcessInstances.size(); i++) {
      if (childProcessInstances.getJsonObject(i).getString("processName").equals("fleet")) {
        JsonNode childNode = mapper.readTree(childProcessInstances.getJsonObject(i).getString("variables"));

        JsonNode requestFleetNode = childNode.get("requestFleet");
        JsonNode fleetNode = requestFleetNode.get("fleet");

        Fleet fleet = new Fleet();
        // Set Name
        JsonNode metadataNode = fleetNode.get("metadata");
        fleet.setName(metadataNode.get("name").asText());
        fleets.add(fleet);
      }
    }

    return fleets;
  }

  @Override
  public String readPlatformContent(String version, String resource) throws IOException {
    URL url = new URL("https://raw.githubusercontent.com/robolaunch/robolaunch/main/platform.json");
    InputStreamReader reader = new InputStreamReader(url.openStream());
    Gson gson = new Gson();
    com.google.gson.JsonObject wholeObject = gson.fromJson(reader, com.google.gson.JsonObject.class);
    com.google.gson.JsonArray versions = wholeObject.getAsJsonArray("versions");
    String yamlString = "";
    for (int i = 0; i < versions.size(); i++) {
      com.google.gson.JsonObject versionObj = versions.get(i).getAsJsonObject();
      String innerURL = "";
      if (versionObj.get("version").getAsString().equals(version)) {

        if (resource.equals("certManager")) {
          innerURL = versionObj.get("roboticsCloud").getAsJsonObject().get("kubernetes").getAsJsonObject()
              .get("operators").getAsJsonObject().get("cert-manager").getAsJsonObject().get("release").getAsString();
        } else if (resource.equals("robotOperator")) {
          innerURL = versionObj.get("roboticsCloud").getAsJsonObject().get("kubernetes").getAsJsonObject()
              .get("operators").getAsJsonObject()
              .get("robot")
              .getAsJsonObject().get("release").getAsString();
        } else if (resource.equals("connectionHub")) {
          innerURL = versionObj.get("roboticsCloud").getAsJsonObject().get("kubernetes")
              .getAsJsonObject()
              .get("operators").getAsJsonObject().get("connectionHub").getAsJsonObject().get("release").getAsString();
        } else if (resource.equals("fleetOperator")) {
          innerURL = versionObj.get("roboticsCloud").getAsJsonObject().get("kubernetes")
              .getAsJsonObject()
              .get("operators").getAsJsonObject().get("fleet").getAsJsonObject().get("release").getAsString();
        }

        URL innerConnection = new URL(innerURL);
        InputStreamReader innerReader = new InputStreamReader(innerConnection.openStream());
        BufferedReader br = new BufferedReader(innerReader);

        String line;
        while ((line = br.readLine()) != null) {
          yamlString += line + "\n";
        }
        br.close();
        innerReader.close();
      }
    }
    return yamlString;
  }

  @Override
  public com.google.gson.JsonObject readPlatformContentAsJsonObject(String version, String resource)
      throws IOException {
    URL url = new URL("https://raw.githubusercontent.com/robolaunch/robolaunch/main/platform.json");
    InputStreamReader reader = new InputStreamReader(url.openStream());
    Gson gson = new Gson();
    com.google.gson.JsonObject wholeObject = gson.fromJson(reader, com.google.gson.JsonObject.class);
    com.google.gson.JsonArray versions = wholeObject.getAsJsonArray("versions");
    String yamlString = "";
    com.google.gson.JsonObject resultingJson = new com.google.gson.JsonObject();
    for (int i = 0; i < versions.size(); i++) {
      com.google.gson.JsonObject versionObj = versions.get(i).getAsJsonObject();
      String innerURL = "";
      if (versionObj.get("version").getAsString().equals(version)) {

        if (resource.equals("connectionHubCloud")) {
          innerURL = versionObj.get("roboticsCloud").getAsJsonObject().get("kubernetes").getAsJsonObject()
              .get("operators").getAsJsonObject().get("connectionHub").getAsJsonObject().get("subresources")
              .getAsJsonObject().get("cloudInstance").getAsString();
        }

        URL innerConnection = new URL(innerURL);
        InputStreamReader innerReader = new InputStreamReader(innerConnection.openStream());
        BufferedReader br = new BufferedReader(innerReader);

        String line;
        while ((line = br.readLine()) != null) {
          yamlString += line + "\n";
        }
        br.close();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        var object = yamlMapper.readValue(yamlString, Object.class);
        ObjectMapper jsonMapper = new ObjectMapper();
        var jsonString = jsonMapper.writeValueAsString(object);

        resultingJson = new Gson().fromJson(jsonString, com.google.gson.JsonObject.class);
      }
    }
    return resultingJson;
  }

  @Override
  public String getLatestPlatformVersion() throws IOException {
    URL url = new URL("https://raw.githubusercontent.com/robolaunch/robolaunch/main/platform.json");
    InputStreamReader reader = new InputStreamReader(url.openStream());
    Gson gson = new Gson();
    com.google.gson.JsonObject wholeObject = gson.fromJson(reader, com.google.gson.JsonObject.class);
    com.google.gson.JsonArray versions = wholeObject.getAsJsonArray("versions");

    return versions.get(0).getAsJsonObject().get("version").getAsString();
  }

  @Override
  public Boolean isAuthorizedRoboticsCloud(Organization organization, String teamId, String username)
      throws InternalError, IOException {
    Boolean organizationManager = organizationService.isCurrentUserManager(organization);
    Boolean teamManager = departmentService.isCurrentUserManagerTeam(teamId);
    return teamManager || organizationManager;
  }

}
