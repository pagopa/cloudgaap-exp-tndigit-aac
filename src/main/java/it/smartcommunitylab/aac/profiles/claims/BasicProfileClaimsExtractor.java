package it.smartcommunitylab.aac.profiles.claims;

import java.util.Collection;
import java.util.Collections;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.service.BasicProfileExtractor;

public class BasicProfileClaimsExtractor extends ProfileClaimsExtractor {
    private final BasicProfileExtractor extractor;

    public BasicProfileClaimsExtractor() {
        this.extractor = new BasicProfileExtractor();
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_BASIC_PROFILE);
    }

    @Override
    public String getKey() {
        // no key for basic, we merge to TLD
        return null;
    }

    @Override
    protected AbstractProfile buildUserProfile(User user, Collection<String> scopes)
            throws InvalidDefinitionException {

        BasicProfile profile = extractor.extractUserProfile(user);

        return profile;
    }

}