package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.jboss.resteasy.spi.ApplicationException;
import org.robolaunch.model.account.Organization;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

public interface CloudInstanceHelperRepository {
        public String getCloudInstanceIP(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public String generateBufferName();

        public String getGeneratedMachineName(String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public Boolean nodeRefChecker(String bufferName, String machineName, String provider, String region,
                        String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public Boolean isVirtualClusterReady(String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public String selectBufferedVirtualCluster(Integer vcCount, String provider, String region, String superCluster)
                        throws KubectlException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, ApiException, MinioException;

        public String getNodeName(String machineName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public String selectNode(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, KubectlException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public Boolean isStatefulSetsUp(String namespaceName, String provider, String region, String superCluster)
                        throws ApiException, IOException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, InterruptedException, MinioException;

        public Boolean isStatefulSetsDown(String namespaceName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public Boolean isCoreDNSDeploymentUp(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public Boolean isCertManagerReady(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, IOException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public Boolean isNodeUnschedulable(String nodeName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public Boolean isNodeReady(String nodeName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public Boolean isSubnetUsed(String bufferName, String provider, String region, String superCluster)
                        throws InternalError, IOException, ApiException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, InterruptedException, MinioException;

        public String findNode(String bufferName, Organization organization, String teamId,
                        String cloudInstanceName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public Boolean isMachineCreated(String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public Boolean isConnectionHubOperatorReady(String bufferName, String provider, String region,
                        String superCluster, String namespaceName);

        public Boolean healthCheck(Organization organization, String teamId, String cloudInstanceName,
                        String nodeName, String provider, String region, String superCluster);

        public void deleteDNSRecord(Organization organization, String nodeName, String provider, String region,
                        String superCluster)
                        throws ApiException, JsonProcessingException, InternalError, ApplicationException, IOException,
                        InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, InterruptedException,
                        MinioException;

        public void deleteClusterVersion(String bufferName, String provider, String region, String superCluster)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;

        public void deleteOAuth2ProxyResources(String bufferName, String provider, String region, String superCluster)
                        throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, InterruptedException, MinioException;

        public void deleteVirtualCluster(String bufferName, String provider, String region, String superCluster)
                        throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void deleteSubnet(String bufferName, String provider, String region, String superCluster)
                        throws InternalError, IOException, ApiException, InvalidKeyException, NoSuchAlgorithmException,
                        IllegalArgumentException, InterruptedException, MinioException;

        public void deleteMachineDeployment(String bufferName, String provider, String region, String superCluster)
                        throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void deleteOrganizationLabelsFromSuperCluster(String nodeName, String provider, String region,
                        String superCluster)
                        throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void deleteWorkerLabelFromNode(String nodeName, String provider, String region, String superCluster)
                        throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public void deleteVirtualClusterNodes(String bufferName, String provider, String region, String superCluster)
                        throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public String getTeamIdFromProcessId(String processId, String provider, String region, String superCluster)
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public String convertJsonStringToYamlString(String jsonString)
                        throws JsonProcessingException, IOException;

        public String getNamespaceNameWithBufferName(String bufferName, String provider, String region,
                        String superCluster)
                        throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException,
                        IOException, InterruptedException, MinioException;

        public ApiClient getVirtualClusterClientWithBufferName(String bufferName, String provider, String region,
                        String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public Boolean doesCloudInstanceExist(Organization organization, String teamId,
                        String cloudInstanceName, String provider, String region, String superCluster)
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public ApiClient userApiClient(String bufferName, String token, String provider, String region,
                        String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public ApiClient adminApiClient(String provider, String region,
                        String superCluster)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        NoSuchAlgorithmException, IllegalArgumentException, MinioException;

        public String getAvailableCIDRBlock(String provider, String region, String superClutser)
                        throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
                        ApiException, InterruptedException, MinioException;
}
