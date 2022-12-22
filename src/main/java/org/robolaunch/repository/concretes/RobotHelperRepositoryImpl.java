package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotHelperRepository;

import com.google.gson.Gson;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;

@ApplicationScoped
public class RobotHelperRepositoryImpl implements RobotHelperRepository {
   private ApiClient adminApiClient;
   private CoreV1Api coreV1Api;

   @Inject
   CloudInstanceHelperRepository cloudInstanceHelperRepository;

   @PostConstruct
   public void initializeApis() throws IOException, ApiException, InterruptedException {
      this.adminApiClient = cloudInstanceHelperRepository.adminApiClient();
      this.coreV1Api = new CoreV1Api(adminApiClient);
   }

   @Override
   public String getAvailablePortRange(Integer requestedPortCount) throws ApiException {
      List<V1Service> services = coreV1Api
            .listServiceForAllNamespaces(null, null, null, null, null, null,
                  null, null, null, null)
            .getItems();

      List<Integer> ports = new ArrayList<Integer>();

      for (V1Service service : services) {
         Optional<String> type = Optional.ofNullable(service).map(V1Service::getSpec)
               .map(V1ServiceSpec::getType);
         Optional<List<V1ServicePort>> servicePorts = Optional.ofNullable(service)
               .map(V1Service::getSpec)
               .map(V1ServiceSpec::getPorts);
         // Check the spec -> type -> NodePort
         if (type.get().equals("NodePort")) {
            ports.add(servicePorts.get().get(0).getNodePort());
         }
      }
      Gson gson = new Gson();

      System.out.println("Ports: " + gson.toJson(ports));

      List<Integer> selectedPorts = new ArrayList<Integer>();
      Integer currentPort = 30000;
      while (selectedPorts.size() < requestedPortCount) {
         if (!ports.contains(currentPort)) {
            selectedPorts.add(currentPort);
         } else {
            selectedPorts.clear();
         }
         currentPort++;
      }
      String webRTCPorts = selectedPorts.get(0) + "-" + selectedPorts.get(requestedPortCount - 1);

      return webRTCPorts;
   }
}
