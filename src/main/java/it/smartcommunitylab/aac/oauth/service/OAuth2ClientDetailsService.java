package it.smartcommunitylab.aac.oauth.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;

@Transactional
public class OAuth2ClientDetailsService implements ClientDetailsService {

    // TODO evaluate direct repo access VS service
    // we lose validation but reduce complexity
//    private final OAuth2ClientService clientService;
    private final OAuth2ClientEntityRepository clientRepository;

    // we need access to client roles, we use service since we are outside core
    private final ClientEntityService clientService;

    public OAuth2ClientDetailsService(ClientEntityService clientService,
            OAuth2ClientEntityRepository clientRepository) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(clientRepository, "oauth client repository is mandatory");
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    // TODO add a local cache, client definitions don't change frequently
    // even short window (30s) could cover a whole request
    @Override
    @Transactional(readOnly = true)
    public OAuth2ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientEntity client = clientService.findClient(clientId);
        OAuth2ClientEntity oauth = clientRepository.findByClientId(clientId);
        if (client == null || oauth == null) {
            throw new NoSuchClientException("No client with requested id: " + clientId);
        }

        // build details
        OAuth2ClientDetails clientDetails = new OAuth2ClientDetails();
        clientDetails.setRealm(client.getRealm());
        clientDetails.setName(client.getName());
        clientDetails.setClientId(clientId);
        clientDetails.setClientSecret(oauth.getClientSecret());
        clientDetails.setScope(StringUtils.commaDelimitedListToSet(client.getScopes()));
        clientDetails.setResourceIds(StringUtils.commaDelimitedListToSet(client.getResourceIds()));

        clientDetails.setAuthorizedGrantTypes(StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes()));
        clientDetails.setRegisteredRedirectUri(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));
        clientDetails.setAuthenticationMethods(StringUtils.commaDelimitedListToSet(oauth.getAuthenticationMethods()));

        clientDetails.setFirstParty(oauth.isFirstParty());
        clientDetails.setAutoApproveScopes(StringUtils.commaDelimitedListToSet(oauth.getAutoApproveScopes()));

        // token settings
        clientDetails.setTokenType(oauth.getTokenType());
        clientDetails.setAccessTokenValiditySeconds(oauth.getAccessTokenValidity());
        clientDetails.setRefreshTokenValiditySeconds(oauth.getRefreshTokenValidity());

        // JWT config
        clientDetails.setJwks(oauth.getJwks());
        clientDetails.setJwksUri(oauth.getJwksUri());
        clientDetails.setJwtSignAlgorithm(oauth.getJwtSignAlgorithm());
        clientDetails.setJwtEncMethod(oauth.getJwtEncMethod());
        clientDetails.setJwtEncAlgorithm(oauth.getJwtEncAlgorithm());

        // map additional info
        if (oauth.getAdditionalInformation() != null) {
            clientDetails.setAdditionalInformation(oauth.getAdditionalInformation());
        }

        // always grant role_client
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));
        try {
            List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);

            authorities.addAll(clientRoles.stream()
                    .filter(r -> !StringUtils.hasText(r.getRealm()))
                    .map(r -> new SimpleGrantedAuthority(r.getRole()))
                    .collect(Collectors.toSet()));

            authorities.addAll(clientRoles.stream()
                    .filter(r -> StringUtils.hasText(r.getRealm()))
                    .map(r -> new RealmGrantedAuthority(r.getRealm(), r.getRole()))
                    .collect(Collectors.toSet()));

        } catch (it.smartcommunitylab.aac.common.NoSuchClientException e) {
//            throw new NoSuchClientException("No client with requested id: " + clientId);
        }

        clientDetails.setAuthorities(Collections.unmodifiableCollection(authorities));

        return clientDetails;
    }

}
