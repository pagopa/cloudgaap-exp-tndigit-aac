package it.smartcommunitylab.aac.saml;

import java.io.Serializable;
import java.util.Collections;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.DefaultIdentityImpl;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

public class SamlUserIdentity extends DefaultIdentityImpl implements Serializable {

    private String username;
    private SamlAuthenticatedPrincipal principal;

    public SamlUserIdentity(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        attributes = Collections.emptySet();
    }

    public SamlUserIdentity(String provider, String realm, SamlAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        attributes = Collections.emptySet();
        this.principal = principal;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SAML;
    }

    @Override
    public void eraseCredentials() {
        // we do not have credentials or sensible data

    }

    @Override
    public SamlAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    /*
     * props: only getters we want this to be immutable
     */
    public String getUsername() {
        return username;
    }

    /*
     * Builder
     */
//    public static SamlUserIdentity from(SamlUserAccount user) {
//        SamlUserIdentity i = new SamlUserIdentity(user.getProvider(), user.getRealm());
//        i.account = user;
//        i.username = user.getUsername();
//
//        // TODO add attributes
//
//        return i;
//
//    }
}
