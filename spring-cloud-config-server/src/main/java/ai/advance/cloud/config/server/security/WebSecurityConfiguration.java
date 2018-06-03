package ai.advance.cloud.config.server.security;

import static ai.advance.cloud.config.server.security.Role.ADMIN;
import static ai.advance.cloud.config.server.security.Role.HOOK;
import static org.springframework.boot.autoconfigure.security.SecurityProperties.ACCESS_OVERRIDE_ORDER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@ConditionalOnProperty(prefix = "security.basic", name = "enabled", havingValue = "true")
@Configuration
@EnableWebSecurity
@Order(ACCESS_OVERRIDE_ORDER)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  private EnvironmentController environmentController;

  @Value("${spring.cloud.config.server.prefix:}")
  private String configServerPrefix;

  @Value("${spring.cloud.config.server.prefix:}/users/login")
  private String loginEndpoint;

  @Value("${management.context-path:}")
  private String managementContextPath;

  @Value("${spring.cloud.config.server.monitor.endpoint.path:}/monitor")
  private String monitorEndpoint;

  @Override
  public void init(final WebSecurity web) throws Exception {
    super.init(web);
  }

  @Override
  protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(this.userDetailsService()).passwordEncoder(NoOpPasswordEncoder.getInstance());
  }

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    http //
        .csrf().disable() //
        .authorizeRequests() //
        .antMatchers(this.configServerPrefix + "/").permitAll()//
        .antMatchers(this.configServerPrefix + "/deployKeyPublic").permitAll()//
        .antMatchers(this.configServerPrefix + "/encrypt", this.monitorEndpoint)
        .hasAnyRole(ADMIN.toString(), HOOK.toString())
        .antMatchers(this.configServerPrefix + "/decrypt")
        .hasRole(ADMIN.toString()) //
        .antMatchers(new String[]{ //
            this.configServerPrefix + "/{name}/{profiles:.*[^-].*}", //
            this.configServerPrefix + "/{name}/{profiles}/{label:.*}", //
            this.configServerPrefix + "/{name}-{profiles}.properties", //
            this.configServerPrefix + "/{label}/{name}-{profiles}.properties", //
            this.configServerPrefix + "/{name}-{profiles}.json", //
            this.configServerPrefix + "/{label}/{name}-{profiles}.json", //
            this.configServerPrefix + "/{name}-{profiles}.yml", //
            this.configServerPrefix + "/{name}-{profiles}.yaml", //
            this.configServerPrefix + "/{label}/{name}-{profiles}.yml", //
            this.configServerPrefix + "/{label}/{name}-{profiles}.yaml", //
            this.configServerPrefix + "/{name}/{profile}/{label}/**", //
            this.configServerPrefix + "/{name}/{profile}/{label}/**",//
        }).access("@applicationConfigSecurity.checkAuthenticationName(#name)")//
        .anyRequest().hasRole(ADMIN.toString()) //
        .and() //
        .httpBasic();
  }

  @Bean
  public ApplicationConfigSecurity applicationConfigSecurity() {
    return new ApplicationConfigSecurity();
  }

  @Bean
  public ConfigServerSecurityProperties configServerSecurityProperties() {
    return new ConfigServerSecurityProperties();
  }

  @Bean
  public GitFileConfigUserDetailsService userDetailsService() {
    final GitFileConfigUserDetailsService userDetailsService = new GitFileConfigUserDetailsService();
    userDetailsService.setEnvironmentController(this.environmentController);
    userDetailsService.setConfigServerSecurityProperties(this.configServerSecurityProperties());
    return userDetailsService;
  }
}
