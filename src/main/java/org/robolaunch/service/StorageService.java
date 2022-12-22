package org.robolaunch.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
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

    public Response createPricingFile(Organization organization) {
        try {
            storageRepository.createPricingFile(organization);
            return new Response(true, UUID.randomUUID().toString());
        } catch (Exception e) {
            storageLogger.error("Start time saving failed: " + e);
            return new Response(false, UUID.randomUUID().toString());
        }
    }

    public void addPricingStart(Organization organization, String teamId, String cloudInstanceName, String type) {
        try {
            storageRepository.addPricingStart(organization, teamId, cloudInstanceName, type);
            storageLogger.info("Start time is saved: " + organization.getName());
        } catch (Exception e) {
            storageLogger.error("Start time saving failed: " + e);
        }
    }

    public void addPricingStop(Organization organization, String teamId, String cloudInstanceName, String type) {
        try {
            storageRepository.addPricingStop(organization, teamId, cloudInstanceName, type);
            storageLogger.info("Start time is saved: " + organization.getName());
        } catch (Exception e) {
            storageLogger.error("Start time saving failed: " + e);
        }
    }

    public void infinispanConnect() {
        try {
            storageRepository.infinispanConnect();
        } catch (Exception e) {
        }
    }

}
