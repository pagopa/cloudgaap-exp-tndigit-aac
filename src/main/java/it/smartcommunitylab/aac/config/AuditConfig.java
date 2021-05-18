package it.smartcommunitylab.aac.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import it.smartcommunitylab.aac.audit.AuthenticationEventListener;
import it.smartcommunitylab.aac.audit.AuthorizationEventListener;
import it.smartcommunitylab.aac.audit.ExtendedAuthenticationEventPublisher;
import it.smartcommunitylab.aac.audit.OAuth2EventListener;
import it.smartcommunitylab.aac.audit.store.AutoJdbcAuditEventStore;

/*
 * Audit configuration
 * Event based trail with persistence
 */

@Configuration
@Order(5)
public class AuditConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public ExtendedAuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new ExtendedAuthenticationEventPublisher(applicationEventPublisher);
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }

//    @Bean
//    public AuditEventRepository auditEventRepository() {
//        return new InMemoryAuditEventRepository();
//    }

    @Bean
    public AutoJdbcAuditEventStore auditEventRepository() {
        return new AutoJdbcAuditEventStore(dataSource);
    }

    @Bean
    public AuthenticationEventListener authenticationEventListener() {
        return new AuthenticationEventListener();
    }

    @Bean
    public OAuth2EventListener oauth2EventListener() {
        return new OAuth2EventListener();
    }

    @Bean
    public AuthorizationEventListener authorizationEventListener() {
        return new AuthorizationEventListener();
    }

}
