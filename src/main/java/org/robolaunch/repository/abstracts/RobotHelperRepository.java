package org.robolaunch.repository.abstracts;

import io.kubernetes.client.openapi.ApiException;

public interface RobotHelperRepository {

   public String getAvailablePortRange(Integer requestedPortCount) throws ApiException;
}
