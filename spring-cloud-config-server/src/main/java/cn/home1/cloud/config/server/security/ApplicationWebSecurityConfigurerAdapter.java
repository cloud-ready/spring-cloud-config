package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.security.Role.ADMIN;
import static cn.home1.cloud.config.server.security.Role.HOOK;
import static org.springframework.boot.autoconfigure.security.SecurityProperties.ACCESS_OVERRIDE_ORDER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
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
public class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

  @Autowired
  private ConfigSecurity configSecurity;

  @Autowired
  private ConfigServerProperties configServerProperties;

  @Autowired
  private EnvironmentController environmentController;

  @Value("${management.context-path:}")
  private String managementContextPath;

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
    final String configServerPrefix = this.configServerProperties.getPrefix();
    final String loginEndpoint = configServerPrefix + "/users/login";
    final String monitorEndpoint = configServerPrefix + "/monitor";

    http //
        .csrf().disable() //
        .authorizeRequests() //
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
        .and() //
        .httpBasic();
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
