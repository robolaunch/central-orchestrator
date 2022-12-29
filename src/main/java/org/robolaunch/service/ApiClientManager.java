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
   private Map<String, CoreV1Api> coreApiMap = new ConcurrentHashMap<>();
   private Map<String, AppsV1Api> appsApiMap = new ConcurrentHashMap<>();

   private Map<String, DynamicKubernetesApi> machineDeploymentApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> clusterVersionApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> virtualClusterApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> subnetApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> machineApiMap = new ConcurrentHashMap<>();

   @Unremovable
   public ApiClient getAdminApiClient(String region) throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      ApiClient apiClient = apiClientMap.get(region);
      if (apiClient == null) {
         System.out.println("will create a new vc");
         apiClient = cloudInstanceHelperRepository.adminApiClient(region);
         apiClientMap.put(region, apiClient);
      }
      return apiClient;
   }

   @Unremovable
   public DynamicKubernetesApi getMachineDeploymentApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi machineDeploymentApi = machineDeploymentApiMap.get(region);
      if (machineDeploymentApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         machineDeploymentApi = new DynamicKubernetesApi("cluster.k8s.io", "v1alpha1",
               "machinedeployments",
               apiClient);
         machineDeploymentApiMap.put(region, machineDeploymentApi);
      }
      return machineDeploymentApi;
   }

   @Unremovable
   public CoreV1Api getCoreApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      CoreV1Api coreV1Api = coreApiMap.get(region);
      if (coreV1Api == null) {
         ApiClient apiClient = getAdminApiClient(region);
         coreV1Api = new CoreV1Api(apiClient);
         coreApiMap.put(region, coreV1Api);
      }
      return coreV1Api;
   }

   @Unremovable
   public AppsV1Api getAppsApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      AppsV1Api appsApi = appsApiMap.get(region);
      if (appsApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         appsApi = new AppsV1Api(apiClient);
         appsApiMap.put(region, appsApi);
      }
      return appsApi;
   }

   @Unremovable
   public DynamicKubernetesApi getClusterVersionApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi clusterVersionApi = clusterVersionApiMap.get(region);

      if (clusterVersionApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         clusterVersionApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
               "clusterversions", apiClient);
         clusterVersionApiMap.put(region, clusterVersionApi);
      }
      return clusterVersionApi;
   }

   @Unremovable
   public DynamicKubernetesApi getVirtualClusterApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi virtualClusterApi = virtualClusterApiMap.get(region);
      if (virtualClusterApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         virtualClusterApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
               "virtualclusters", apiClient);
         virtualClusterApiMap.put(region, virtualClusterApi);
      }
      return virtualClusterApi;
   }

   @Unremovable
   public DynamicKubernetesApi getSubnetApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi subnetApi = subnetApiMap.get(region);
      if (subnetApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         subnetApi = new DynamicKubernetesApi("kubeovn.io", "v1",
               "subnets", apiClient);
         subnetApiMap.put(region, subnetApi);
      }
      return subnetApi;
   }

   @Unremovable
   public DynamicKubernetesApi getMachineApi(String region)
         throws InvalidKeyException, NoSuchAlgorithmException,
         IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
      DynamicKubernetesApi machineApi = machineApiMap.get(region);
      if (machineApi == null) {
         ApiClient apiClient = getAdminApiClient(region);
         machineApi = new DynamicKubernetesApi("cluster.k8s.io",
               "v1alpha1", "machines",
               apiClient);
         machineApiMap.put(region, machineApi);
      }
      return machineApi;
   }
}