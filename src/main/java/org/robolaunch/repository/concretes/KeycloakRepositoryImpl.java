package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.core.abstracts.IPAAdmin;
import org.robolaunch.core.abstracts.IPALogin;
import org.robolaunch.core.concretes.IPAAdminLogin;
import org.robolaunch.core.concretes.IPAUserLogin;
import org.robolaunch.models.LoginRefreshToken;
import org.robolaunch.models.LoginRefreshTokenOrganization;
import org.robolaunch.models.LoginRequest;
import org.robolaunch.models.LoginRequestOrganization;
import org.robolaunch.models.LoginResponse;
import org.robolaunch.models.LoginResponseWithIPA;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.service.KeycloakService;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@ApplicationScoped
public class KeycloakRepositoryImpl implements KeycloakRepository {
    @Inject
    private Vertx vertx;
    private WebClient client;

    @Inject
    UserAdminRepository userAdminRepository;

    @Inject
    GroupAdminRepository groupAdminRepository;

    @Inject
    KeycloakService keycloakService;

    @ConfigProperty(name = "quarkus.oidc.client.id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.client.credentials")
    String clientSecret;

    /**
     * Read KC URI from config file.
     */
    @PostConstruct
    void initialize() {
        this.client = WebClient.create(vertx,
                new WebClientOptions().setDefaultHost("keycloak.robolaunch.dev").setDefaultPort(443).setSsl(true));

    }

    @Override
    public LoginResponseWithIPA loginOrganization(LoginRequestOrganization loginRequestOrganization)
            throws InternalError, IOException {
        CompletableFuture<LoginResponse> response = new CompletableFuture<>();

        if (loginRequestOrganization.getUsername().contains("@")) {
            System.out.println("Login with email");
            String username = userAdminRepository.getUserByEmail(loginRequestOrganization.getUsername()).getUsername();
            System.out.println("Username: " + username);
            loginRequestOrganization.setUsername(username);
        }

        MultiMap userFormData = convertUserOrganization(loginRequestOrganization);
        this.client
                .post("/auth/realms/" + loginRequestOrganization.getOrganization() + "/protocol/openid-connect/token")
                .sendForm(userFormData).onFailure(
                        throwable -> {
                            System.out.println("Error: " + throwable.getMessage());
                            System.out.println("Error: " + throwable.getCause());
                            response.completeExceptionally(throwable);
                        })
                .onSuccess(
                        httpResponse -> {
                            if (httpResponse.statusCode() == 200) {
                                LoginResponse logResponse = new LoginResponse();
                                logResponse.setAccessToken(httpResponse.bodyAsJsonObject().getString("access_token"));
                                logResponse.setExpiresIn(httpResponse.bodyAsJsonObject().getString("expires_in"));
                                logResponse.setRefreshExpiresIn(
                                        httpResponse.bodyAsJsonObject().getString("refresh_expires_in"));
                                logResponse.setIdToken(httpResponse.bodyAsJsonObject().getString("id_token"));
                                logResponse.setRefreshToken(httpResponse.bodyAsJsonObject().getString("refresh_token"));
                                response.complete(logResponse);
                            } else {
                                response.completeExceptionally(new InternalError("Login failed"));
                            }
                        })

                .toCompletionStage().toCompletableFuture()
                .orTimeout(10000, TimeUnit.MILLISECONDS)
                .thenAccept(resp -> {
                    if (resp.statusCode() != 200) {
                        response.completeExceptionally(new InternalError());
                    }
                    response.complete(resp.bodyAsJson(LoginResponse.class));
                }).exceptionally(throwable -> {
                    response.completeExceptionally(throwable);
                    return null;
                });

        IPALogin ipaLogin = new IPAUserLogin();
        ipaLogin.setUsername(loginRequestOrganization.getUsername());
        ipaLogin.setPassword(loginRequestOrganization.getPassword());
        List<HttpCookie> newCookies = ipaLogin.login();
        LoginResponseWithIPA loginResponseWithIPA = new LoginResponseWithIPA();
        loginResponseWithIPA.setLoginResponse(response);
        loginResponseWithIPA.setCookies(newCookies);
        return loginResponseWithIPA;
    }

