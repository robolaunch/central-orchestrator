package org.robolaunch.repository.concretes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.minio.MinioAdminClient;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Cluster;
import org.robolaunch.models.Organization;
import org.robolaunch.repository.abstracts.StorageRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import io.quarkus.infinispan.client.Remote;

@ApplicationScoped
public class StorageRepositoryImpl implements StorageRepository {
    private MinioClient minioClient;

    @Inject
    RemoteCacheManager remoteCacheManager;

    @Inject
    @Remote("test_domain")
    RemoteCache<String, Object> cache;

    @ConfigProperty(name = "quarkus.minio.url")
    String minioURL;

    @ConfigProperty(name = "quarkus.minio.access-key")
    String accessKey;

    @ConfigProperty(name = "quarkus.minio.secret-key")
    String secretKey;

    @ConfigProperty(name = "quarkus.minio.admin.api.url")
    String minioAdminApiURL;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder().endpoint(minioURL).credentials(accessKey, secretKey).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void push(byte[] content, Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalArgumentException, IOException {
        Path tempFile = Files.createTempFile("hello", ".yaml");
        Files.write(tempFile, content);
        UploadObjectArgs.Builder builder = UploadObjectArgs.builder().bucket(bucket)
                .object(artifact.getClusterName() + "/" + artifact.getName()).filename(tempFile.toString());
        minioClient.uploadObject(builder.build());

    }

    /* Get Template from "template-artifacts bucket" */
    @Override
    public JsonObject getYamlTemplate(Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalArgumentException, IOException {
        InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket)
                .object("/" + artifact.getName()).build());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        String data = "";
        while ((line = bufferedReader.readLine()) != null) {
            data += line + "\n";
        }

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        var object = yamlMapper.readValue(data, Object.class);
        ObjectMapper jsonMapper = new ObjectMapper();
        var jsonString = jsonMapper.writeValueAsString(object);

        return new Gson().fromJson(jsonString, JsonObject.class);
    }

    /* Get text content of the object on minio. */
    @Override
    public String getContent(Artifact artifact, String bucket)
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
            InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException,
            IllegalArgumentException, IOException {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(artifact.getName()).build());

        // Read the input stream and print to the console till EOF.
        String yaml = "";
        byte[] buf = new byte[16384];
        int bytesRead;
        while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
            yaml += new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
        }

        // Close the input stream.
        stream.close();

        return yaml;
    }

    /* Delete an object from minio. */
    @Override
    public void remove(Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException, IOException {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(artifact.getName()).build());
    }

    /* Get list of buckets. */
    @Override
    public List<Artifact> list() throws MinioException, InvalidKeyException,
            NoSuchAlgorithmException, IOException {
        List<Bucket> buckets = null;
        List<Artifact> artifacts = new ArrayList<Artifact>();
        buckets = minioClient.listBuckets();
        for (Bucket bucket : buckets) {
            artifacts.add(new Artifact(bucket.name(), ""));
        }
        return artifacts;

    }

    @Override
    public void removeDir(Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
            IOException {
        Iterable<Result<Item>> items = minioClient
                .listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
        for (Result<Item> item : items) {
            if (item.get().objectName().contains(artifact.getClusterName()))
                remove(new Artifact(item.get().objectName(), artifact.getClusterName()),
                        bucket);
        }
    }

    @Override
    public Boolean doesExist(Cluster cluster, String bucket)
            throws MinioException, InvalidKeyException, IllegalArgumentException,
            NoSuchAlgorithmException, IOException {
        Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
        for (Result<Item> result : items) {
            if (result.get().objectName().equals(cluster.getName() + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createBucket(String bucket) throws InvalidKeyException, ErrorResponseException,
            InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException, IllegalArgumentException, IOException, ApplicationException {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    @Override
    public String getSuperClusterContent(String provider, String region, String superCluster)
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
            InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException,
            IllegalArgumentException, IOException {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket("providers")
                        .object(provider + "/" + region + "/" + superCluster + "/" + provider + "_" + region + "_"
                                + superCluster + "_" + "config" + "_" + "buffer" + ".txt")
                        .build());

        // Read the input stream and print to the console till EOF.
        String content = "";
        byte[] buf = new byte[16384];
        int bytesRead;
        while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
            content += new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
        }

        // Close the input stream.
        stream.close();

        return content;
    }

    @Override
    public void createMinioFileForRobotScript(String provider, String region, String superCluster,
            Organization organization, String teamId, String physicalInstanceName, String script, String username)
            throws InvalidKeyException,
            ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException, IOException {

        String bucketName = "users";
        String objectName = provider + "-" + region + "-" + superCluster + "-" + organization.getName()
                + "-" + teamId + "-" + physicalInstanceName + ".sh";

        Path tempFile = Files.createTempFile("temp", ".txt");
        Files.write(tempFile, script.getBytes());
        PutObjectArgs poa = PutObjectArgs.builder().bucket(bucketName).object(username + "/" + objectName)
                .stream(new ByteArrayInputStream(script.getBytes()), script.getBytes().length, -1).build();
        minioClient.putObject(poa);
    }

    @Override
    public void createPolicyForUser(String username) throws InvalidKeyException,
            ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException, IOException {
        MinioAdminClient minioAdminClient = MinioAdminClient.builder()
                .endpoint(minioAdminApiURL).credentials(accessKey,
                        secretKey)
                .build();
        String policy = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Action\": [\"s3:GetBucketLocation\",\"s3:GetObject\"],\"Resource\": [\"arn:aws:s3:::users/"
                + username + "/*" + "/]}]}";
        if (username != null) {
            minioAdminClient.addCannedPolicy(username, policy);
        }

    }

    @Override
    public void assignPolicyToUser(String username) throws InvalidKeyException,
            ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException, IOException {
        MinioAdminClient minioAdminClient = MinioAdminClient.builder()
                .endpoint(minioAdminApiURL).credentials(accessKey,
                        secretKey)
                .build();
        if (username != null) {
            minioAdminClient.setPolicy("uid=" + username + ",cn=users,cn=accounts,dc=robolaunch,dc=dev", false,
                    username);
        }
    }

    @Override
    public String generateUserScript(String provider, String region, String superCluster, Organization organization,
            String teamId, String physicalInstanceName, String username) throws InvalidKeyException,
            ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
            NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException, IOException {
        Artifact artifact = new Artifact("template_script.sh", "");
        String bucketName = "template-artifacts";
        String scriptContent = getContent(artifact, bucketName);
        return null;
    }

}