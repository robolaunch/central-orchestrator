package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import org.robolaunch.model.request.RequestFleet;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface FleetRepository {
      public void createFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException,
                  InterruptedException, MinioException, ExecutionException;

      public void createFederatedFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException,
                  InterruptedException, MinioException, ExecutionException;
}
