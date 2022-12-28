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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Cluster;
import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RequestCreateProvider;
import org.robolaunch.models.request.RequestCreateRegion;
import org.robolaunch.repository.abstracts.StorageRepository;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
    @Inject
    MinioClient minioClient;
    @Inject
    RemoteCacheManager remoteCacheManager;

    @Inject
    @Remote("test_domain")
    RemoteCache<String, Object> cache;

    /* Send objects from local computer */
    @Override
    public void push(byte[] content, Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalArgumentException, IOException {
        Path tempFile = Files.createTempFile("hello", ".yaml");
        Files.write(tempFile, content);
        UploadObjectArgs.Builder builder = UploadObjectArgs.builder().bucket(bucket)
                .object(artifact.getClusterName() + "/" + artifact.getName()).filename(tempFile.toString());
        minioClient.uploadObject(builder.build());

        /*
         * minioClient.uploadObject(UploadObjectArgs.builder().bucket(bucket)
         * .object(artifact.getClusterName() + "/" + artifact.getName())
         * .filename(artifactPath + "/" + artifact.getName()).build());
         */
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

    /* Get Template from "template-artifacts bucket" */
    @Override
    public JsonObject getContentJson(Artifact artifact, String bucket)
            throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalArgumentException, IOException {
        InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket)
                .object("/" + artifact.getName()).build());

        Yaml yaml = new Yaml();
        Iterable<Object> mData = yaml.loadAll(inputStream);
        for (Object data : mData) {
            String response = new Gson().toJson(data);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(data);
        }

        return null;
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
    public void createProvider(RequestCreateProvider requestCreateProvider)
            throws InvalidKeyException, ErrorResponseException,
            InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException, IllegalArgumentException, IOException {

        // Check if the provider already exists
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket("providers").build();
        Iterator<Result<Item>> iterator = minioClient.listObjects(listObjectsArgs).iterator();
        while (iterator.hasNext()) {
            Result<Item> item = iterator.next();
            System.out.println("provider: " + item.get().objectName());
            if (item.get().objectName().equals(requestCreateProvider.getName() + "/")) {
                throw new ApplicationException("Provider already exists.");
            }
        }

        // Create the provider
        PutObjectArgs.Builder provider = PutObjectArgs.builder().bucket("providers")
                .object(requestCreateProvider.getName() + "/").stream(new ByteArrayInputStream("".getBytes()), 0, -1);

        minioClient.putObject(provider.build());
    }

    @Override
    public void createRegion(RequestCreateRegion requestCreateRegion)
            throws InvalidKeyException, ErrorResponseException,
            InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException, IllegalArgumentException, IOException {

        // Check if the provider exists.
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket("providers").build();
        Iterator<Result<Item>> iterator = minioClient.listObjects(listObjectsArgs).iterator();
        while (iterator.hasNext()) {
            Result<Item> item = iterator.next();
            if (item.get().objectName().equals(requestCreateRegion.getProvider() + "/")) {
                System.out.println("Provider exists!");
                break;
            }
            if (!iterator.hasNext()) {
                throw new ApplicationException("Provider does not exist.");
            }
        }
        // Check if the region already exists
        while (iterator.hasNext()) {
            Result<Item> item = iterator.next();
            if (item.get().isDir() && item.get().objectName()
                    .equals(requestCreateRegion.getProvider() + "/" + requestCreateRegion.getName() + "/")) {
                throw new ApplicationException("Region already exists.");
            }
        }

        // Create the Region
        PutObjectArgs.Builder region = PutObjectArgs.builder().bucket("providers")
                .object(requestCreateRegion.getProvider() + "/" + requestCreateRegion.getName() + "/")
                .stream(new ByteArrayInputStream("".getBytes()), 0, -1);

        minioClient.putObject(region.build());
    }

}
