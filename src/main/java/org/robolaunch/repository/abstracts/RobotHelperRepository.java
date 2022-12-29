package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface RobotHelperRepository {

   public String getAvailablePortRange(Integer requestedPortCount, String provider, String region, String superCluster)
         throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
         InterruptedException, MinioException;
}
