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
import org.robolaunch.model.account.UserGroupMember;
import org.robolaunch.model.account.LoginRefreshToken;
import org.robolaunch.model.account.LoginRefreshTokenOrganization;
import org.robolaunch.model.account.LoginRequest;
import org.robolaunch.model.account.LoginRequestOrganization;
import org.robolaunch.model.account.LoginResponse;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.Team;
import org.robolaunch.model.account.User;
import org.robolaunch.model.response.PlainResponse;
import org.robolaunch.model.response.ResponseLogin;
import org.robolaunch.model.response.ResponseRefreshToken;
import org.robolaunch.repository.abstracts.AccountRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakRepository;

import io.quarkus.arc.log.LoggerName;

import org.jboss.logging.Logger;

@ApplicationScoped
public class AccountService {
    @Inject
    KeycloakRepository keycloakRepository;

    @Inject
    KeycloakAdminRepository keycloakAdminRepository;

    @Inject
    GroupService groupService;

    @Inject
    AccountRepository accountRepository;

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
        try {
            if (accountRepository.getCurrentCookies().isEmpty()) {
                System.out.println("No cookie found. Generating new cookie.");
                IPAAdmin adminLogin = new IPAAdminLogin();
                List<HttpCookie> innerCookies;
                try {
                    innerCookies = adminLogin.login();
                    innerCookies.forEach(innerCookie -> {
                        accountRepository.appendCookie(innerCookie);
                        accountRepository.appendCookie(innerCookie);
                    });
                } catch (InternalError | IOException e) {
                    accountLogger.error("Error happened when granting admin privileges. While logging in: "
                            + e.getMessage());
                }
                accountLogger.info("New cookie generated for admin privileges.");
            } else {
                System.out.println("Cookies: " + accountRepository.getCurrentCookies() + " - ");
                accountRepository.getCurrentCookies().forEach(cookie -> {
                    if (cookie.getMaxAge() <= 0) {
                        System.out.println("Cookie expired. Generating new cookie.");
                        accountRepository.clearCookies();
                        IPAAdmin adminLogin = new IPAAdminLogin();
                        List<HttpCookie> innerCookies;
                        try {
                            innerCookies = adminLogin.login();
                            innerCookies.forEach(innerCookie -> {
                                accountRepository.appendCookie(innerCookie);
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
        } catch (Exception e) {
            accountLogger.error("Error happened when granting admin privileges. " + e.getMessage());
        }

    }

    public ResponseLogin userLogin(LoginRequest loginRequest) throws InterruptedException, ExecutionException {
        ResponseLogin responseLogin = new ResponseLogin();
        try {
            LoginResponse response = keycloakRepository.login(loginRequest);
            responseLogin.setMessage("Login successful.");
            responseLogin.setSuccess(true);
            responseLogin.setData(response);
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
            LoginResponse response = keycloakRepository.loginOrganization(loginRequestOrganization);
            responseLogin.setMessage("Login successful.");
            responseLogin.setSuccess(true);
            responseLogin.setData(response);
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

    public LoginResponse login(LoginRequest loginRequest) throws ApplicationException {
        try {
            LoginResponse response = keycloakRepository.login(loginRequest);
            accountLogger.info("Login successful");
            return response;
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
            accountRepository.changePassword(oldPassword, newPassword, user.getUsername());
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
            accountRepository.clearCookies();
        } catch (Exception e) {
            accountLogger.error("Error when logout: " + e);
        }
    }

    public UserGroupMember currentUser(Organization organization) {
        try {
            User user = new User();
            user.setUsername(jwt.getClaim("preferred_username"));
            UserGroupMember returnedUser = new UserGroupMember();
            returnedUser.setUsername(jwt.getClaim("preferred_username"));
            returnedUser.setEmail(jwt.getClaim("email"));
            returnedUser.setFirstName(jwt.getClaim("given_name"));
            returnedUser.setLastName(jwt.getClaim("family_name"));
            returnedUser.setAdmin(accountRepository.isGroupManager(user, organization));
            accountLogger.info("Current user: " + returnedUser);
            return returnedUser;
        } catch (Exception e) {
            accountLogger.error("Error happened when getting current user " + e.getMessage());
        }
        return null;
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
            String password = accountRepository.createUserWithPassword(user);
            accountLogger.info("User " + user.getUsername() + " created");
            return password;
        } catch (ApplicationException e) {
            throw new ApplicationException(e.getMessage());
        }
    }

    public PlainResponse createRegisteredUserWithPassword(@Valid User user) throws ApplicationException, IOException {
        PlainResponse plainResponse = new PlainResponse();
        try {
            accountRepository.createUserWithPassword(user);
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
            accountRepository.deleteUser(user);
            accountLogger.info("User " + user.getUsername() + " deleted from FreeIPA.");
        } catch (ApplicationException e) {
            throw new ApplicationException(e.getMessage());
        }
    }

    public Boolean doesEmailExistWithEmail(String email) throws ApplicationException {
        try {
            Boolean doesEmailExists = accountRepository.doesEmailExist(email);
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
            return accountRepository.isGroupMemberByEmail(email, organization);
        } catch (Exception e) {
            throw new ApplicationException(
                    "Error happened while checking if the user is a member of the organization.");
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
            accountRepository.updateUser(currentUsername, user);
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
            Boolean doesItExist = accountRepository.doesFirstNameExist(firstName);
            return doesItExist;
        } catch (Exception e) {
            throw new ApplicationException("Error while checking if the first name exists.");
        }
    }

    public Boolean doesEmailExist(User user) {
        try {
            Boolean doesItExist = accountRepository.doesEmailExist(user.getEmail());
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

    public User getUserFromJWT() {
        try {
            String username = jwt.getClaim("preferred_username");
            String email = jwt.getClaim("email");
            String firstName = jwt.getClaim("given_name");
            String lastName = jwt.getClaim("family_name");
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            accountLogger.info("Username from JWT: " + username);
            return user;
        } catch (Exception e) {
            accountLogger.error("Error getting username from JWT: " + e.getMessage());
            return null;
        }
    }

}
