package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.robolaunch.models.Organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;
import io.smallrye.graphql.execution.ExecutionException;

public interface KubernetesRepository {

        public void getCloudInstances(Organization organization, String teamId)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

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
}
