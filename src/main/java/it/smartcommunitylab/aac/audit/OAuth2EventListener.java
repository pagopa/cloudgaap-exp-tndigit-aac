package it.smartcommunitylab.aac.audit;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.event.OAuth2Event;
import it.smartcommunitylab.aac.oauth.event.TokenGrantEvent;

public class OAuth2EventListener
        implements ApplicationListener<OAuth2Event>, ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String TOKEN_GRANT = "TOKEN_GRANT";

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    protected ApplicationEventPublisher getPublisher() {
        return this.publisher;
    }

    public void onApplicationEvent(OAuth2Event event) {
        if (event instanceof TokenGrantEvent) {
            onTokenGrantEvent((TokenGrantEvent) event);
        }

    }

    public void onTokenGrantEvent(TokenGrantEvent event) {
        logger.debug("token grant event " + event.toString());
        if (event.getToken() instanceof AACOAuth2AccessToken) {
            AACOAuth2AccessToken token = (AACOAuth2AccessToken) event.getToken();
            OAuth2Authentication auth = event.getAuthentication();

            String principal = auth.getName();
            String realm = token.getRealm();
            String type = auth.getUserAuthentication() == null ? "client" : "user";

            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
            data.put("token", token.getValue());
            data.put("scope", token.getScope());

            data.put("jti", token.getToken());
            data.put("realm", token.getRealm());
            data.put("expiration", token.getExpiration());
            data.put("issuedAt", token.getIssuedAt());

            if (token.getAudience() != null) {
                data.put("audience", StringUtils.collectionToCommaDelimitedString(Arrays.asList(token.getAudience())));
            }

            if (token.getAuthorizedParty() != null) {
                data.put("authorizedParty", token.getAuthorizedParty());
            }

            // build audit
            RealmAuditEvent audit = new RealmAuditEvent(realm, Instant.now(), principal, TOKEN_GRANT, data);

            // publish as event, listener will persist to store
            publish(audit);

        }

    }

    protected void publish(AuditEvent event) {
        if (getPublisher() != null) {
            getPublisher().publishEvent(new AuditApplicationEvent(event));
        }
    }

}
