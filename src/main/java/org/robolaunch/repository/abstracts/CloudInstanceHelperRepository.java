package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.jboss.resteasy.spi.ApplicationException;
import org.robolaunch.models.Organization;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.smallrye.graphql.execution.ExecutionException;

public interface CloudInstanceHelperRepository {
        public String getCloudInstanceIP(String bufferName) throws ApiException;

        public String generateBufferName();

        public void bufferCall(String instanceType) throws MalformedURLException, IOException;

        public void CIOperationCall(String processId, String operation) throws IOException;

        public String getGeneratedMachineName(String bufferName);

        public Boolean nodeRefChecker(String bufferName, String machineName) throws ApiException;

        public Boolean isVirtualClusterReady(String bufferName);

        public Integer getBufferingVirtualClusterCount(String instanceType)
                        throws ExecutionException, InterruptedException, java.util.concurrent.ExecutionException;

        public Integer getBufferedVirtualClusterCount(String instanceType);

        public String selectBufferedVirtualCluster(Integer vcCount)
                        throws KubectlException, IOException, InterruptedException;

        public String getNodeName(String machineName);

        public String selectNode(String bufferName)
                        throws ApiException, KubectlException, IOException, InterruptedException;

        public Boolean isStatefulSetsUp(String namespaceName) throws ApiException, IOException;

        public Boolean isStatefulSetsDown(String namespaceName) throws ApiException;

        public Boolean isCoreDNSDeploymentUp(String bufferName) throws IOException, ApiException, InterruptedException;

        public Boolean isCertManagerReady(String namespaceName) throws ApiException, IOException;

        public Boolean isNodeUnschedulable(String nodeName) throws ApiException;

        public Boolean isNodeReady(String nodeName) throws ApiException;

        public Boolean isSubnetUsed(String bufferName) throws InternalError, IOException, ApiException;

        public String findNode(String bufferName, Organization organization, String departmentName,
                        String cloudInstanceName) throws ApiException;

        public Boolean isMachineCreated(String bufferName);

        public Boolean healthCheck(Organization organization, String departmentName, String cloudInstanceName,
                        String nodeName);

        public void deleteDNSRecord(Organization organization, String nodeName)
                        throws ApiException, JsonProcessingException, InternalError, ApplicationException, IOException;

        public void deleteClusterVersion(String bufferName);

        public void deleteOAuth2ProxyResources(String bufferName)
                        throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
                        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
                        XmlParserException, IllegalArgumentException, IOException, InterruptedException;

        public void deleteVirtualCluster(String bufferName) throws KubectlException, IOException;

        public void deleteSubnet(String bufferName) throws InternalError, IOException, ApiException;

        public void deleteMachineDeployment(String bufferName) throws IOException, KubectlException;

        public void deleteOrganizationLabelsFromSuperCluster(String nodeName) throws IOException, KubectlException;

        public void deleteWorkerLabelFromNode(String nodeName) throws IOException, KubectlException;

        public void deleteVirtualClusterNodes(String bufferName)
                        throws IOException, ApiException, InterruptedException, KubectlException;

        public String getTeamIdFromProcessId(String processId)
                        throws java.util.concurrent.ExecutionException, InterruptedException;

        public String convertJsonStringToYamlString(String jsonString) throws JsonProcessingException, IOException;

        public String getNamespaceNameWithBufferName(String bufferName) throws ApiException;

        public ApiClient getVirtualClusterClientWithBufferName(String bufferName)
                        throws IOException, ApiException, InterruptedException;

        public Boolean doesCloudInstanceExist(Organization organization, String teamId,
                        String cloudInstanceName) throws java.util.concurrent.ExecutionException, InterruptedException;

        public void createCRB(String bufferName)
                        throws IOException, ApiException, InterruptedException, InvalidKeyException,
                        ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
                        NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException;

        public ApiClient userApiClient(String bufferName, String token)
                        throws IOException, ApiException, InterruptedException;

        public void testingUserApiClient(String bufferName, String token)
                        throws IOException, ApiException, InterruptedException;
}
