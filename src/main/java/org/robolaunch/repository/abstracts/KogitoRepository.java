package org.robolaunch.repository.abstracts;

import org.robolaunch.model.account.Organization;

import io.smallrye.graphql.execution.ExecutionException;

public interface KogitoRepository {

  public String getProcessId(Organization organization, String teamId)
      throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

}
