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

        public void createMachineDeployment(String bufferName, String instanceType)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException;

        public void claimTheSuperClusterNode(String nodeName, String bufferName)
                        throws IOException, KubectlException, ApiException;

        public void createClusterVersion(String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException;

        public void createVirtualCluster(String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException,
                        ApiException;

        public void scaleStatefulSetsUp(String bufferName) throws ApiException, IOException, KubectlException;

        public void scaleStatefulSetsDown(String bufferName) throws ApiException, KubectlException, IOException;

        public void drainNode(String nodeName) throws IOException, KubectlException;

        public void uncordonNode(String nodeName) throws IOException, KubectlException;

        public void labelVirtualCluster(String bufferName, Organization organization, String departmentName,
                        String superClusterName,
                        String cloudInstanceName, Boolean connectionHub)
                        throws IOException, KubectlException, InterruptedException;

        public void addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
                        String departmentName,
                        String superClusterName,
                        Boolean connectionHub)
                        throws IOException, KubectlException;

        public void addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
                        String departmentName,
                        String cloudInstanceName, String superClusterName, String bufferName) throws ApiException;

        public void createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException;

        public void createVirtualLink(String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName, String bufferName)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, MinioException,
                        IOException, ApiException, InterruptedException;

        public void createOauth2ProxyNamespace(String bufferName)
                        throws ApiException, IOException, InterruptedException;

        public void createTLSSecrets(String bufferName)
                        throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, InterruptedException;

        public void createOAuth2ProxyResources(Organization organization, String departmentName,
                        String cloudInstanceName,
                        String superClusterName, String namespaceName, String bufferName)
                        throws InvalidKeyException, ErrorResponseException,
                        InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        IOException, ApiException, JSONException, GeneralSecurityException, MinioException,
                        InterruptedException;

        public void createIngressRule(Organization organization, String cloudInstanceName,
                        String bufferName)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, ApiException, MinioException,
                        InterruptedException;

        public void createCoreDNS(Organization organization, String departmentName,
                        String cloudInstanceName, String nodeName,
                        String bufferName,
                        String superClusterName)
                        throws IOException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, ApiException, MinioException,
                        InterruptedException;

        public void addLabelsToVirtualClusterNode(Organization organization, String nodeName, String cloudInstanceName,
                        String departmentName, String bufferName, String superClusterName, Boolean connectionHub)
                        throws KubectlException, IOException, ApiException, InterruptedException;

        public void createCertManager(Organization organization, String departmentName,
                        String cloudInstanceName, String bufferName,
                        String superClusterName) throws KubectlException, IOException, ApiException,
                        InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
                        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException,
                        IllegalArgumentException, MinioException, InterruptedException;

        public void createConnectionHubOperator(String namespaceName, String cloudInstanceName,
                        String departmentName, Organization organization, String superClusterName,
                        String bufferName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        KubectlException, MinioException;

        public void createConnectionHub(String bufferName, Organization organization, String departmentName,
                        String cloudInstanceName,
                        String serverIP,
                        String namespaceName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        MinioException;

        public void createRobotOperator(Organization organization, String cloudInstanceName,
                        String departmentName,
                        String superClusterName,
                        String bufferName) throws InvalidKeyException, ErrorResponseException,
                        InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException,
                        IOException, MinioException, ApiException, KubectlException, InterruptedException;

        public void createDNSRecord(Organization organization, String nodeName)
                        throws ApiException, InternalError, ApplicationException, IOException;

        public void addBufferedLabelToVC(String bufferName, String instanceType) throws KubectlException, IOException;

        public void scaleVCWorkloadsDown(String bufferName)
                        throws IOException, ApiException, InterruptedException, KubectlException;

        public void scaleVCWorkloadsUp(String bufferName)
                        throws IOException, ApiException, InterruptedException, KubectlException;

        public void scaleOAuth2ProxyDown(String bufferName) throws ApiException, IOException, KubectlException;

        public void scaleOAuth2ProxyUp(String bufferName) throws ApiException, IOException, KubectlException;

        public void scaleCoreDNSUp(String bufferName) throws ApiException, IOException, InterruptedException;

        public void unlabelSuperClusterNode(String nodeName) throws IOException, KubectlException;

        public void createClusterAdminRole(Organization organization, String bufferName, String username)
                        throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, ApiException, InterruptedException;
}
