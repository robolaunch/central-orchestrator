package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;

import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

public interface CloudInstanceRepository {

        public void createMachineDeployment(String bufferName, String instanceType, String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void claimTheSuperClusterNode(String nodeName, String bufferName, String region)
                        throws IOException, KubectlException, ApiException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException;

        public void createClusterVersion(String bufferName, String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException, InterruptedException;

        public void createVirtualCluster(String bufferName, String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException, InterruptedException;

        public void scaleStatefulSetsUp(String bufferName, String region)
                        throws ApiException, IOException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException;

        public void scaleStatefulSetsDown(String bufferName, String region)
                        throws ApiException, KubectlException, IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException;

        public void drainNode(String nodeName, String region)
                        throws IOException, KubectlException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException;

        public void uncordonNode(String nodeName, String region)
                        throws IOException, KubectlException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException;

        public void labelVirtualCluster(String bufferName, Organization organization, String teamId,
                        String region,
                        String cloudInstanceName, Boolean connectionHub)
                        throws IOException, KubectlException, InterruptedException, ApiException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
                        String teamId,
                        String region,
                        Boolean connectionHub)
                        throws IOException, KubectlException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, ApiException, InterruptedException, MinioException;

        public void addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
                        String teamId,
                        String cloudInstanceName, String region, String bufferName)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public void createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization, String region)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createVirtualLink(String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization, String region, String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createOauth2ProxyNamespace(String bufferName, String region)
                        throws ApiException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void createTLSSecrets(String bufferName, String region)
                        throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, InterruptedException, MinioException;

        public void createOAuth2ProxyResources(Organization organization, String teamId,
                        String cloudInstanceName,
                        String region, String namespaceName, String bufferName)
                        throws InvalidKeyException, ErrorResponseException,
                        InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        IOException, ApiException, JSONException, GeneralSecurityException, MinioException,
                        InterruptedException;

        public void createIngressRule(Organization organization, String cloudInstanceName,
                        String bufferName, String region)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, ApiException, MinioException,
                        InterruptedException;

        public void createCoreDNS(Organization organization, String teamId,
                        String cloudInstanceName, String nodeName,
                        String bufferName,
                        String region)
                        throws IOException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, ApiException, MinioException,
                        InterruptedException;

        public void addLabelsToVirtualClusterNode(Organization organization, String nodeName, String cloudInstanceName,
                        String teamId, String bufferName, String region, Boolean connectionHub)
                        throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void createCertManager(Organization organization, String teamId,
                        String cloudInstanceName, String bufferName,
                        String region) throws KubectlException, IOException, ApiException,
                        InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
                        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException,
                        IllegalArgumentException, MinioException, InterruptedException;

        public void createConnectionHubOperator(String namespaceName, String cloudInstanceName,
                        String teamId, Organization organization, String region,
                        String bufferName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        KubectlException, MinioException;

        public void createConnectionHub(String bufferName, Organization organization, String teamId,
                        String cloudInstanceName,
                        String serverIP,
                        String namespaceName, String region)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        MinioException;

        public void createRobotOperator(Organization organization, String cloudInstanceName,
                        String teamId,
                        String region,
                        String bufferName) throws InvalidKeyException, ErrorResponseException,
                        InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        IOException, MinioException, ApiException, KubectlException, InterruptedException;

        public void createDNSRecord(Organization organization, String nodeName, String region)
                        throws ApiException, InternalError, ApplicationException, IOException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException;

        public void addBufferedLabelToVC(String bufferName, String instanceType, String region)
                        throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void scaleVCWorkloadsDown(String bufferName, String region)
                        throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void scaleVCWorkloadsUp(String bufferName, String region)
                        throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void scaleOAuth2ProxyDown(String bufferName, String region)
                        throws ApiException, IOException, KubectlException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void scaleOAuth2ProxyUp(String bufferName, String region)
                        throws ApiException, IOException, KubectlException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void scaleCoreDNSUp(String bufferName, String region)
                        throws ApiException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void unlabelSuperClusterNode(String nodeName, String region)
                        throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void createClusterAdminRole(Organization organization, String bufferName, String username, String region)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, ApiException, InterruptedException,
                        MinioException;
}
