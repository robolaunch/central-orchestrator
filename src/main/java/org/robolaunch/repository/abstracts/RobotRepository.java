package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface RobotRepository {

        public void createRobot(Organization organization, String teamId, String region, String cloudInstance)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException;

        public void createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotDevelopmentSuite(RobotDevSuite robotDevSuite, String bufferName, String token)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void makeRobotsPassive(String bufferName) throws IOException, ApiException, InterruptedException;

        public void makeRobotsActive(String bufferName) throws IOException, ApiException, InterruptedException;
}
