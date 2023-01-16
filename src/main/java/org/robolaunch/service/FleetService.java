package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.request.RequestFleet;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.FleetRepository;

import com.google.gson.Gson;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class FleetService {
   @Inject
   FleetRepository fleetRepository;

   @Inject
   Logger log;

   @LoggerName("fleetService")
   Logger fleetLogger;

   @Inject
   JsonWebToken jwt;

   public PlainResponse createFleet(RequestFleet requestFleet) {
      PlainResponse plainResponse = new PlainResponse();
      try {
         Gson gson = new Gson();
         String token = jwt.getRawToken();
         if (!requestFleet.isFederated()) {
            System.out.println("Fleet is not federated: " + gson.toJson(requestFleet));
            fleetRepository.createFleet(requestFleet, token);
         } else {
            System.out.println("Fleet is federated: " + gson.toJson(requestFleet));
            fleetRepository.createFederatedFleet(requestFleet, token);
         }
         fleetLogger.info("Fleet created");
         plainResponse.setMessage("Fleet created");
         plainResponse.setSuccess(true);
         return plainResponse;
      } catch (Exception e) {
         plainResponse.setMessage("Fleet could not be created");
         plainResponse.setSuccess(false);
         fleetLogger.error("Fleet could not be created");
      }
      return plainResponse;
   }
}
