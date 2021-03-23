package it.smartcommunitylab.aac.oauth.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.ClientService;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientInfo;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

/*
 * Client service for internal usage
 * 
 * Serves the OAuth2 authorization server components
 */

@Service
public class OAuth2ClientService implements ClientService {

    // we use service since we are outside core
    private final ClientEntityService clientService;

    // our client repo
    private final OAuth2ClientEntityRepository oauthClientRepository;

    public OAuth2ClientService(ClientEntityService clientService,
            OAuth2ClientEntityRepository oauthClientRepository) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(oauthClientRepository, "oauth client repository is mandatory");
        this.clientService = clientService;
        this.oauthClientRepository = oauthClientRepository;

    }

    public OAuth2Client findClient(String clientId) {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            return null;
        }

        return OAuth2Client.from(client, oauth);

    }

    @Override
    public OAuth2Client getClient(String clientId) throws NoSuchClientException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        return OAuth2Client.from(client, oauth);

    }

//    @Override
    public Collection<BaseClient> listClients() {
        List<BaseClient> result = new ArrayList<>();
        List<OAuth2ClientEntity> oauths = oauthClientRepository.findAll();

        for (OAuth2ClientEntity oauth : oauths) {
            ClientEntity client = clientService.findClient(oauth.getClientId());
            if (client != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
    }

    public List<OAuth2Client> listClients(String realm) {
        List<OAuth2Client> result = new ArrayList<>();
        Collection<ClientEntity> clients = clientService.listClients(realm, OAuth2Client.CLIENT_TYPE);

        for (ClientEntity client : clients) {
            OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(client.getClientId());
            if (oauth != null) {
                result.add(OAuth2Client.from(client, oauth));
            }
        }

        return result;
    }

    @Override
    public ClientSecret getClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        return (oauth.getClientSecret() == null ? null : new ClientSecret(oauth.getClientSecret()));
    }

    /*
     * Reset client secret, autogenerated
     */
    @Override
    public ClientSecret resetClientCredentials(String clientId) throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        String secret = generateClientSecret();
        oauth.setClientSecret(secret);

        oauth = oauthClientRepository.save(oauth);

        return new ClientSecret(oauth.getClientSecret());

    }

    /*
     * Set client secret to a given value
     * 
     * to be used internally for bootstrap/import etc
     */
    @Override
    public ClientSecret setClientCredentials(String clientId, ClientCredentials credentials)
            throws NoSuchClientException {
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth == null) {
            throw new NoSuchClientException();
        }

        // we expect a string as secret
        if (!(credentials instanceof ClientSecret)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String secret = ((ClientSecret) credentials).getCredentials();

        // TODO validate secret: length, complexity, policies etc
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("invalid secret");
        }

        oauth.setClientSecret(secret);

        oauth = oauthClientRepository.save(oauth);

        return new ClientSecret(oauth.getClientSecret());

    }

    /*
     * Client management
     */
    public OAuth2Client addClient(String realm, String name) {
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return this.addClient(realm, clientId, name);
    }

    public OAuth2Client addClient(String realm, String clientId, String name) {
        return this.addClient(realm, clientId, name,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    public OAuth2Client addClient(
            String realm,
            String name, String description,
            Collection<String> scopes,
            Collection<String> providers,
            String clientSecret,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            TokenType tokenType,
            Integer accessTokenValidity, Integer refreshTokenValidity,
            JWSAlgorithm jwtSignAlgorithm,
            EncryptionMethod jwtEncMethod, JWEAlgorithm jwtEncAlgorithm,
            String jwks, String jwksUri,
            OAuth2ClientInfo additionalInfo) throws IllegalArgumentException {

        // generate a clientId and then add
        ClientEntity client = clientService.createClient();
        String clientId = client.getClientId();

        return addClient(
                realm, clientId,
                name, description,
                scopes,
                providers,
                clientSecret,
                authorizedGrantTypes,
                redirectUris,
                tokenType,
                accessTokenValidity, refreshTokenValidity,
                jwtSignAlgorithm,
                jwtEncMethod, jwtEncAlgorithm,
                jwks, jwksUri,
                additionalInfo);

    }

    public OAuth2Client addClient(
            String realm, String clientId,
            String name, String description,
            Collection<String> scopes,
            Collection<String> providers,
            String clientSecret,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            TokenType tokenType,
            Integer accessTokenValidity, Integer refreshTokenValidity,
            JWSAlgorithm jwtSignAlgorithm,
            EncryptionMethod jwtEncMethod, JWEAlgorithm jwtEncAlgorithm,
            String jwks, String jwksUri,
            OAuth2ClientInfo additionalInfo) throws IllegalArgumentException {

        // TODO add custom validator for class
        // manual validation for now
        if (!StringUtils.hasText(clientId)) {
            // regenerate clientId
            clientId = clientService.createClient().getClientId();
        }

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("realm cannot be empty");
        }

        // TODO validate secret
        if (!StringUtils.hasText(clientSecret)) {
            clientSecret = generateClientSecret();
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream()
                    .anyMatch(gt -> !ArrayUtils.contains(AuthorizationGrantType.values(), gt))) {
                throw new IllegalArgumentException("Invalid grant type");
            }
        }

        String tokenTypeValue = null;
        if (tokenType != null) {
            if (!ArrayUtils.contains(TokenType.values(), tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            tokenTypeValue = tokenType.getValue();
        } else {
            // default is null, will use system default
            tokenType = null;
        }

        String jwtSignAlgorithmName = null;
        if (jwtSignAlgorithm != null) {
            jwtSignAlgorithmName = jwtSignAlgorithm.getName();
        }

        String jwtEncMethodName = null;
        if (jwtEncMethod != null) {
            jwtEncMethodName = jwtEncMethod.getName();
        }

        String jwtEncAlgorithmName = null;
        if (jwtEncAlgorithm != null) {
            jwtEncAlgorithmName = jwtEncAlgorithm.getName();
        }

        ClientEntity client = clientService.addClient(
                clientId, realm, OAuth2Client.CLIENT_TYPE,
                name, description,
                scopes, providers);

        OAuth2ClientEntity oauth = new OAuth2ClientEntity();
        oauth.setClientId(clientId);
        oauth.setClientSecret(clientSecret);
        oauth.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizedGrantTypes));
        oauth.setRedirectUris(StringUtils.collectionToCommaDelimitedString(redirectUris));
        oauth.setTokenType(tokenTypeValue);
        oauth.setAccessTokenValidity(accessTokenValidity);
        oauth.setRefreshTokenValidity(refreshTokenValidity);
        oauth.setJwtSignAlgorithm(jwtSignAlgorithmName);
        oauth.setJwtEncMethod(jwtEncMethodName);
        oauth.setJwtEncAlgorithm(jwtEncAlgorithmName);
        oauth.setJwks(jwks);
        oauth.setJwksUri(jwksUri);
        if (additionalInfo != null) {
            oauth.setAdditionalInformation(additionalInfo.toJson());
        }
        oauth = oauthClientRepository.save(oauth);

        return OAuth2Client.from(client, oauth);
    }

    public OAuth2Client updateClient(
            String clientId,
            String name, String description,
            Collection<String> scopes,
            Collection<String> providers,
            Map<String, String> hookFunctions,
            Collection<AuthorizationGrantType> authorizedGrantTypes,
            Collection<String> redirectUris,
            TokenType tokenType,
            Integer accessTokenValidity, Integer refreshTokenValidity,
            JWSAlgorithm jwtSignAlgorithm,
            EncryptionMethod jwtEncMethod, JWEAlgorithm jwtEncAlgorithm,
            String jwks, String jwksUri,
            OAuth2ClientInfo additionalInfo) throws NoSuchClientException, IllegalArgumentException {

        // TODO add custom validator for class
        // manual validation for now
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        if (authorizedGrantTypes != null) {
            // check if valid grants
            if (authorizedGrantTypes.stream()
                    .anyMatch(gt -> !ArrayUtils.contains(AuthorizationGrantType.values(), gt))) {
                throw new IllegalArgumentException("Invalid grant type");
            }
        }

        String tokenTypeValue = null;
        if (tokenType != null) {
            if (!ArrayUtils.contains(TokenType.values(), tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            tokenTypeValue = tokenType.getValue();
        } else {
            // default is null, will use system default
            tokenType = null;
        }

        String jwtSignAlgorithmName = null;
        if (jwtSignAlgorithm != null) {
            jwtSignAlgorithmName = jwtSignAlgorithm.getName();
        }

        String jwtEncMethodName = null;
        if (jwtEncMethod != null) {
            jwtEncMethodName = jwtEncMethod.getName();
        }

        String jwtEncAlgorithmName = null;
        if (jwtEncAlgorithm != null) {
            jwtEncAlgorithmName = jwtEncAlgorithm.getName();
        }

        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (client == null || oauth == null) {
            throw new NoSuchClientException();
        }

        client = clientService.updateClient(clientId, name, description, scopes, providers, hookFunctions);

        oauth.setAuthorizedGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizedGrantTypes));
        oauth.setRedirectUris(StringUtils.collectionToCommaDelimitedString(redirectUris));
        oauth.setTokenType(tokenTypeValue);
        oauth.setAccessTokenValidity(accessTokenValidity);
        oauth.setRefreshTokenValidity(refreshTokenValidity);
        oauth.setJwtSignAlgorithm(jwtSignAlgorithmName);
        oauth.setJwtEncMethod(jwtEncMethodName);
        oauth.setJwtEncAlgorithm(jwtEncAlgorithmName);
        oauth.setJwks(jwks);
        oauth.setJwksUri(jwksUri);
        if (additionalInfo != null) {
            oauth.setAdditionalInformation(additionalInfo.toJson());
        }
        oauth = oauthClientRepository.save(oauth);

        return OAuth2Client.from(client, oauth);
    }

    public void deleteClient(String clientId) {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = oauthClientRepository.findByClientId(clientId);

        if (oauth != null) {
            oauthClientRepository.delete(oauth);
        }

        if (client != null) {
            clientService.deleteClient(clientId);
        }
    }

    /*
     * Helpers
     */

    /**
     * Generate new value to be used as client secret (String)
     * 
     * @return
     */
    private synchronized String generateClientSecret() {
        return UUID.randomUUID().toString();
    }

}
