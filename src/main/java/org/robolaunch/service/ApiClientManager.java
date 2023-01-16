package org.robolaunch.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.minio.errors.MinioException;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ApiClientManager {

   @Inject
   CloudInstanceHelperRepository cloudInstanceHelperRepository;

   private Map<String, ApiClient> apiClientMap = new ConcurrentHashMap<>();
   private Map<String, ApiClient> userApiClientMap = new ConcurrentHashMap<>();
   private Map<String, CoreV1Api> coreApiMap = new ConcurrentHashMap<>();
   private Map<String, AppsV1Api> appsApiMap = new ConcurrentHashMap<>();

   private Map<String, DynamicKubernetesApi> machineDeploymentApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> clusterVersionApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> virtualClusterApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> subnetApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> machineApiMap = new ConcurrentHashMap<>();

   // Virtual Cluster
   private Map<String, DynamicKubernetesApi> robotApiMap = new ConcurrentHashMap<>();

   @Unremovable
   public ApiClient getAdminApiClient(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      ApiClient apiClient = apiClientMap.get(provider + "/" + region + "/" + superCluster);
      if (apiClient == null) {
         System.out.println("Creating admin api client for " + provider + "/" + region + "/" + superCluster);
         apiClient = cloudInstanceHelperRepository.adminApiClient(provider, region, superCluster);
         apiClientMap.put(provider + "/" + region + "/" + superCluster, apiClient);
      }
      return apiClient;
   }

   @Unremovable
   public ApiClient getUserApiClient(String bufferName, String token, String provider, String region,
         String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      ApiClient apiClient = userApiClientMap.get(provider + "/" + region + "/" + superCluster + "/" + token);
      if (apiClient == null) {
         apiClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName, provider,
               region, superCluster);
         userApiClientMap.put(provider + "/" + region + "/" + superCluster + "/" + token, apiClient);
      }
      return apiClient;
   }

   @Unremovable
   public DynamicKubernetesApi getMachineDeploymentApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi machineDeploymentApi = machineDeploymentApiMap
            .get(provider + "/" + region + "/" + superCluster);
      if (machineDeploymentApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         machineDeploymentApi = new DynamicKubernetesApi("cluster.k8s.io", "v1alpha1",
               "machinedeployments",
               apiClient);
         machineDeploymentApiMap.put(provider + "/" + region + "/" + superCluster, machineDeploymentApi);
      }
      return machineDeploymentApi;
   }

   @Unremovable
   public CoreV1Api getCoreApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      CoreV1Api coreV1Api = coreApiMap.get(provider + "/" + region + "/" + superCluster);
      if (coreV1Api == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         coreV1Api = new CoreV1Api(apiClient);
         coreApiMap.put(provider + "/" + region + "/" + superCluster, coreV1Api);
      }
      return coreV1Api;
   }

   @Unremovable
   public AppsV1Api getAppsApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      AppsV1Api appsApi = appsApiMap.get(provider + "/" + region + "/" + superCluster);
      if (appsApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         appsApi = new AppsV1Api(apiClient);
         appsApiMap.put(provider + "/" + region + "/" + superCluster, appsApi);
      }
      return appsApi;
   }

   @Unremovable
   public DynamicKubernetesApi getClusterVersionApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi clusterVersionApi = clusterVersionApiMap.get(provider + "/" + region + "/" + superCluster);

      if (clusterVersionApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         clusterVersionApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
               "clusterversions", apiClient);
         clusterVersionApiMap.put(provider + "/" + region + "/" + superCluster, clusterVersionApi);
      }
      return clusterVersionApi;
   }

   @Unremovable
   public DynamicKubernetesApi getVirtualClusterApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi virtualClusterApi = virtualClusterApiMap.get(provider + "/" + region + "/" + superCluster);
      if (virtualClusterApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         virtualClusterApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
               "virtualclusters", apiClient);
         virtualClusterApiMap.put(provider + "/" + region + "/" + superCluster, virtualClusterApi);
      }
      return virtualClusterApi;
   }

   @Unremovable
   public DynamicKubernetesApi getSubnetApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi subnetApi = subnetApiMap.get(provider + "/" + region + "/" + superCluster);
      if (subnetApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         subnetApi = new DynamicKubernetesApi("kubeovn.io", "v1",
               "subnets", apiClient);
         subnetApiMap.put(provider + "/" + region + "/" + superCluster, subnetApi);
      }
      return subnetApi;
   }

   @Unremovable
   public DynamicKubernetesApi getMachineApi(String provider, String region, String superCluster)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi machineApi = machineApiMap.get(provider + "/" + region + "/" + superCluster);
      if (machineApi == null) {
         ApiClient apiClient = getAdminApiClient(provider, region, superCluster);
         machineApi = new DynamicKubernetesApi("cluster.k8s.io",
               "v1alpha1", "machines",
               apiClient);
         machineApiMap.put(provider + "/" + region + "/" + superCluster, machineApi);
      }
      return machineApi;
   }

}