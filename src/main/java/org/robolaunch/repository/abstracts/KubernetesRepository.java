package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.robolaunch.models.Organization;
import org.robolaunch.models.Provider;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.Robot;
import org.robolaunch.models.RoboticsCloudKubernetes;
import org.robolaunch.models.SuperClusterKubernetes;

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

        public ArrayList<SuperClusterKubernetes> getSuperClusters(String provider, String region)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

        public ArrayList<String> getSuperClusterProcesses()
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsOrganization(Organization organization)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonMappingException, JsonProcessingException;

        public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsTeam(Organization organization, String teamId)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonMappingException, JsonProcessingException;

        public ArrayList<RoboticsCloudKubernetes> getRoboticsCloudsUser(Organization organization, String teamId,
                        String username)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException,
                        JsonMappingException, JsonProcessingException;

        public ArrayList<Robot> getRobotsOrganization(Organization organization)
                        throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
                        JsonProcessingException, ExecutionException;

        public ArrayList<Robot> getRobotsTeam(Organization organization, String teamId)
                        throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
                        JsonProcessingException, ExecutionException;

        public ArrayList<Robot> getRobotsRoboticsCloud(String roboticsCloudProcessId)
                        throws java.util.concurrent.ExecutionException, InterruptedException, JsonMappingException,
                        JsonProcessingException, ExecutionException;

        public String readPlatformContent(String version, String resource) throws MalformedURLException, IOException;

        public com.google.gson.JsonObject readPlatformContentAsJsonObject(String version, String resource)
                        throws IOException;

        public String getLatestPlatformVersion() throws IOException;

        public Boolean isAuthorizedRoboticsCloud(Organization organization, String teamId, String username)
                        throws InternalError, IOException;
}
