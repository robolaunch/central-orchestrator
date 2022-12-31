package org.robolaunch.repository.abstracts;

import org.robolaunch.models.Organization;

import io.smallrye.graphql.execution.ExecutionException;

public interface KubernetesRepository {

        public void getCloudInstances(Organization organization, String teamId)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

        public Integer getBufferInstanceCount(String provider, String region, String superCluster)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

}
