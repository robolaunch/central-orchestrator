package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Workspace;
import org.robolaunch.models.kubernetes.V1CloudRobot;
import org.robolaunch.models.kubernetes.V1CloudRobotList;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.KubernetesRepository;
import org.robolaunch.repository.abstracts.RobotRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.ModelMapper;
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
        System.out.println("Making robots passive..");
        ApiClient vcClient = cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
        DynamicKubernetesApi robotsApi = new DynamicKubernetesApi("robot.roboscale.io", "v1alpha1",
                "robots",
                vcClient);
        String patchString = "[{ \"op\": \"replace\", \"path\": \"/spec/robot/state\", \"value\": \"Passive\"}]";

        var robotList = robotsApi.list().getObject().getItems();
        V1Patch patch = new V1Patch(patchString);

        for (var robot : robotList) {
            System.out.println("Will be patched: " + robot.getMetadata().getName());
            robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
        }
        System.out.println("Robots are now passive");
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
            System.out.println("Will be patched: " + robot.getMetadata().getName());
            robotsApi.patch("default", robot.getMetadata().getName(), null, patch);
        }

    }

    @Override
    public void deployCloudRobot(Organization organization, String cloudInstanceName, String robotName,
            String distro, Integer storage, String cpu,
            String memory, List<Workspace> workspaces, String departmentName)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
            IOException {
        ModelMapper.addModelMap("robot.roboscale.io", "v1alpha1", "Robot", "robots", V1CloudRobot.class,
                V1CloudRobotList.class);

        Artifact artifact = new Artifact();
        artifact.setName("cloudRobot.yaml");
        String bucket = "template-artifacts";
        JsonObject object = storageRepository.getYamlTemplate(artifact, bucket);
        object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                "robolaunch.io/organization",
                organization.getName());
        object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                "robolaunch.io/cloud-instance",
                cloudInstanceName);
        object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                "robolaunch.io/department",
                departmentName);
        object.get("metadata").getAsJsonObject().get("labels").getAsJsonObject().addProperty(
                "robolaunch.io/super-cluster",
                "super-cluster");

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("nodeSelector")
                .getAsJsonObject()
                .addProperty(
                        "robolaunch.io/organization",
                        organization.getName());
        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("nodeSelector")
                .getAsJsonObject()
                .addProperty(
                        "robolaunch.io/cloud-instance",
                        cloudInstanceName);
        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("nodeSelector")
                .getAsJsonObject()
                .addProperty(
                        "robolaunch.io/department",
                        departmentName);
        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("nodeSelector")
                .getAsJsonObject()
                .addProperty(
                        "robolaunch.io/super-cluster",
                        "super-cluster");

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().addProperty("distro", distro);

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("resources").getAsJsonObject()
                .addProperty("storage", storage);

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("resources").getAsJsonObject()
                .addProperty("cpuPerContainer", cpu);

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject().get("resources").getAsJsonObject()
                .addProperty("memoryPerContainer", memory);

        String json = new Gson().toJson(workspaces);
        JsonElement jsonWorkspaces = JsonParser.parseString(json);

        object.get("spec").getAsJsonObject().get("robot").getAsJsonObject()
                .add("workspaces", jsonWorkspaces);

        /*
         * Kubectl.apply(V1CloudRobot.class).forceConflict(true)
         * .resource(cloudRobot).apiClient(kubernetesRepository.
         * getVirtualClusterClientWithBufferName).execute();
         */
    }
}
