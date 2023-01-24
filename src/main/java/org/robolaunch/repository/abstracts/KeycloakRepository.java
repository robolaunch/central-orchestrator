package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.robolaunch.model.account.LoginRefreshToken;
import org.robolaunch.model.account.LoginRefreshTokenOrganization;
import org.robolaunch.model.account.LoginRequest;
import org.robolaunch.model.account.LoginRequestOrganization;
import org.robolaunch.model.account.LoginResponse;

public interface KeycloakRepository {
        LoginResponse login(LoginRequest loginRequestOrganization)
                        throws InternalError, IOException;

        LoginResponse loginOrganization(LoginRequestOrganization loginRequestOrganization)
                        throws InternalError, IOException;

        CompletableFuture<LoginResponse> refreshLogin(LoginRefreshToken loginRefreshToken) throws InternalError;

        CompletableFuture<LoginResponse> refreshLoginOrganization(
                        LoginRefreshTokenOrganization loginRefreshTokenOrganization) throws InternalError;
}
