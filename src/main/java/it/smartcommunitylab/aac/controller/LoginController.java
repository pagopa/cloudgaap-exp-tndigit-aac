package it.smartcommunitylab.aac.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.model.Realm;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private RealmManager realmManager;

    @RequestMapping(value = {
            "/login",
            "/-/{realm}/login",
            "/-/{realm}/login/{providerId}"
    }, method = RequestMethod.GET)
    public String login(
            @PathVariable("realm") Optional<String> realmKey,
            @PathVariable("providerId") Optional<String> providerKey,
            Model model,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        // TODO handle /login as COMMON login, ie any realm is valid
        String realm = SystemKeys.REALM_SYSTEM;
        String providerId = "";

        // fetch realm+provider
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }
        if (providerKey.isPresent()) {
            providerId = providerKey.get();
        }

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("no suitable realm for login");
        }

        model.addAttribute("realm", realm);

        String displayName = null;
        if (!realm.equals(SystemKeys.REALM_COMMON)) {
            Realm re = realmManager.getRealm(realm);
            displayName = re.getName();
        }

        model.addAttribute("displayName", displayName);

        // fetch providers for given realm
        Collection<IdentityProvider> providers = providerManager.getIdentityProviders(realm);

        if (StringUtils.hasText(providerId)) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            if (idp.getRealm().equals(realm)) {
                providers = Collections.singleton(idp);
            }
        }

        // fetch as authorities model
        // TODO make an helper to format
        List<LoginAuthorityBean> authorities = new ArrayList<>();
        LoginAuthorityBean internal = null;
        for (IdentityProvider idp : providers) {
            LoginAuthorityBean a = new LoginAuthorityBean();
            a.authority = idp.getAuthority();
            a.provider = idp.getProvider();
            a.realm = idp.getRealm();
            a.loginUrl = idp.getAuthenticationUrl();
//            a.name = idp.getName();
            a.name = idp.getName();
            String key = a.name.trim()
                    .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            a.cssClass = "provider-" + key;
            a.icon = "key";

            if (ArrayUtils.contains(icons, key)) {
                a.icon = key;
            }

            if (SystemKeys.AUTHORITY_INTERNAL.equals(a.authority)) {
                internal = a;
                // also lookup internal service for registration/reset links
                IdentityService ids = providerManager.fetchIdentityService(idp.getRealm(), idp.getProvider());
                if (ids != null) {
                    if (ids.canRegister() && StringUtils.hasText(ids.getRegistrationUrl())) {
                        a.registrationUrl = ids.getRegistrationUrl();
                    }

                    if (ids.getCredentialsService() != null
                            && ids.getCredentialsService().canReset()
                            && StringUtils.hasText(ids.getCredentialsService().getResetUrl())) {
                        a.resetUrl = ids.getCredentialsService().getResetUrl();
                    }

                }

            } else {

                if (StringUtils.hasText(a.loginUrl)) {
                    authorities.add(a);
                }
            }
        }

        Collections.sort(authorities);

        if (internal != null) {
            model.addAttribute("internalAuthority", internal);
        }

        model.addAttribute("externalAuthorities", authorities);

        // check errors
        AuthenticationException error = (AuthenticationException) req.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error != null) {
            model.addAttribute("error", error.getMessage());

            // also remove from session
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }

    private String[] icons = {
            "twitter", "facebook", "github"
    };

    private class LoginAuthorityBean implements Comparable {
        public String provider;
        public String authority;
        public String realm;
        public String loginUrl;
        public String registrationUrl;
        public String resetUrl;
        public String icon;
        public String name;
        public String cssClass;

        @Override
        public int compareTo(Object o) {
            if (o instanceof LoginAuthorityBean) {
                return name.compareTo(((LoginAuthorityBean) o).name);
            } else {
                return 0;
            }
        }

    }

}
