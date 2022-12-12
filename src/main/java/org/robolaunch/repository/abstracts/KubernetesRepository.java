package org.robolaunch.repository.abstracts;

import java.util.ArrayList;

import org.robolaunch.models.CloudInstance;
import org.robolaunch.models.Organization;

import io.smallrye.graphql.execution.ExecutionException;

public interface KubernetesRepository {

        public ArrayList<CloudInstance> getCloudInstances(Organization organization, String departmentName)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

}
