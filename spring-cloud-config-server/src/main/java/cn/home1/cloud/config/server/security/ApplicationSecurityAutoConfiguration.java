package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.security.Role.ACTUATOR;
import static cn.home1.cloud.config.server.security.Role.ADMIN;
import static cn.home1.cloud.config.server.security.Role.HOOK;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityDataConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.autoconfigure.security.servlet.SpringBootWebSecurityConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.WebSecurityEnablerConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * see: https://github.com/spring-projects/spring-boot/issues/12323
 * see: {@link org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration}
 * see: https://spring.io/blog/2017/09/15/security-changes-in-spring-boot-2-0-m4
 */
@Configuration
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
// @ConditionalOnProperty(prefix = "spring.security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SecurityProperties.class)
@Import({SpringBootWebSecurityConfiguration.class, WebSecurityEnablerConfiguration.class,
    SecurityDataConfiguration.class})
public class ApplicationSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthenticationEventPublisher.class)
    public DefaultAuthenticationEventPublisher authenticationEventPublisher(
        ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    // @ConditionalOnProperty(prefix = "spring.security", name = "enabled", havingValue = "true")
    @Configuration
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    //@Order(ApplicationSecurityAutoConfiguration.ACCESS_OVERRIDE_ORDER)
    static class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

//  /**
//   * org.springframework.boot.autoconfigure.security.SecurityProperties.ACCESS_OVERRIDE_ORDER
//   *
//   * TODO deprecated in spring-boot 2.x
//   */
//  public static final int ACCESS_OVERRIDE_ORDER = 2147483640;

        @Autowired
        private ConfigSecurity configSecurity;

        @Autowired
        private ConfigServerProperties configServerProperties;

        @Value("${spring.security.enabled:false}")
        private Boolean enabled;

        @Autowired
        private EnvironmentController environmentController;

        /**
         * TODO rename
         */
        @Value("${management.endpoints.web.base-path:}")
        private String managementContextPath;

        @Override
        public void init(final WebSecurity web) throws Exception {
            super.init(web);
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            if (this.enabled) {
                auth.userDetailsService(this.userDetailsService()).passwordEncoder(NoOpPasswordEncoder.getInstance());
            }
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            final String configServerPrefix = this.configServerProperties.getPrefix();
            //final String loginEndpoint = configServerPrefix + "/users/login";
            final String monitorEndpoint = configServerPrefix + "/monitor";

            //super.configure(http); // default config
            if (this.enabled) {
                http = http //
                    .authorizeRequests() //
                    .requestMatchers(EndpointRequest.to("health", "info")).permitAll() //
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(ACTUATOR.toString()) //
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() //
                    .antMatchers(configServerPrefix + "/").permitAll() //
                    .antMatchers(configServerPrefix + "/deployKeyPublic").permitAll() //
                    .antMatchers(configServerPrefix + "/decrypt").hasRole(ADMIN.toString()) //
                    .antMatchers(configServerPrefix + "/encrypt", monitorEndpoint).permitAll() //
                    .antMatchers(configServerPrefix + "/encryptParentPassword").hasRole(ADMIN.toString()) //
                    .antMatchers(configServerPrefix + "/monitor").hasAnyRole(ADMIN.toString(), HOOK.toString()) //
                    .antMatchers(new String[]{ //
                        configServerPrefix + "/{application}/{profiles:.*[^-].*}", //
                        configServerPrefix + "/{application}/{profiles}/{label:.*}", //
                        configServerPrefix + "/{application}-{profiles}.json", //
                        configServerPrefix + "/{label}/{application}-{profiles}.json", //
                        configServerPrefix + "/{application}-{profiles}.properties", //
                        configServerPrefix + "/{application}/{name}-{profiles}.properties", //
                        configServerPrefix + "/{application}-{profiles}.yml", //
                        configServerPrefix + "/{application}-{profiles}.yaml", //
                        configServerPrefix + "/{label}/{application}-{profiles}.yml", //
                        configServerPrefix + "/{label}/{application}-{profiles}.yaml", //
                        configServerPrefix + "/{application}/{profiles}/{label}/**", //
                    }).access("@applicationConfigSecurity.checkAuthentication(#application,#profiles)")//
                    .anyRequest().hasRole(ADMIN.toString()) //
                    //.antMatchers("/**").hasRole(USER.toString()) //
                    .and() //
                    .httpBasic().and() //
                    .sessionManagement().sessionCreationPolicy(STATELESS).and() //
                    .exceptionHandling()
                    // .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) //
                    .authenticationEntryPoint(new BasicAuthenticationEntryPoint()) //
                    .and() //
                ;
            } else {
                http = http //
                    .authorizeRequests() //
                    .antMatchers("/**").permitAll() //
                    .and() //
                ;
            }

            http //
                .csrf().disable() //
                .formLogin().disable() //
            ;
        }

        @Bean
        public ApplicationConfigSecurity applicationConfigSecurity() {
            return new ApplicationConfigSecurity();
        }

        @Bean
        public PrivilegedUserProperties privilegedUserProperties() {
            return new PrivilegedUserProperties();
        }

        @Bean
        public GitFileConfigUserDetailsService userDetailsService() {
            final GitFileConfigUserDetailsService userDetailsService = new GitFileConfigUserDetailsService();
            userDetailsService.setConfigSecurity(this.configSecurity);
            userDetailsService.setDefaultLabel(this.configServerProperties.getDefaultLabel());
            userDetailsService.setPrivilegedUserProperties(this.privilegedUserProperties());
            userDetailsService.setEnvironmentController(this.environmentController);
            return userDetailsService;
        }
    }
}
