package org.robolaunch.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
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
   private Map<String, DynamicKubernetesApi> machineDeploymentApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> clusterVersionApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> virtualClusterApiMap = new ConcurrentHashMap<>();
   private Map<String, DynamicKubernetesApi> subnetApiMap = new ConcurrentHashMap<>();

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
}