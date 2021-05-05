package it.smartcommunitylab.aac.openid.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCAccountProvider accountProvider;

    protected OIDCSubjectResolver(String providerId, OIDCUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");

        // build an internal provider bound to repository
        this.accountProvider = new OIDCAccountProvider(providerId, accountRepository, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByUserId(String userId) {
        logger.debug("resolve by user id " + userId);
        try {
            OIDCUserAccount account = accountProvider.getAccount(userId);

            // build subject with username
            return new Subject(account.getSubject(), account.getUsername());
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) {
        try {
            // let provider resolve to an account
            OIDCUserAccount account = accountProvider.getByIdentifyingAttributes(attributes);

            // build subject with username
            return new Subject(account.getSubject(), account.getUsername());
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

//    @Override
//    @Transactional(readOnly = true)
    public Collection<Set<String>> getIdentifyingAttributes() {
        // hardcoded, see repository
        List<Set<String>> attributes = new ArrayList<>();
        // id is enough
        attributes.add(Collections.singleton("userId"));

        // also realm+id attributes
        // init via stream to get an immutable set
        attributes.add(Stream.of("realm", "provider", "userId")
                .collect(Collectors.toSet()));
        attributes.add(Stream.of("realm", "provider", "email")
                .collect(Collectors.toSet()));

        return attributes;
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByLinkingAttributes(Map<String, String> attributes) {

        if (attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && getRealm().equals((attributes.get("realm")))) {
            // ensure we don't use additional params
            Map<String, String> idAttrs = new HashMap<>();
            idAttrs.put("realm", getRealm());
            idAttrs.put("provider", getProvider());
            idAttrs.put("email", attributes.get("email"));
            // let provider resolve to an account
            try {
                OIDCUserAccount account = accountProvider.getByIdentifyingAttributes(idAttrs);

                // build subject with username
                return new Subject(account.getSubject(), account.getUsername());
            } catch (NoSuchUserException nex) {
                return null;
            }
        } else {
            return null;
        }
    }

//    @Override
    public Collection<String> getLinkingAttributes() {
        // only realm+email
        // We don't want global linking with only email
        // Security risk: if we let users link cross domain accounts, a bogus realm
        // could set a provider and obtain identities in other realms!
        return Stream.of("realm", "email")
                .collect(Collectors.toSet());

    }

    @Override
    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof OIDCAuthenticatedPrincipal)) {
            return null;
        }

        Map<String, String> attributes = new HashMap<>();

        // export userId
        attributes.put("userId", exportInternalId(principal.getUserId()));

        return attributes;
    }

    @Override
    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof OIDCAuthenticatedPrincipal)) {
            return null;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put("realm", getRealm());

        // export userId
        attributes.put("userId", exportInternalId(principal.getUserId()));

        Map<String, String> map = principal.getAttributes();
        String email = map.get("email");
        if (StringUtils.hasText(email)) {
            attributes.put("email", email);
        }

        return attributes;
    }

}
