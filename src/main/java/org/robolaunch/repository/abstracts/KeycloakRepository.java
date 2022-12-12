package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.robolaunch.models.LoginRefreshToken;
import org.robolaunch.models.LoginRefreshTokenOrganization;
import org.robolaunch.models.LoginRequest;
import org.robolaunch.models.LoginRequestOrganization;
import org.robolaunch.models.LoginResponse;
import org.robolaunch.models.LoginResponseWithIPA;

public interface KeycloakRepository {
    LoginResponseWithIPA login(LoginRequest loginRequestOrganization)
            throws InternalError, IOException;

    LoginResponseWithIPA loginOrganization(LoginRequestOrganization loginRequestOrganization)
            throws InternalError, IOException;

    CompletableFuture<LoginResponse> refreshLogin(LoginRefreshToken loginRefreshToken) throws InternalError;

    CompletableFuture<LoginResponse> refreshLoginOrganization(
            LoginRefreshTokenOrganization loginRefreshTokenOrganization) throws InternalError;
}
