package org.robolaunch;

import org.robolaunch.service.TeamService;

import com.google.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class TeamTest {

  @Inject
  TeamService departmentService;

  /*
   * @Test
   * public void getTeamManagers() {
   * 
   * 
   * }
   */
}
