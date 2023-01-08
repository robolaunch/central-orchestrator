package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.Token;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;

@ApplicationScoped
public class KeycloakAdminRepositoryImpl implements KeycloakAdminRepository {
    private Keycloak client;

    @ConfigProperty(name = "keycloak.default.realm")
    String defaultRealm;

    @Inject
    GroupRepository groupRepository;

    @PostConstruct
    public void adminLogin() {
        var url = ConfigProvider.getConfig().getValue("keycloak.url", String.class);
        var realm = ConfigProvider.getConfig().getValue("keycloak.realm", String.class);
        var clientId = ConfigProvider.getConfig().getValue("keycloak.clientId", String.class);
        var username = ConfigProvider.getConfig().getValue("keycloak.admin.username", String.class);
        var password = ConfigProvider.getConfig().getValue("keycloak.admin.password", String.class);
        this.client = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();

    }

    @Override
    public void createRealm(Organization organization) throws ApplicationException {
        // Check if realm already exists
        Boolean realmExists = client.realms().findAll().stream()
                .anyMatch(r -> r.getRealm().equals(organization.getName()));
        if (realmExists) {
            throw new ApplicationException("Organization already exists");
        }
        RealmRepresentation realm = new RealmRepresentation();
        realm.setId(organization.getName());
        realm.setRealm(organization.getName());
        realm.setEnabled(true);
        // realm.setLoginTheme("robolaunch");
        client.realms().create(realm);
    }

    @Override
    public void updateFederation(Organization organization) {
        // ProviderRepresentation userStorage= new ProviderRepresentation();
        var ipaHost = ConfigProvider.getConfig().getValue("freeipa.host", String.class);
        var ipaPass = ConfigProvider.getConfig().getValue("freeipa.password", String.class);

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add("allowKerberosAuthentication", "false");
        config.add("authType", "simple");
        config.add("batchSizeForSync", "1000");
        config.add("bindCredential", ipaPass);
        config.add("bindDn", "uid=admin,cn=users,cn=accounts,dc=robolaunch,dc=dev");
        config.add("cachePolicy", "DEFAULT");
        config.add("rdnLDAPAttribute", "uid");
        config.add("usernameLDAPAttribute", "uid");
        config.add("usersDn", "cn=users,cn=accounts,dc=robolaunch,dc=dev");
        config.add("uuidLDAPAttribute", "nsuniqueid");
        config.add("vendor", "rhds");
        config.add("userObjectClasses", "inetOrgPerson, organizationalPerson");
        config.add("connectionUrl", "ldap://" + ipaHost);
        config.add("enabled", "true");
        config.add("fullSyncPeriod", "-1");
        config.add("changedSyncPeriod", "-1");
        config.add("batchSizeForSync", "1000");
        config.add("changedSyncPeriod", "0");

        config.add("editMode", "WRITABLE");
        config.add("priority", "0");
        config.add("importEnabled", "true");
        config.add("customUserSearchFilter", "(&(objectClass=inetOrgPerson)(memberof=cn=" + organization.getName()
                + ",cn=groups,cn=accounts,dc=robolaunch,dc=dev))");
        config.add("allowKerberosAuthentication", "false");
        config.add("debug", "false");
        config.add("syncRegistrations", "false");

        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setConfig(config);
        componentRepresentation.setName("ldap");
        componentRepresentation.setProviderType("org.keycloak.storage.UserStorageProvider");
        componentRepresentation.setParentId(organization.getName());
        componentRepresentation.setProviderId("ldap");

        var realm = client.realm(organization.getName());
        realm.components().add(componentRepresentation);

        var componentId = realm.components()
                .query(organization.getName(), "org.keycloak.storage.UserStorageProvider", "ldap").get(0)
                .getId();

        this.client.realm(organization.getName()).userStorage().syncUsers(componentId, "triggerFullSync");
    }

    @Override
    public void deleteRealm(Organization organization) throws InternalError, IOException {
        this.client.realm(organization.getName()).remove();
    }

