package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.robolaunch.models.Organization;
import org.robolaunch.models.Workspace;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface RobotRepository {
  public void makeRobotsPassive(String bufferName) throws IOException, ApiException, InterruptedException;

  public void makeRobotsActive(String bufferName) throws IOException, ApiException, InterruptedException;

  public void deployCloudRobot(Organization organization, String cloudInstanceName, String robotName,
      String distro, Integer storage, String cpu,
      String memory, List<Workspace> workspaces, String departmentName)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
      IOException;
}
