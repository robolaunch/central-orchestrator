package org.robolaunch.repository.abstracts;

import java.io.IOException;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Cluster;
import org.robolaunch.models.Organization;

import com.google.gson.JsonObject;

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

        JsonObject getContentJson(Artifact artifact, String bucket)
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

        void createPricingFile(Organization organization)
                        throws IOException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException;

        public void addPricingStart(Organization organization, String teamId, String cloudInstanceName, String type)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;

        public void addPricingStop(Organization organization, String teamId, String cloudInstanceName, String type)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException;
}
