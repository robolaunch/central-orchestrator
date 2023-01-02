package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.robolaunch.models.Organization;
import org.robolaunch.models.Provider;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.RoboticsCloud;
import org.robolaunch.models.SuperCluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;
import io.smallrye.graphql.execution.ExecutionException;

public interface KubernetesRepository {
        public String getProviderId(String provider)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonProcessingException;

        public String getSuperClusterProcessId(String provider, String region, String superCluster)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonProcessingException;

        public Integer getCurrentBufferingCountOfType(String instanceType, String provider, String region,
                        String superCluster)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonMappingException, JsonProcessingException;

        public Integer getCurrentBufferedCountOfType(String instanceType, String provider, String region,
                        String superCluster)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonMappingException, JsonProcessingException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, IOException, ApiException, MinioException;

        public Boolean providerExists(String provider)
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public Boolean regionExists(String provider, String region)
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public ArrayList<Provider> getProviders()
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public ArrayList<RegionKubernetes> getRegions(String provider)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

        public ArrayList<SuperCluster> getSuperClusters(String provider, String region)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

}
