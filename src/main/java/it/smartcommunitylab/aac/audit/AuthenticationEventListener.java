package it.smartcommunitylab.aac.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.core.auth.ClientAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;

public class AuthenticationEventListener extends AbstractAuthenticationAuditListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";

    public static final String USER_AUTHENTICATION_FAILURE = "USER_AUTHENTICATION_FAILURE";
    public static final String USER_AUTHENTICATION_SUCCESS = "USER_AUTHENTICATION_SUCCESS";

    public static final String CLIENT_AUTHENTICATION_FAILURE = "CLIENT_AUTHENTICATION_FAILURE";
    public static final String CLIENT_AUTHENTICATION_SUCCESS = "CLIENT_AUTHENTICATION_SUCCESS";

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AbstractAuthenticationFailureEvent) {
            onAuthenticationFailureEvent((AbstractAuthenticationFailureEvent) event);
        } else if (event instanceof UserAuthenticationSuccessEvent) {
            onUserAuthenticationSuccessEvent((UserAuthenticationSuccessEvent) event);
        } else if (event instanceof AuthenticationSuccessEvent) {
            onAuthenticationSuccessEvent((AuthenticationSuccessEvent) event);
        }
    }

    private void onUserAuthenticationSuccessEvent(UserAuthenticationSuccessEvent event) {
        UserAuthenticationToken auth = event.getAuthenticationToken();
        String principal = auth.getSubjectId();
        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();

        String eventType = USER_AUTHENTICATION_SUCCESS;

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);

        // persist web details, should be safe to store
        if (auth.getWebAuthenticationDetails() != null) {
            data.put("details", auth.getWebAuthenticationDetails());
        }

        // build audit
        RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onAuthenticationFailureEvent(AbstractAuthenticationFailureEvent event) {

        AuthenticationException ex = event.getException();
        Authentication auth = event.getAuthentication();
        String principal = auth.getName();
        Object details = auth.getDetails();
        String eventType = AUTHENTICATION_FAILURE;

        // try to extract details if wrapped
        if (auth instanceof WrappedAuthenticationToken) {
            WrappedAuthenticationToken token = (WrappedAuthenticationToken) auth;
            eventType = USER_AUTHENTICATION_FAILURE;
            auth = token.getAuthenticationToken();
            principal = auth.getName();
            details = token.getAuthenticationDetails();

        }

        if (auth instanceof ClientAuthenticationToken) {
            ClientAuthenticationToken token = (ClientAuthenticationToken) auth;
            eventType = CLIENT_AUTHENTICATION_FAILURE;
            details = token.getWebAuthenticationDetails();
        }

        // build data
        Map<String, Object> data = new HashMap<>();
        data.put("type", ex.getClass().getName());
        data.put("message", ex.getMessage());
        data.put("auth", auth.getClass().getName());

        // persist details, should be safe to store
        if (details != null) {
            data.put("details", details);
        }

        // build audit
        AuditEvent audit = new AuditEvent(principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }

    private void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {

        Authentication auth = event.getAuthentication();
        String principal = auth.getName();
        Object details = auth.getDetails();
        String eventType = AUTHENTICATION_SUCCESS;

        Map<String, Object> data = new HashMap<>();
        data.put("auth", auth.getClass().getName());

        // persist details, should be safe to store
        if (details != null) {
            data.put("details", details);
        }

        // check if user auth
        if (auth instanceof UserAuthenticationToken) {
            UserAuthenticationToken token = (UserAuthenticationToken) auth;
            eventType = USER_AUTHENTICATION_SUCCESS;
            data.put("realm", token.getRealm());
            // TODO get last provider from tokens, needs ordering or dedicated field
        }
        if (auth instanceof ClientAuthenticationToken) {
            ClientAuthenticationToken token = (ClientAuthenticationToken) auth;
            eventType = CLIENT_AUTHENTICATION_SUCCESS;
            details = token.getWebAuthenticationDetails();
            data.put("realm", token.getRealm());

        }

        // build audit
        AuditEvent audit = new AuditEvent(principal, eventType, data);

        // publish as event, listener will persist to store
        publish(audit);
    }
}
