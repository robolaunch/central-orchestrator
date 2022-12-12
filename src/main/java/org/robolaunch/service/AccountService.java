package org.robolaunch.service;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.robolaunch.core.abstracts.IPAAdmin;
import org.robolaunch.core.concretes.IPAAdminLogin;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.CurrentUser;
import org.robolaunch.models.Department;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.InvitedUser;
import org.robolaunch.models.LoginRefreshToken;
import org.robolaunch.models.LoginRefreshTokenOrganization;
import org.robolaunch.models.LoginRequest;
import org.robolaunch.models.LoginRequestOrganization;
import org.robolaunch.models.LoginResponse;
import org.robolaunch.models.LoginResponseWithIPA;
import org.robolaunch.models.Organization;
import org.robolaunch.models.RegisteredDepartment;
import org.robolaunch.models.Response;
import org.robolaunch.models.Result;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import com.google.gson.Gson;

import io.quarkus.arc.log.LoggerName;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AccountService {
    @Inject
    KeycloakRepository keycloakRepository;

    @Inject
    KeycloakAdminRepository keycloakAdminRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupService groupService;

    @Inject
    UserAdminRepository userAdminRepository;

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupAdminRepository groupAdminRepository;

    @Inject
    MailService mailService;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "frontend.url")
    String frontendUrl;

    @LoggerName("accountService")
    Logger accountLogger;

    public void timer() {
        System.out.println("timer");
        accountLogger.info("Timer is working");
    }

    public Result notAuthorizedHandler() {
        return new Result("Authorization error. Please make sure that you have the permissions for this operation.",
                false);
    }

    public Result ipaErrorHandler(Response response) {
        accountLogger.error("Error: " + response.getResourceId());
        return new Result(response.getResourceId(), false);
    }

    public Result errorHandler() {
        return new Result("Error creating user", false);
    }

    public LoginResponse userLogin(LoginRequest loginRequest) throws InterruptedException, ExecutionException {
        try {
            LoginResponseWithIPA response = keycloakRepository.login(loginRequest);
            List<HttpCookie> cookies = response.getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
            System.out.println("Res: " + response);
            return response.getLoginResponse().get();
        } catch (Exception e) {
            accountLogger.error("Error logging in user", e);
            throw new ApplicationException("Error logging in user" + e.getMessage() + " : " + e.getLocalizedMessage());
        }
    }

    public LoginResponse userLoginOrganization(LoginRequestOrganization loginRequestOrganization)
            throws InterruptedException, ExecutionException {
        try {
            LoginResponseWithIPA response = keycloakRepository.loginOrganization(loginRequestOrganization);
            System.out.println("Res: " + response);
            List<HttpCookie> cookies = response.getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
            return response.getLoginResponse().get();
        } catch (Exception e) {
            accountLogger.error("Error logging in user", e);
            throw new ApplicationException("Error logging in user" + e.getMessage() + " : " + e.getLocalizedMessage());
        }
    }

    public void userIPALogin(LoginRequest loginRequest) throws ApplicationException {
        try {
            List<HttpCookie> cookies = keycloakRepository.login(loginRequest).getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
        } catch (Exception e) {
            accountLogger.error("Error when getCookies: " + e);
            throw new ApplicationException("An error occured.");
        }
    }

    public void IPALogin(LoginRequest loginRequest) throws ApplicationException {
        try {
            List<HttpCookie> cookies = keycloakRepository.login(loginRequest).getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
        } catch (Exception e) {
            accountLogger.error("Error when getCookies: " + e);
            throw new ApplicationException("An error occured. Please try again.");
        }
    }

    public LoginResponse login(LoginRequest loginRequest) throws ApplicationException {
        try {
            CompletableFuture<LoginResponse> response = keycloakRepository.login(loginRequest).getLoginResponse();
            System.out.println("rrrrresponse: " + response.get());
            LoginResponse finalResp = response.get();
            accountLogger.info("Login successful");
            return finalResp;
        } catch (Exception | InternalError e) {
            accountLogger.error("Error when login: " + e.getMessage() + e.getStackTrace() + e.getLocalizedMessage());
            throw new ApplicationException("Invalid username or password.");
        }
    }

    public Result userChangePassword(String oldPassword, String newPassword)
            throws ApplicationException, IOException {
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            userRepository.changePassword(oldPassword, newPassword, user.getUsername());
            accountLogger.info("User password updated.");
            return new Result("Password changed successfully", true);
        } catch (ApplicationException e) {
            accountLogger.error("Error changing password: ");
            return new Result("Error changing password. Please try again.", false);

        }
    }

    public void logout() {
        try {
            userRepository.clearCookies();
            groupRepository.clearCookies();
            userAdminRepository.clearCookies();
            groupAdminRepository.clearCookies();
        } catch (Exception e) {
            accountLogger.error("Error when logout: " + e);
        }
    }

    public void grantUserAdminPrivileges() {
        try {
            IPAAdmin adminLogin = new IPAAdminLogin();
            List<HttpCookie> cookies = adminLogin.login();
            cookies.forEach(cookie -> {
                userAdminRepository.appendCookie(cookie);
            });
            accountLogger.info("User Admin privileges granted");
        } catch (Exception e) {
            accountLogger.error("Error happened when granting admin privileges " + e.getMessage());
        }
    }

    public void grantAdminPrivileges() {
        try {
            IPAAdmin adminLogin = new IPAAdminLogin();
            List<HttpCookie> cookies = adminLogin.login();
            cookies.forEach(cookie -> {
                groupAdminRepository.appendCookie(cookie);
                userAdminRepository.appendCookie(cookie);
            });
            accountLogger.info("Admin privileges granted");
        } catch (Exception e) {
            accountLogger.error("Error happened when granting admin privileges " + e.getMessage());
        }
    }

    public GroupMember currentUser(Organization organization) {
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            GroupMember returnedUser = new GroupMember();
            returnedUser.setUsername(jwt.getClaim("preferred_username"));
            returnedUser.setEmail(jwt.getClaim("email"));
            returnedUser.setFirstName(jwt.getClaim("given_name"));
            returnedUser.setLastName(jwt.getClaim("family_name"));
            returnedUser.setAdmin(groupRepository.isGroupManager(user, organization));
            accountLogger.info("Current user: " + returnedUser);
            return returnedUser;
        } catch (Exception e) {
            accountLogger.error("Error happened when getting current user " + e.getMessage());
        }
        return null;
    }

    public CurrentUser getBasicCurrentUser() {
        User user = new User();
        user.setUsername(jwt.getClaim("preferred_username"));
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUsername(jwt.getClaim("preferred_username"));
        currentUser.setEmail(jwt.getClaim("email"));
        currentUser.setFirstName(jwt.getClaim("given_name"));
        currentUser.setLastName(jwt.getClaim("family_name"));

        return currentUser;
    }

    public CurrentUser getCurrentUser(Organization organization) throws ApplicationException {
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            CurrentUser currentUser = new CurrentUser();
            currentUser.setUsername(jwt.getClaim("preferred_username"));
            currentUser.setEmail(jwt.getClaim("email"));
            currentUser.setFirstName(jwt.getClaim("given_name"));
            currentUser.setLastName(jwt.getClaim("family_name"));

            currentUser.setAdmin(groupRepository.isGroupManager(user, organization));
            List<Department> departments = groupRepository.getTeams(organization, "member_group");
            Iterator<Department> it = departments.iterator();
            ArrayList<RegisteredDepartment> depts = new ArrayList<RegisteredDepartment>();
            while (it.hasNext()) {
                Department d = it.next();
                Organization org = new Organization();
                org.setName(d.getId());
                if (groupRepository.isGroupMember(user, org)) {
                    RegisteredDepartment rdept = new RegisteredDepartment();
                    rdept.setName(groupRepository.getGroupDescription(org));
                    if (groupRepository.isGroupManager(user, org)) {
                        rdept.setAdmin(true);
                    } else {
                        rdept.setAdmin(false);
                    }
                    depts.add(rdept);
                }
            }
            currentUser.setDepartments(depts);
            accountLogger.info("Current user: " + currentUser);

            return currentUser;
        } catch (Exception e) {
            throw new ApplicationException("Error getting current user.");

        }
    }

    public LoginResponse refreshResponse(LoginRefreshToken loginRequest) throws ApplicationException {
        try {
            CompletableFuture<LoginResponse> response = keycloakRepository.refreshLogin(loginRequest);
            LoginResponse finalResp = response.get();
            return finalResp;
        } catch (Exception e) {
            throw new ApplicationException("Error sending refresh token.");

        }
    }

    public LoginResponse refreshResponseOrganization(LoginRefreshTokenOrganization loginRequestOrganization)
            throws ApplicationException {
        try {
            CompletableFuture<LoginResponse> response = keycloakRepository
                    .refreshLoginOrganization(loginRequestOrganization);
            LoginResponse finalResp = response.get();
            return finalResp;
        } catch (Exception e) {
            throw new ApplicationException("Error sending refresh token.");

        }
    }

    /* START Registration Flow */
    public String createUserWithPassword(@Valid User user) throws ApplicationException, IOException {
        try {
            String password = userAdminRepository.createUserWithPassword(user);
            accountLogger.info("User " + user.getUsername() + " created");
            return password;
        } catch (ApplicationException e) {
            throw new ApplicationException(e.getMessage());
        }
    }

    public Response createRegisteredUserWithPassword(@Valid User user) throws ApplicationException, IOException {
        try {
            userAdminRepository.createUserWithPassword(user);
            accountLogger.info("User " + user.getUsername() + " created");
            return new Response(true,
                    UUID.randomUUID().toString());
        } catch (ApplicationException e) {
            System.out.println("Message: " + e.getMessage());
            return new Response(false,
                    e.getMessage());
        }
    }

    public void deleteUserFromFreeIPA(@Valid User user) throws ApplicationException, IOException {
        try {
            userAdminRepository.deleteUser(user);
            accountLogger.info("User " + user.getUsername() + " deleted from FreeIPA.");
        } catch (ApplicationException e) {
            throw new ApplicationException(e.getMessage());
        }
    }

    public Boolean doesEmailExistWithEmail(String email) throws ApplicationException {
        try {
            Boolean doesEmailExists = userRepository.doesEmailExist(email);
            System.out.println("Email exists: " + doesEmailExists);
            return doesEmailExists;
        } catch (Exception e) {
            throw new ApplicationException("Error while checking if the email exists.");
        }
    }

    public Boolean isMemberCurrentOrganizationWithEmail(Organization organization, String email)
            throws ApplicationException {
        try {
            return groupRepository.isGroupMemberByEmail(email, organization);
        } catch (Exception e) {
            throw new ApplicationException(
                    "Error happened while checking if the user is a member of the organization.");
        }
    }

    public String createUserFromInvite(InvitedUser user) throws ApplicationException {
        try {
            String password = userAdminRepository.createUserFromInviteWithPassword(user);
            accountLogger.info("User " + user.getUsername() + " created");
            return password;
        } catch (Exception e) {
            throw new ApplicationException("Error happened while creating user.");

        }
    }

    public void assignInvitedUserToDefaultGroup(InvitedUser user) throws ApplicationException {
        try {
            User usr = new User();
            usr.setUsername(user.getUsername());
            Organization organization = new Organization();
            organization.setName("fm_users");
            groupAdminRepository.addUserToGroup(usr, organization);
            accountLogger.info("User " + user.getUsername() + " assigned to default group");
        } catch (Exception e) {
            throw new ApplicationException("Error creating user.");

        }
    }

    public void sendEmailWithCredentials(InvitedUser user, String password) throws ApplicationException {
        try {
            mailService.sendPasswordMail(user, password);
            accountLogger.info("User " + user.getUsername() + " password email sent");
        } catch (Exception e) {
            throw new ApplicationException("Error creating user.");

        }
    }

    public Result userUpdateUser(String currentUsername, User user) throws ApplicationException {
        try {
            userRepository.updateUser(currentUsername, user);
            accountLogger.info("User " + user.getUsername() + " updated");
            return new Result("User updated successfully.", true);
        } catch (Exception e) {
            accountLogger.error("Error happened when updating user " + e.getMessage());
            return new Result("Error updating user.", false);

        }
    }

    public Boolean doesFirstNameExist(String firstName) throws ApplicationException {
        try {
            Boolean doesItExist = userRepository.doesFirstNameExist(firstName);
            return doesItExist;
        } catch (Exception e) {
            throw new ApplicationException("Error while checking if the first name exists.");
        }
    }

    public Response doesEmailExist(User user) {
        try {
            Boolean doesItExist = userAdminRepository.doesEmailExist(user.getEmail());
            System.out.println("Email: " + user.getEmail() + " exists: " + doesItExist);
            if (doesItExist) {
                return new Response(false, "Email already exists.");
            } else {
                return new Response(true, "Email does not exists");
            }
        } catch (Exception e) {
            System.out.println("Error while checking if the email exists.");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            return null;
        }
    }

    public String getUsernameFromJWT() {
        try {
            String username = jwt.getClaim("preferred_username");
            accountLogger.info("Username from JWT: " + username);
            return username;
        } catch (Exception e) {
            accountLogger.error("Error getting username from JWT: " + e.getMessage());
            return null;
        }
    }

}
