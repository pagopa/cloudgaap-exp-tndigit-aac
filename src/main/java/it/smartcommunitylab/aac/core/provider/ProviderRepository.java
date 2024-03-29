package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

public interface ProviderRepository<T extends ResourceProvider> {

    T findByProviderId(String providerId);

    Collection<T> findAll();

    Collection<T> findByRealm(String realm);

    void addRegistration(T registration);

    void removeRegistration(String providerId);

    void removeRegistration(T registration);
}
