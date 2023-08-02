package uk.gov.hmcts.reform.pcqconsolidationservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfiguration {

    @Autowired
    HandlerMappingIntrospector introspector;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            new MvcRequestMatcher(introspector, "/swagger-ui.html"),
            new MvcRequestMatcher(introspector, "/webjars/springfox-swagger-ui/**"),
            new MvcRequestMatcher(introspector, "/swagger-resources/**"),
            new MvcRequestMatcher(introspector, "/health"),
            new MvcRequestMatcher(introspector, "/health/liveness"),
            new MvcRequestMatcher(introspector, "/v2/api-docs/**"),
            new MvcRequestMatcher(introspector, "/info"),
            new MvcRequestMatcher(introspector, "/favicon.ico"),
            new MvcRequestMatcher(introspector, "/")
        );
    }
}