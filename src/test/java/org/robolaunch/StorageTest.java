package org.robolaunch;

import org.junit.jupiter.api.Test;
import org.robolaunch.model.account.Organization;
import org.robolaunch.service.StorageService;

import javax.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StorageTest {

  @Inject
  StorageService storageService;

  /*
   * @Test
   * public void testStorage() {
   * Organization organization = new Organization();
   * organization.setName("org-divides");
   * String teamId = "org-divides-dep-nkfnplnv";
   * storageService.roboticsCloudSaveStartTime(organization, teamId);
   * }
   */
}
