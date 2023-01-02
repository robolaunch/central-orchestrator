package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.models.Organization;
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

  @Override
  public void getCloudInstances(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"superCluster\"}}){id childProcessInstances{processName parentProcessInstanceId variables}}}";

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
  }

  @Override
  public String getSuperClusterProcessId(String provider, String region, String superCluster)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"superCluster\"}}){id variables}}";

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
    var vcs = virtualClustersApi.list(listOptions).getObject().getItems();
    Integer counter = 0;
    for (DynamicKubernetesObject vc : vcs) {
      if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString().equals("Running")) {
        counter++;
      }
    }
    return counter;
  }

}