    private MultiMap convertUserOrganization(LoginRequestOrganization loginRequest) {
        String clientSecret = keycloakService.getClientSecret(loginRequest.getOrganization(), "gatekeeper");
        MultiMap userInfo = MultiMap.caseInsensitiveMultiMap();
        userInfo.add("username", loginRequest.getUsername());
        userInfo.add("password", loginRequest.getPassword());
        userInfo.add("client_id", clientId);
        userInfo.add("client_secret", clientSecret);
        userInfo.add("grant_type", "password");
        userInfo.add("scope", "openid");

        return userInfo;
    }

    @Override
    public LoginResponseWithIPA login(LoginRequest loginRequest)
            throws InternalError, IOException {
        CompletableFuture<LoginResponse> response = new CompletableFuture<>();

        if (loginRequest.getUsername().contains("@")) {
            System.out.println("Login with email");
            String username = userAdminRepository.getUserByEmail(loginRequest.getUsername()).getUsername();
            System.out.println("Username: " + username);
            loginRequest.setUsername(username);
        }

        MultiMap userFormData = convertUser(loginRequest);
        this.client
                .post("/auth/realms/kogito/protocol/openid-connect/token")
                .sendForm(userFormData).onFailure(
                        throwable -> {
                            System.out.println("Error: " + throwable.getMessage());
                            System.out.println("Error: " + throwable.getCause());
                            response.completeExceptionally(throwable);
                        })
                .onSuccess(
                        httpResponse -> {
                            if (httpResponse.statusCode() == 200) {
                                LoginResponse logResponse = new LoginResponse();
                                logResponse.setAccessToken(httpResponse.bodyAsJsonObject().getString("access_token"));
                                logResponse.setExpiresIn(httpResponse.bodyAsJsonObject().getString("expires_in"));
                                logResponse.setRefreshExpiresIn(
                                        httpResponse.bodyAsJsonObject().getString("refresh_expires_in"));
                                logResponse.setIdToken(httpResponse.bodyAsJsonObject().getString("id_token"));
                                logResponse.setRefreshToken(httpResponse.bodyAsJsonObject().getString("refresh_token"));
                                response.complete(logResponse);
                            } else {
                                response.completeExceptionally(new InternalError("Login failed"));
                            }
                        })

                .toCompletionStage().toCompletableFuture()
                .orTimeout(10000, TimeUnit.MILLISECONDS)
                .thenAccept(resp -> {
                    if (resp.statusCode() != 200) {
                        response.completeExceptionally(new InternalError());
                    }
                    response.complete(resp.bodyAsJson(LoginResponse.class));
                }).exceptionally(throwable -> {
                    response.completeExceptionally(throwable);
                    return null;
                });

        IPALogin ipaLogin = new IPAUserLogin();
        ipaLogin.setUsername(loginRequest.getUsername());
        ipaLogin.setPassword(loginRequest.getPassword());
        List<HttpCookie> newCookies = ipaLogin.login();
        LoginResponseWithIPA loginResponseWithIPA = new LoginResponseWithIPA();
        loginResponseWithIPA.setLoginResponse(response);
        loginResponseWithIPA.setCookies(newCookies);
        return loginResponseWithIPA;
    }

    private MultiMap convertUser(LoginRequest loginRequest) {
        MultiMap userInfo = MultiMap.caseInsensitiveMultiMap();
        userInfo.add("username", loginRequest.getUsername());
        userInfo.add("password", loginRequest.getPassword());
        userInfo.add("client_id", clientId);
        userInfo.add("client_secret", clientSecret);
        userInfo.add("grant_type", "password");
        userInfo.add("scope", "openid");

        return userInfo;
    }

    private MultiMap convertRefreshToken(LoginRefreshToken loginRefreshToken) {
        MultiMap refreshInfo = MultiMap.caseInsensitiveMultiMap();
        refreshInfo.add("client_id", clientId);
        refreshInfo.add("client_secret", clientSecret);
        refreshInfo.add("refresh_token", loginRefreshToken.getRefreshToken());
        refreshInfo.add("grant_type", "refresh_token");
        return refreshInfo;
    }

