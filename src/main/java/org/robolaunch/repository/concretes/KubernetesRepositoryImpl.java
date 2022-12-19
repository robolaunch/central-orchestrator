package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.models.CloudInstance;
import org.robolaunch.models.Organization;
import org.robolaunch.repository.abstracts.KubernetesRepository;

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
        .url("http://" + kogitoDataIndexUrl + "/graphql/")
        .build();

  }

  @Override
  public ArrayList<CloudInstance> getCloudInstances(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "{InitializeRoboticsCloud(where: {and: [{organization: {name: {equal:\""
        + organization.getName()
        + "\"}}},{departmentName: {equal:\""
        + teamId
        + "\"}}]}) {id cloudInstanceName region departmentName diskSize instanceType userStage}}";

    Response response = graphqlClient.executeSync(queryStr);

    JsonObject data = response.getData();

    ArrayList<CloudInstance> cloudInstances = new ArrayList<CloudInstance>();

    data.getJsonArray("InitializeRoboticsCloud").forEach(instance -> {
      CloudInstance cloudInstance = new CloudInstance();
      cloudInstance.setProcessId(instance.asJsonObject().getString("id"));
      cloudInstance.setDiskSize(instance.asJsonObject().getInt("diskSize"));
      cloudInstance.setInstanceType(instance.asJsonObject().getString("instanceType"));
      cloudInstance.setName(instance.asJsonObject().getString("cloudInstanceName"));
      cloudInstance.setRegion(instance.asJsonObject().getString("region"));
      // cloudInstance.setStatus(instance.asJsonObject().getString("userStage"));
      cloudInstances.add(cloudInstance);
    });

    return cloudInstances;
  }

}
