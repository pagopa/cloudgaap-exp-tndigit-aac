package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Identity providers handle authentication for users and produce a valid user identity
 * 
 * An identity is composed by an account, bounded to the provider, and one or more attribute sets.
 * At minimum, we expect every provider to fulfill core attribute sets (basic, email, openid, account).
 */

public interface IdentityProvider extends ResourceProvider {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    /*
     * Config
     */
    public String getName();

    public String getDescription();

    public ConfigurableProperties getConfiguration();

    /*
     * auth provider
     */
    public ExtendedAuthenticationProvider getAuthenticationProvider();

    /*
     * internal providers
     */
    public AccountProvider getAccountProvider();

    public AttributeProvider getAttributeProvider();
    
    /*
     * subjects are global, we can resolve
     */

    public SubjectResolver getSubjectResolver();

    /*
     * convert identities from authenticatedPrincipal. Used for login only.
     * 
     * If given a subjectId the provider should update the account
     */

    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException;

    /*
     * fetch identities from this provider
     * 
     * implementations are not required to support this
     */

    // userId is provider-specific
    public UserIdentity getIdentity(String subject, String userId) throws NoSuchUserException;

    public UserIdentity getIdentity(String subject, String userId, boolean fetchAttributes) throws NoSuchUserException;

    /*
     * fetch for subject
     * 
     * opt-in, loads identities outside login for persisted accounts linked to the
     * subject
     * 
     * providers implementing this will enable the managers to fetch identities
     * outside the login flow!
     */

    public Collection<? extends UserIdentity> listIdentities(String subject);

    public Collection<? extends UserIdentity> listIdentities(String subject, boolean fetchAttributes);

    /*
     * Delete accounts.
     * 
     * Implementations are required to implement this, even as a no-op. At minimum
     * we expect providers to clean up any local registration or cache.
     */
    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException;

    public void deleteIdentities(String subjectId);

    /*
     * Login
     * 
     * Url is required to be presented in login forms, while authEntrypoint can
     * handle different kind of requests.
     */

    public String getAuthenticationUrl();

//    public AuthenticationEntryPoint getAuthenticationEntryPoint();

    public String getDisplayMode();

    /*
     * Additional action urls
     */
    public Map<String, String> getActionUrls();
}