    private MultiMap convertRefreshTokenOrganization(LoginRefreshTokenOrganization loginRefreshTokenOrganization) {
        String clientSecret = keycloakService.getClientSecret(loginRefreshTokenOrganization.getOrganization(),
                "gatekeeper");
        MultiMap refreshInfo = MultiMap.caseInsensitiveMultiMap();
        refreshInfo.add("client_id", clientId);
        refreshInfo.add("client_secret", clientSecret);
        refreshInfo.add("refresh_token", loginRefreshTokenOrganization.getRefreshToken());
        refreshInfo.add("grant_type", "refresh_token");
        return refreshInfo;
    }

    @Override
    public CompletableFuture<LoginResponse> refreshLogin(LoginRefreshToken loginRefreshToken) throws InternalError {
        System.out.println("TOken: " + loginRefreshToken.getRefreshToken());
        CompletableFuture<LoginResponse> response = new CompletableFuture<>();
        MultiMap loginFormData = convertRefreshToken(loginRefreshToken);
        System.out.println("login data: " + loginFormData.get("refresh_token"));
        this.client.post("/auth/realms/kogito/protocol/openid-connect/token").sendForm(loginFormData)
                .onFailure(er -> {
                    System.out.println(er.getMessage());
                    System.out.println(er.getCause());

                })
                .toCompletionStage().toCompletableFuture()
                .orTimeout(5000, TimeUnit.MILLISECONDS)
                .thenAccept(httpResponse -> {
                    LoginResponse logResponse = new LoginResponse();
                    logResponse.setAccessToken(httpResponse.bodyAsJsonObject().getString("access_token"));
                    logResponse.setExpiresIn(httpResponse.bodyAsJsonObject().getString("expires_in"));
                    logResponse.setRefreshExpiresIn(
                            httpResponse.bodyAsJsonObject().getString("refresh_expires_in"));
                    logResponse.setIdToken(httpResponse.bodyAsJsonObject().getString("id_token"));
                    logResponse.setRefreshToken(httpResponse.bodyAsJsonObject().getString("refresh_token"));

                    response.complete(logResponse);
                }).exceptionally(throwable -> {
                    response.completeExceptionally(throwable);
                    return null;
                });
        return response;
    }

    @Override
    public CompletableFuture<LoginResponse> refreshLoginOrganization(
            LoginRefreshTokenOrganization loginRefreshTokenOrganization)
            throws InternalError {
        System.out.println("TOken: " + loginRefreshTokenOrganization.getRefreshToken());
        CompletableFuture<LoginResponse> response = new CompletableFuture<>();
        MultiMap loginFormData = convertRefreshTokenOrganization(loginRefreshTokenOrganization);
        System.out.println("login data: " + loginFormData.get("refresh_token"));
        this.client
                .post("/auth/realms/" + loginRefreshTokenOrganization.getOrganization()
                        + "/protocol/openid-connect/token")
                .sendForm(loginFormData)
                .onFailure(er -> {
                    System.out.println(er.getMessage());
                    System.out.println(er.getCause());

                })
                .toCompletionStage().toCompletableFuture()
                .orTimeout(5000, TimeUnit.MILLISECONDS)
                .thenAccept(httpResponse -> {
                    LoginResponse logResponse = new LoginResponse();
                    logResponse.setAccessToken(httpResponse.bodyAsJsonObject().getString("access_token"));
                    logResponse.setExpiresIn(httpResponse.bodyAsJsonObject().getString("expires_in"));
                    logResponse.setRefreshExpiresIn(
                            httpResponse.bodyAsJsonObject().getString("refresh_expires_in"));
                    logResponse.setIdToken(httpResponse.bodyAsJsonObject().getString("id_token"));
                    logResponse.setRefreshToken(httpResponse.bodyAsJsonObject().getString("refresh_token"));

                    response.complete(logResponse);
                }).exceptionally(throwable -> {
                    response.completeExceptionally(throwable);
                    return null;
                });
        return response;
    }
}
