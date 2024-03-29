package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;

/*
 * Abstract class for user accounts
 * 
 * all implementations should derive from this
 */

public abstract class AbstractAccount extends AbstractBaseResource implements UserAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAccount(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

}
