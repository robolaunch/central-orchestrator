package org.robolaunch.repository.concretes;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.model.account.Organization;
import org.robolaunch.repository.abstracts.KogitoRepository;

import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.graphql.execution.ExecutionException;

@ApplicationScoped
public class KogitoRepositoryImpl implements KogitoRepository {
  DynamicGraphQLClient graphqlClient;
  @ConfigProperty(name = "kogito.dataindex.http.url")
  String dataIndexUrl;

  @PostConstruct
  public void initializeApis() throws IOException {
    this.graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(dataIndexUrl + "/graphql/")
        .build();

  }

  @Override
  public String getProcessId(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException {
    String queryStr = "{roboticsCloud(where: {and: [{organization: {name: {equal:\""
        + organization.getName()
        + "\"}}},{teamId: {equal:\""
        + teamId
        + "\"}}]}) {id}}";

    Response response = graphqlClient.executeSync(queryStr);

    javax.json.JsonObject data = response.getData();
    String processId = data.getJsonArray("roboticsCloud").getJsonObject(0).getString("id");
    return processId;
  }

}
