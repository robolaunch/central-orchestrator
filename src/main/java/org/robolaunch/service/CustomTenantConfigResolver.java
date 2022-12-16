package org.robolaunch.service;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

   @Inject
   KeycloakAdminRepository keycloakAdminRepository;

   @ConfigProperty(name = "quarkus.oidc.client.credentials")
   String mainRealmSecret;

   @ConfigProperty(name = "quarkus.oidc.auth-server-url")
   String mainRealmUrl;

   @ConfigProperty(name = "quarkus.oidc.client.id")
   String clientId;

   @ConfigProperty(name = "keycloak.default.realm")
   String defaultRealm;

   @ConfigProperty(name = "keycloak.url")
   String keycloakUrl;

   @Override
   public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
         OidcRequestContext<OidcTenantConfig> requestContext) {
      if (routingContext.request().headers().get("Authorization") == null) {
         return Uni.createFrom().item(createTenantConfig());
      }
      String token = routingContext.request().headers().get("Authorization").split("\\s+")[1];
      JsonObject jwtContent = OidcUtils.decodeJwtContent(token);
      String realm = jwtContent.getMap().get("iss").toString().split("/")[5];

      String path = routingContext.request().path();
      String[] parts = path.split("/");

      if (parts.length == 0) {
         return Uni.createFrom().item(createTenantConfig());
      }

      if (parts[1].equals("userLoginOrganization")) {
         return Uni.createFrom().item(createTenantConfig());
      }

      return Uni.createFrom().item(createTenantConfig(realm));
   }

   private Supplier<OidcTenantConfig> createTenantConfig(String organization) {
      final OidcTenantConfig config = new OidcTenantConfig();
      String clientSecret = keycloakAdminRepository.getClientSecret(organization,
            clientId);
      config.setTenantId(organization);
      config.setAuthServerUrl(keycloakUrl + "/realms/" + organization);
      config.setClientId(clientId);
      OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
      credentials.setSecret(clientSecret);
      config.setCredentials(credentials);
      return () -> config;
   }

   private Supplier<OidcTenantConfig> createTenantConfig() {
      final OidcTenantConfig config = new OidcTenantConfig();
      config.setTenantId(defaultRealm);
      config.setAuthServerUrl(mainRealmUrl);
      config.setClientId(clientId);
      OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
      credentials.setSecret(mainRealmSecret);
      config.setCredentials(credentials);
      return () -> config;
   }
}
