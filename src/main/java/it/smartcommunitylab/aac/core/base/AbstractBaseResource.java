package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserResource;

public abstract class AbstractBaseResource implements UserResource, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;
    private final String provider;

    protected AbstractBaseResource(String authority, String provider, String realm) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractBaseResource() {
        this((String) null, (String) null, (String) null);
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
    }

    public String getProvider() {
        return provider;
    }

    /*
     * Resource id logic
     * 
     * authority|provider|id
     * 
     * subclasses can override
     */

    public static final String ID_SEPARATOR = "|";

    protected String exportInternalId(String internalId) {
        return getAuthority() + ID_SEPARATOR + getProvider() + ID_SEPARATOR + internalId;
    }

    protected String parseResourceId(String resourceId) throws IllegalArgumentException {
        if (!StringUtils.hasText(resourceId)) {
            throw new IllegalArgumentException("empty or null id");
        }

        String[] s = resourceId.split(Pattern.quote(ID_SEPARATOR));

        if (s.length != 3) {
            throw new IllegalArgumentException("invalid resource id");
        }

        // check match
        if (!getAuthority().equals(s[0])) {
            throw new IllegalArgumentException("authority mismatch");
        }

        if (!getProvider().equals(s[1])) {
            throw new IllegalArgumentException("provider mismatch");
        }

        if (!StringUtils.hasText(s[2])) {
            throw new IllegalArgumentException("empty resource id");
        }

        return s[2];

    }

}
