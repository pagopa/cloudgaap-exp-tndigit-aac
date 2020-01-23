package it.smartcommunitylab.aac.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.authority.DefaultAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBNativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.GoogleNativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.InternalAuthorityHandler;
import it.smartcommunitylab.aac.authority.NativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.NativeAuthorityHandlerContainer;
import it.smartcommunitylab.aac.oauth.CachedResourceStorage;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Configuration 
public class AACConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	@Bean
	public CachedResourceStorage getResourceStorage() {
		return new CachedResourceStorage();
	}
	
	@Bean
	public AuthorityHandlerContainer getAuthorityHandlerContainer() {
		Map<String, AuthorityHandler> map = Maps.newTreeMap();
		map.put(Config.IDP_INTERNAL, getInternalHandler());
		FBAuthorityHandler fh = new FBAuthorityHandler();
		map.put("facebook", fh);
		AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
		return bean;
	}

	@Bean
	public NativeAuthorityHandlerContainer getNativeAuthorityHandlerContainer() {
		Map<String, NativeAuthorityHandler> map = Maps.newTreeMap();
		
		GoogleNativeAuthorityHandler gh = new GoogleNativeAuthorityHandler(clientDetailsRepository);
		map.put("google", gh);
		FBNativeAuthorityHandler fh = new FBNativeAuthorityHandler();
		map.put("facebook", fh);
		
		NativeAuthorityHandlerContainer bean = new NativeAuthorityHandlerContainer(map);
		
		return bean;
	}

	@Bean
	public DefaultAuthorityHandler getDefaultHandler() {
		return new DefaultAuthorityHandler();
	}
	@Bean
	public InternalAuthorityHandler getInternalHandler() {
		return new InternalAuthorityHandler();
	}

	@Bean
	public CookieLocaleResolver getLocaleResolver() {
		CookieLocaleResolver bean = new CookieLocaleResolver();
		bean.setDefaultLocale(Locale.ITALY);
		return bean;
	}	

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	}	
	
    @Override
    public void configurePathMatch(final PathMatchConfigurer configurer) {
        //configure a sane path mapping, avoid huge security holes with: 
        // * spring security considering /about /about/ /about.any correctly as different
        // * spring MVC considering all those the same
        // result is only /about is protected by antMatcher, all the other variants are open to the world
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseTrailingSlashMatch(false);
    }
}
