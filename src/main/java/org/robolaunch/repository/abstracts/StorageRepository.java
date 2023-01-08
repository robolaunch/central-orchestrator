package org.robolaunch.repository.abstracts;

import java.io.IOException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Cluster;
import org.robolaunch.models.Organization;

import com.google.gson.JsonObject;

import io.minio.errors.BucketPolicyTooLargeException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface StorageRepository {
        void push(byte[] content, Artifact artifact, String bucket)
                        throws MinioException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException;

        JsonObject getYamlTemplate(Artifact artifact, String bucket)
                        throws MinioException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException;

        void remove(Artifact artifact, String bucket)
                        throws MinioException, InvalidKeyException, NoSuchAlgorithmException, IOException;

        List<Artifact> list() throws MinioException, InvalidKeyException,
                        NoSuchAlgorithmException, IOException;

        void removeDir(Artifact artifact, String bucket)
                        throws MinioException, InvalidKeyException, NoSuchAlgorithmException,
                        IOException;

        Boolean doesExist(Cluster cluster, String bucket)
                        throws MinioException, InvalidKeyException, IllegalArgumentException, NoSuchAlgorithmException,
                        IOException;

        void createBucket(String bucket)
                        throws MinioException, InvalidKeyException, NoSuchAlgorithmException, IOException,
                        IllegalArgumentException, ApplicationException;

        String getContent(Artifact artifact,
                        String bucket) throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;

        String getSuperClusterContent(String provider, String region, String superCluster)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;

        void createTemporaryBucketForRobot(String provider, String region, String superCluster,
                        Organization organization, String teamId, String physicalInstanceName)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;

        void createBucketPolicyForRobotBucket(String provider, String region, String superCluster,
                        Organization organization, String teamId, String physicalInstanceName)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;

        void minioTest() throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, BucketPolicyTooLargeException,
                        InvalidCipherTextException;

}
