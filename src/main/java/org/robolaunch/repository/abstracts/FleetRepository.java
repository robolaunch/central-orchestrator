package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.robolaunch.models.request.RequestFleet;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface FleetRepository {
      public void createFleet(RequestFleet requestFleet, String token)
                  throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                  ApiException,
                  InterruptedException, MinioException;
}
