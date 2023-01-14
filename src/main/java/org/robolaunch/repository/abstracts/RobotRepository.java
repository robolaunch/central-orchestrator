package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.robolaunch.models.Organization;
import org.robolaunch.models.Robot;
import org.robolaunch.models.request.RequestRobot;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotLaunchManager;
import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface RobotRepository {

        public void createRobot(RequestRobot requestRobot, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String token,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName, String token,
                        String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void makeRobotsPassive(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void makeRobotsActive(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public String hybridRobotScript(String provider, String region, String superCluster, Organization organization,
                        String teamId, String bufferName,
                        String cloudInstanceName, String physicalInstanceName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public ArrayList<Robot> getRobotsOrganization(Organization organization);

}