    @Override
    public void addGroupMapper(Organization organization) {
        var parentId = this.client.realm(organization.getName()).components()
                .query(organization.getName(), "org.keycloak.storage.UserStorageProvider", "ldap").get(0).getId();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add("drop.non.existing.groups.during.sync", "false");
        config.add("group.name.ldap.attribute", "cn");
        config.add("group.object.classes", "groupOfNames");
        config.add("groups.dn", "cn=groups,cn=accounts,dc=robolaunch,dc=dev");
        config.add("groups.ldap.filter",
                "(memberof=cn=" + organization.getName() + ",cn=groups,cn=accounts,dc=robolaunch,dc=dev)");
        config.add("user.roles.retrieve.strategy", "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE");

        config.add("groups.path", "/");
        config.add("ignore.missing.groups", "false");
        config.add("memberof.ldap.attribute", "memberOf");
        config.add("membership.attribute.type", "DN");
        config.add("membership.ldap.attribute", "member");
        config.add("membership.user.ldap.attribute", "uid");
        config.add("user.model.attribute", "memberof");
        config.add("mode", "LDAP_ONLY");
        config.add("preserve.group.inheritance", "true");
        config.add("user.roles.retrieve.strategy", "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE");
        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setConfig(config);
        componentRepresentation.setName("groups");
        componentRepresentation.setParentId(parentId);

        componentRepresentation.setProviderId("group-ldap-mapper");

        componentRepresentation.setProviderType("org.keycloak.storage.ldap.mappers.LDAPStorageMapper");

        var realm = client.realm(organization.getName());
        realm.components().add(componentRepresentation);

    }

    @Override
    public void setManagementRoles(User user, Organization organization) {
        UserRepresentation userRepresentation = this.client.realm(organization.getName()).users()
                .search(user.getUsername()).get(0);

        var id = this.client.realm(organization.getName()).clients().findByClientId("realm-management").get(0).getId();

        List<RoleRepresentation> roleList = this.client.realm(organization.getName()).clients().get(id).roles().list();
        this.client.realm(organization.getName()).users().get(userRepresentation.getId()).roles().clientLevel(id)
                .add(roleList);
    }

    @Override
    public void forgotPassword(String username) {
        String userId = this.client.realm(defaultRealm).users().search(username).get(0).getId();
        this.client.realm(this.defaultRealm).users().get(userId).executeActionsEmail(Arrays.asList("UPDATE_PASSWORD"));
    }

    @Override
    public Boolean isPasswordUpdated(User user) {
        String userId = this.client.realm(defaultRealm).users().search(user.getUsername()).get(0).getId();
        var events = this.client.realm(this.defaultRealm).getEvents();
        if (events.stream()
                .filter(event -> event.getUserId().equals(userId) && event.getType().equals("UPDATE_PASSWORD"))
                .count() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void syncFederationMainRealm() {
        var components = this.client.realm(this.defaultRealm).components().query();
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).getName() == null) {
                continue;
            }
            if (components.get(i).getName().equals("ldap")) {
                this.client.realm(this.defaultRealm).userStorage().syncUsers(components.get(i).getId(),
                        "triggerChangedUsersSync");
            }
        }

