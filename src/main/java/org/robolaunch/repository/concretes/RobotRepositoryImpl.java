package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.google.gson.JsonObject;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.minio.errors.MinioException;

@ApplicationScoped
public class RobotRepositoryImpl implements RobotRepository {

        @Inject
        CloudInstanceHelperRepository cloudInstanceHelperRepository;

        @Inject
        StorageRepository storageRepository;

        @Override
        public void makeRobotsPassive(String bufferName)
                        throws IOException, ApiException, InterruptedException {
                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                DynamicKubernetesApi robotsApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robots",
                                vcClient);
                String patchString = "[{ \"op\": \"replace\", \"path\": \"/spec/robot/state\", \"value\": \"Passive\"}]";

                var robotList = robotsApi.list().getObject().getItems();
                V1Patch patch = new V1Patch(patchString);

                for (var robot : robotList) {
                        robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
                }
        }

        @Override
        public void makeRobotsActive(
                        String bufferName)
                        throws IOException, ApiException, InterruptedException {

                ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
                DynamicKubernetesApi robotsApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                                "robots",
                                vcClient);
                String patchString = "[{ \"op\": \"replace\", \"path\": \"/spec/robot/state\", \"value\": \"Launched\"}]";

                var robotList = robotsApi.list().getObject().getItems();
                V1Patch patch = new V1Patch(patchString);

                for (var robot : robotList) {
                        robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
                }

        }

        public void createRobot(Organization organization, String teamId, String region, String cloudInstance)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException {
                // Get template Robot YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robot.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        }

        public void createRobotLaunchManager(Organization organization, String teamId, String region,
                        String cloudInstance) throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException {
                // Get template RobotLaunchManager YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robot.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        }

        public void createRobotBuildManager(Organization organization, String teamId, String region,
                        String cloudInstance) throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException {
                // Get template RobotBuildManager YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotBuildManager.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        }

        public void createRobotDevelopmentSuite(Organization organization, String teamId, String region,
                        String cloudInstance) throws InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, MinioException, IOException {
                // Get template RobotDevelopmentSuite YAML from MINIO.
                Artifact artifact = new Artifact();
                artifact.setName("robotDevelopmentSuite.yaml");
                String bucket = "template-artifacts";
                JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        }

}
