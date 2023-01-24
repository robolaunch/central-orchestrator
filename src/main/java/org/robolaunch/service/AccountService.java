package org.robolaunch.service;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.robolaunch.exception.UserNotFoundException;
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
import org.robolaunch.models.User;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.models.response.ResponseCurrentUser;
import org.robolaunch.models.response.ResponseLogin;
import org.robolaunch.models.response.ResponseRefreshToken;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

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

    /*
     * Granting admin privileges, needed when user permissions not enough.
     * HELPER FUNCTION.
     */
    public void grantAdminPrivileges() {
        groupAdminRepository.getCurrentCookies().forEach(cookie -> {
            if (cookie.getMaxAge() <= 0) {
                try {
                    groupAdminRepository.clearCookies();
                } catch (URISyntaxException e) {
                    accountLogger.error("Error happened when granting admin privileges. While clearing cookies: "
                            + e.getMessage());
                }
                IPAAdmin adminLogin = new IPAAdminLogin();
                List<HttpCookie> innerCookies;
                try {
                    innerCookies = adminLogin.login();
                    innerCookies.forEach(innerCookie -> {
                        groupAdminRepository.appendCookie(innerCookie);
                        userAdminRepository.appendCookie(innerCookie);
                    });
                } catch (InternalError | IOException e) {
                    accountLogger.error("Error happened when granting admin privileges. While logging in: "
                            + e.getMessage());
                }
                accountLogger.info("New cookie generated for admin privileges.");
            } else {
                accountLogger.info("Cookie still works. No need to refresh.");
            }
        });

    }

    public ResponseLogin userLogin(LoginRequest loginRequest) throws InterruptedException, ExecutionException {
        ResponseLogin responseLogin = new ResponseLogin();
        try {
            LoginResponseWithIPA response = keycloakRepository.login(loginRequest);
            List<HttpCookie> cookies = response.getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
            responseLogin.setMessage("Login successful.");
            responseLogin.setSuccess(true);
            responseLogin.setData(response.getLoginResponse().get());
            return responseLogin;
        } catch (UserNotFoundException e) {
            responseLogin.setMessage(e.getMessage());
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
        } catch (ApplicationException e) {
            responseLogin.setMessage(e.getMessage());
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
        } catch (Exception e) {
            responseLogin.setMessage("An unexpected error occurred. Please try again later.");
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
        }
    }

    public ResponseLogin userLoginOrganization(LoginRequestOrganization loginRequestOrganization)
            throws InterruptedException, ExecutionException {
        ResponseLogin responseLogin = new ResponseLogin();
        try {
            LoginResponseWithIPA response = keycloakRepository.loginOrganization(loginRequestOrganization);
            List<HttpCookie> cookies = response.getCookies();
            cookies.forEach(cookie -> {
                groupRepository.appendCookie(cookie);
                userRepository.appendCookie(cookie);
            });
            responseLogin.setMessage("Login successful.");
            responseLogin.setSuccess(true);
            responseLogin.setData(response.getLoginResponse().get());
            return responseLogin;
        } catch (UserNotFoundException e) {
            responseLogin.setMessage(e.getMessage());
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
        } catch (ApplicationException e) {
            responseLogin.setMessage(e.getMessage());
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
        } catch (Exception e) {
            responseLogin.setMessage("An unexpected error occurred. Please try again later.");
            responseLogin.setSuccess(false);
            responseLogin.setData(null);
            return responseLogin;
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
            LoginResponse finalResp = response.get();
            accountLogger.info("Login successful");
            return finalResp;
        } catch (Exception | InternalError e) {
            accountLogger.error("Error when login: " + e.getMessage() + e.getStackTrace() + e.getLocalizedMessage());
            throw new ApplicationException("Invalid username or password.");
        }
    }

    public PlainResponse userChangePassword(String oldPassword, String newPassword)
            throws ApplicationException, IOException {
        PlainResponse plainResponse = new PlainResponse();
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            userRepository.changePassword(oldPassword, newPassword, user.getUsername());
            accountLogger.info("User password updated.");
            plainResponse.setSuccess(true);
            plainResponse.setMessage("Password updated successfully.");
        } catch (ApplicationException e) {
            accountLogger.error("Error changing password: ");
            plainResponse.setSuccess(false);
            plainResponse.setMessage("Error changing password.");
        }
        return plainResponse;
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

    public GroupMember currentUser(Organization organization) {
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            GroupMember returnedUser = new GroupMember();
            returnedUser.setUsername(jwt.getClaim("preferred_username"));
            returnedUser.setEmail(jwt.getClaim("email"));
            returnedUser.setFirstName(jwt.getClaim("given_name"));
            returnedUser.setLastName(jwt.getClaim("family_name"));
            returnedUser.setAdmin(groupAdminRepository.isGroupManager(user, organization));
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

    public ResponseCurrentUser getCurrentUser(Organization organization) {
        ResponseCurrentUser responseCurrentUser = new ResponseCurrentUser();
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            CurrentUser currentUser = new CurrentUser();
            currentUser.setUsername(jwt.getClaim("preferred_username"));
            currentUser.setEmail(jwt.getClaim("email"));
            currentUser.setFirstName(jwt.getClaim("given_name"));
            currentUser.setLastName(jwt.getClaim("family_name"));

            currentUser.setAdmin(groupAdminRepository.isGroupManager(user, organization));
            List<Department> departments = groupAdminRepository.getTeams(organization, "member_group");
            Iterator<Department> it = departments.iterator();
            ArrayList<RegisteredDepartment> depts = new ArrayList<RegisteredDepartment>();
            while (it.hasNext()) {
                Department d = it.next();
                Organization org = new Organization();
                org.setName(d.getId());
                if (groupAdminRepository.isGroupMember(user, org)) {
                    RegisteredDepartment rdept = new RegisteredDepartment();
                    rdept.setName(groupAdminRepository.getGroupDescription(org));
                    if (groupAdminRepository.isGroupManager(user, org)) {
                        rdept.setAdmin(true);
                    } else {
                        rdept.setAdmin(false);
                    }
                    depts.add(rdept);
                }
            }
            currentUser.setDepartments(depts);

            responseCurrentUser.setSuccess(true);
            responseCurrentUser.setUser(currentUser);
            responseCurrentUser.setMessage("User data retrieved successfully.");
        } catch (Exception e) {
            responseCurrentUser.setMessage("Error getting current user.");
            responseCurrentUser.setSuccess(false);
        }
        return responseCurrentUser;

    }

    public ResponseRefreshToken refreshResponse(LoginRefreshToken loginRequest) {
        ResponseRefreshToken responseRefreshToken = new ResponseRefreshToken();
        try {
            CompletableFuture<LoginResponse> response = keycloakRepository.refreshLogin(loginRequest);
            LoginResponse finalResp = response.get();
            responseRefreshToken.setSuccess(true);
            responseRefreshToken.setMessage("Refresh token sent successfully.");
            responseRefreshToken.setData(finalResp);
        } catch (Exception e) {
            responseRefreshToken.setSuccess(false);
            responseRefreshToken.setMessage("Error sending refresh token.");
        }
        return responseRefreshToken;

    }

    public ResponseRefreshToken refreshResponseOrganization(LoginRefreshTokenOrganization loginRequestOrganization)
            throws ApplicationException {
        ResponseRefreshToken responseRefreshToken = new ResponseRefreshToken();
        try {
            CompletableFuture<LoginResponse> response = keycloakRepository
                    .refreshLoginOrganization(loginRequestOrganization);
            LoginResponse finalResp = response.get();
            responseRefreshToken.setSuccess(true);
            responseRefreshToken.setMessage("Refresh token sent successfully.");
            responseRefreshToken.setData(finalResp);
        } catch (Exception e) {
            responseRefreshToken.setSuccess(false);
            responseRefreshToken.setMessage("Error sending refresh token.");
        }
        return responseRefreshToken;
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

    public PlainResponse createRegisteredUserWithPassword(@Valid User user) throws ApplicationException, IOException {
        PlainResponse plainResponse = new PlainResponse();
        try {
            userAdminRepository.createUserWithPassword(user);
            plainResponse.setSuccess(true);
            plainResponse.setMessage("User created successfully.");
            accountLogger.info("User " + user.getUsername() + " created");
        } catch (ApplicationException e) {
            plainResponse.setSuccess(false);
            plainResponse.setMessage("Error registering user with password.");
            accountLogger.error("Error registering user with password.");
        }
        return plainResponse;
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
            Boolean doesEmailExists = userAdminRepository.doesEmailExist(email);
            accountLogger.info("Email " + email + " exists: " + doesEmailExists);
            return doesEmailExists;
        } catch (Exception e) {
            accountLogger.error("Error checking if email exists.");
            return null;
        }
    }

    public Boolean isMemberCurrentOrganizationWithEmail(Organization organization, String email)
            throws ApplicationException {
        try {
            return groupAdminRepository.isGroupMemberByEmail(email, organization);
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

    public PlainResponse userUpdateUser(String currentUsername, User user) throws ApplicationException {
        PlainResponse plainResponse = new PlainResponse();
        try {
            if (!jwt.getClaim("preferred_username").equals(currentUsername)) {
                plainResponse.setSuccess(false);
                plainResponse.setMessage("You are not authorized to update this user.");
                return plainResponse;
            }
            userRepository.updateUser(currentUsername, user);
            accountLogger.info("User " + user.getUsername() + " updated");
            plainResponse.setSuccess(true);
            plainResponse.setMessage("User is successfully updated.");
        } catch (Exception e) {
            plainResponse.setSuccess(false);
            plainResponse.setMessage("Error happened when updating user.");
        }
        return plainResponse;
    }

    public Boolean doesFirstNameExist(String firstName) throws ApplicationException {
        try {
            Boolean doesItExist = userRepository.doesFirstNameExist(firstName);
            return doesItExist;
        } catch (Exception e) {
            throw new ApplicationException("Error while checking if the first name exists.");
        }
    }

    public Boolean doesEmailExist(User user) {
        try {
            Boolean doesItExist = userAdminRepository.doesEmailExist(user.getEmail());
            return doesItExist;
        } catch (Exception e) {
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