        // this.client.realm(defaultRealm).userStorage().syncUsers(componentId,
        // "triggerChangedUsersSync");
    }

    @Override
    public void syncFederationInCurrentRealm(Organization organization) {
        var components = this.client.realm(organization.getName()).components().query();
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).getName() == null) {
                continue;
            }
            if (components.get(i).getName().equals("ldap")) {
                this.client.realm(this.defaultRealm).userStorage().syncUsers(components.get(i).getId(),
                        "triggerChangedUsersSync");
            }
        }
    }

    @Override
    public void syncIPAGroupsInCurrentRealm(Organization organization) {
        var componentId = this.client.realm(organization.getName()).components()
                .query(organization.getName(), "org.keycloak.storage.UserStorageProvider", "ldap").get(0).getId();

        var mapperId = this.client.realm(organization.getName()).components().query(componentId,
                "org.keycloak.storage.ldap.mappers.LDAPStorageMapper", "groups").get(0).getId();

        this.client.realm(organization.getName()).userStorage().syncMapperData(componentId, mapperId, "fedToKeycloak");

    }

    @Override
    public void createClient(Organization organization) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        List<ProtocolMapperRepresentation> protocols = new ArrayList<>();

        Map<String, String> config = new HashMap<>();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("backchannel.logout.session.required", "true");
        attributes.put("backchannel.logout.revoke.offline.tokens", "false");

        config.put("access.token.claim", "true");
        config.put("claim.name", "groups");
        config.put("id.token.claim", "true");
        config.put("jsonType.label", "String");
        config.put("multivalued", "true");
        config.put("user.attribute", "memberof");
        config.put("userinfo.token.claim", "true");

        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();

        protocolMapper.setName("groups");
        protocolMapper.setProtocol("openid-connect");
        protocolMapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
        protocolMapper.setConfig(config);

        protocols.add(protocolMapper);
        clientRepresentation.setAttributes(attributes);
        clientRepresentation.setFullScopeAllowed(true);
        clientRepresentation.setDirectAccessGrantsEnabled(true);
        clientRepresentation.setClientId("acc");
        clientRepresentation.setFrontchannelLogout(false);
        clientRepresentation.setProtocolMappers(protocols);
        this.client.realm(organization.getName()).clients().create(clientRepresentation);
    }

    @Override
    public String getClientSecret(String organizationName, String clientId) {
        var client = this.client.realm(organizationName).clients().findByClientId(clientId).get(0);
        var secret = this.client.realm(organizationName).clients().get(client.getId()).getSecret();
        return secret.getValue();
    }

    /* Gatekeeper client is used for policy. */
    @Override
    public void createGatekeeperClient(Organization organization) throws InternalError, IOException {

        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId("gatekeeper");
        clientRepresentation.setProtocol("openid-connect");
        clientRepresentation.setPublicClient(false);
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("http://*");
        redirectUris.add("https://*");
        clientRepresentation.setRedirectUris(redirectUris);
        List<String> clientScopes = new ArrayList<>();
        List<String> optionalClientScopes = new ArrayList<>();
        clientScopes.add("profile");
        clientScopes.add("email");
        clientScopes.add("groups");
        clientScopes.add("roles");
        clientScopes.add("web-origins");

        optionalClientScopes.add("offline_access");
        optionalClientScopes.add("address");
        optionalClientScopes.add("phone");
        optionalClientScopes.add("microprofile-jwt");

        clientRepresentation.setDefaultClientScopes(clientScopes);
        clientRepresentation.setOptionalClientScopes(optionalClientScopes);
        this.client.realm(organization.getName()).clients().create(clientRepresentation);
    }

    @Override
    public void createOAuthProxyClient(Organization organization) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId("oauth2-proxy");
        clientRepresentation.setProtocol("openid-connect");
        clientRepresentation.setPublicClient(false);
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("*");
        clientRepresentation.setRedirectUris(redirectUris);

        List<ProtocolMapperRepresentation> protocols = new ArrayList<>();

        Map<String, String> config = new HashMap<>();
        config.put("access.token.claim", "true");
        config.put("claim.name", "groups");
        config.put("id.token.claim", "true");
        config.put("userinfo.token.claim", "true");

        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();

        protocolMapper.setName("groups");
        protocolMapper.setProtocol("openid-connect");
        protocolMapper.setProtocolMapper("oidc-group-membership-mapper");
        protocolMapper.setConfig(config);
        protocols.add(protocolMapper);

        clientRepresentation.setProtocolMappers(protocols);
        this.client.realm(organization.getName()).clients().create(clientRepresentation);
    }

    @Override
    public void createClientScope(Organization organization) throws InternalError, IOException {

        ClientScopeRepresentation clientScopeRepresentation = new ClientScopeRepresentation();
        clientScopeRepresentation.setName("groups");
        clientScopeRepresentation.setProtocol("openid-connect");

        List<ProtocolMapperRepresentation> protocols = new ArrayList<>();
        ProtocolMapperRepresentation protocolMapperName = new ProtocolMapperRepresentation();
        Map<String, String> configName = new HashMap<>();
        configName.put("access.token.claim", "true");
        configName.put("claim.name", "name");
        configName.put("id.token.claim", "true");
        configName.put("jsonType.label", "String");
        configName.put("user.attribute", "memberof");
        configName.put("userinfo.token.claim", "true");

        protocolMapperName.setName("name");
        protocolMapperName.setProtocol("openid-connect");
        protocolMapperName.setProtocolMapper("oidc-usermodel-attribute-mapper");
        protocolMapperName.setConfig(configName);
        protocols.add(protocolMapperName);

        ProtocolMapperRepresentation protocolMapperGroups = new ProtocolMapperRepresentation();
        Map<String, String> configGroups = new HashMap<>();
        configGroups.put("access.token.claim", "true");
        configGroups.put("claim.name", "groups");
        configGroups.put("id.token.claim", "true");
        configGroups.put("full.path", "false");
        configGroups.put("userinfo.token.claim", "true");

        protocolMapperGroups.setName("groups");
        protocolMapperGroups.setProtocol("openid-connect");
        protocolMapperGroups.setProtocolMapper("oidc-group-membership-mapper");
        protocolMapperGroups.setConfig(configGroups);

        protocols.add(protocolMapperGroups);
        clientScopeRepresentation.setProtocolMappers(protocols);
        this.client.realm(organization.getName()).clientScopes().create(clientScopeRepresentation);
    }

}
