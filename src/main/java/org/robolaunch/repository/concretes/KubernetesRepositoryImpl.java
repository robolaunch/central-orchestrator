package org.robolaunch.repository.concretes;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.models.Organization;
import org.robolaunch.repository.abstracts.KubernetesRepository;

import com.google.gson.Gson;

import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.graphql.execution.ExecutionException;

@ApplicationScoped
public class KubernetesRepositoryImpl implements KubernetesRepository {
  DynamicGraphQLClient graphqlClient;

  @ConfigProperty(name = "kogito.dataindex.http.url")
  String kogitoDataIndexUrl;

  @PostConstruct
  public void initializeServices() throws IOException {
    graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(kogitoDataIndexUrl + "/graphql")
        .build();

  }

  @Override
  public void getCloudInstances(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    System.out.println("Organization: " + organization.getName());
    System.out.println("TeamId: " + teamId);
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"superCluster\"}}){id childProcessInstances{processName parentProcessInstanceId variables}}}";
    Gson gson = new Gson();

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    System.out.println("data: " + gson.toJson(data));
  }

  @Override
  public Integer getBufferInstanceCount(String provider, String region, String superCluster)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "query{ProcessInstances(where: {processName: {equal:\"superCluster\"}}){id variables}}";
    Gson gson = new Gson();

    Response response = graphqlClient.executeSync(queryStr);
    JsonObject data = response.getData();
    System.out.println("data: " + gson.toJson(data));
    data.getJsonArray("ProcessInstances").forEach(processInstance -> {
      System.out.println("processInstance: " + gson.toJson(processInstance.asJsonObject().getString("variables")));
    });
    return 0;
  }

}
