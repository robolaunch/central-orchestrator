package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.robolaunch.models.request.RequestCreateRobot;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;

import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.MinioException;

public interface RobotRepository {

        public void createRobot(RequestCreateRobot requestCreateRobot, String token, String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String token,
                        String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName, String token,
                        String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createRobotDevelopmentSuite(RobotDevSuite robotDevSuite, String bufferName, String token,
                        String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void makeRobotsPassive(String bufferName, String region)
                        throws IOException, ApiException, InterruptedException;

        public void makeRobotsActive(String bufferName, String region)
                        throws IOException, ApiException, InterruptedException;
}
