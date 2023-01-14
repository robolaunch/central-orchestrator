package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.models.request.RequestFleet;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.FleetRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.minio.errors.MinioException;

@ApplicationScoped
public class FleetRepositoryImpl implements FleetRepository {
      @Inject
      CloudInstanceHelperRepository cloudInstanceHelperRepository;

      @Inject
      ApiClientManager apiClientManager;

      @Override
      public void createFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException, InterruptedException, MinioException {
            Gson gson = new Gson();
            String bufferName = requestFleet.getBufferName();

            ApiClient vcApi = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName,
                        requestFleet.getProvider(), requestFleet.getRegion(), requestFleet.getSuperCluster());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(requestFleet);

            JsonObject fleetObject = gson.fromJson(json, JsonObject.class);
            JsonObject labelsObject = new JsonObject();
            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject().add("labels",
                        labelsObject);

            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject()
                        .get("labels")
                        .getAsJsonObject().addProperty("robolaunch.io/organization",
                                    requestFleet.getOrganization().getName());
            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                        .getAsJsonObject().addProperty("robolaunch.io/team",
                                    requestFleet.getTeamId());
            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                        .getAsJsonObject().addProperty("robolaunch.io/region",
                                    requestFleet.getRegion());
            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance",
                                    requestFleet.getBufferName());
            fleetObject.get("fleet").getAsJsonObject().get("metadata").getAsJsonObject().get("labels")
                        .getAsJsonObject().addProperty("robolaunch.io/cloud-instance-alias",
                                    requestFleet.getCloudInstanceName());

            JsonObject specObject = new JsonObject();
            specObject.add("discoveryServerTemplate",
                        fleetObject.get("fleet").getAsJsonObject().get("discoveryServerTemplate").getAsJsonObject());

            fleetObject.add("spec", specObject);
            fleetObject.remove("discoveryServerTemplate");

            fleetObject.get("fleet").getAsJsonObject().get("spec").getAsJsonObject().get("discoveryServerTemplate")
                        .getAsJsonObject().addProperty("cluster", requestFleet.getBufferName());

            JsonObject finalFleetObject = fleetObject.get("fleet").getAsJsonObject();

            CustomObjectsApi customObjectsApi = new CustomObjectsApi(vcApi);
            customObjectsApi.createClusterCustomObject("fleet.roboscale.io", "v1alpha1", "fleets", finalFleetObject,
                        null,
                        null, null);
      }

}
