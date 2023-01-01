package org.robolaunch.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.request.RequestCreateProvider;
import org.robolaunch.models.request.RequestCreateRegion;
import org.robolaunch.models.request.RequestCreateSuperCluster;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.StorageRepository;

import io.quarkus.arc.log.LoggerName;
import org.jboss.logging.Logger;

@ApplicationScoped
/** Storage service is responsible to store credentials of cluster. */
public class StorageService {
    @Inject
    Logger logger;

    @Inject
    StorageRepository storageRepository;

    @Inject
    GroupRepository groupRepository;

    @LoggerName("storageService")
    Logger storageLogger;

    public void listArtifacts() {
        try {
            storageRepository.list();
        } catch (Exception e) {
            storageLogger.error("List operation is failed. Reason: " + e);
        }
    }

    public void removeArtifact(Artifact artifact, String bucket) {
        try {
            storageRepository.remove(artifact, bucket);
        } catch (Exception e) {
            storageLogger.error("Remove object operation is failed: " + e);
        }
    }

    public void removeClusterArtifacts(Organization organization, String cloudInstanceName)
            throws ApplicationException {
        try {
            storageRepository.removeDir(new Artifact("name", cloudInstanceName), organization.getName());
        } catch (Exception e) {
            storageLogger.error("Remove objects operation is failed: " + e);
            throw new ApplicationException("Remove minio objects operation is failed, the objects may not exist.");
        }
    }

    public Response createBucketForOrganization(Organization organization) throws ApplicationException {
        try {
            Organization org = new Organization();
            org.setName(organization.getName().toLowerCase());
            org.setEnterprise(organization.isEnterprise());
            storageRepository.createBucket(org.getName());
            storageLogger.info("Bucket is created: " + organization.getName());
            return new Response(true, UUID.randomUUID().toString());
        } catch (Exception e) {
            storageLogger.error("Create bucket operation is failed: " + e);
            return new Response(false, UUID.randomUUID().toString());

        }
    }

    public PlainResponse createProvider(RequestCreateProvider requestCreateProvider) throws ApplicationException {
        PlainResponse plainResponse = new PlainResponse();
        plainResponse.setSuccess(true);
        return plainResponse;
    }

    public PlainResponse createRegion(RequestCreateRegion requestCreateRegion, String provider)
            throws ApplicationException {
        PlainResponse plainResponse = new PlainResponse();
        plainResponse.setSuccess(true);
        return plainResponse;
    }

    public PlainResponse createSuperCluster(RequestCreateSuperCluster requestCreateSuperCluster, String regionName,
            String providerName)
            throws ApplicationException {
        PlainResponse plainResponse = new PlainResponse();
        plainResponse.setSuccess(true);
        return plainResponse;
    }

    public String getSuperClusterContent(String provider, String region, String superCluster)
            throws ApplicationException {
        try {
            return storageRepository.getSuperClusterContent(provider, region, superCluster);
        } catch (Exception e) {
            storageLogger.error("Get content operation is failed: " + e);
            throw new ApplicationException("Get content operation is failed: " + e);
        }
    }

    public ArrayList<String> getSuperClusterBufferTypes(String provider, String region, String superCluster) {
        try {
            ArrayList<String> list = new ArrayList<String>();
            String content = storageRepository.getSuperClusterContent(provider, region, superCluster);
            String lines[] = content.split("\\r?\\n");
            // iterate over lines
            for (String line : lines) {
                String[] row = line.split(":");
                String type = row[0];
                list.add(type);
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

}
