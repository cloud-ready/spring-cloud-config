package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.security.Role.ADMIN;
import static cn.home1.cloud.config.server.security.Role.HOOK;
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
  private ConfigSecurity configSecurity;

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
        .antMatchers(this.configServerPrefix + "/").permitAll() //
        .antMatchers(this.configServerPrefix + "/deployKeyPublic").permitAll() //
        .antMatchers(this.configServerPrefix + "/decrypt").hasRole(ADMIN.toString()) //
        .antMatchers(this.configServerPrefix + "/encrypt", this.monitorEndpoint).permitAll() //
        .antMatchers(this.configServerPrefix + "/monitor").hasAnyRole(ADMIN.toString(), HOOK.toString()) //
        .antMatchers(new String[]{ //
            this.configServerPrefix + "/{application}/{profiles:.*[^-].*}", //
            this.configServerPrefix + "/{application}/{profiles}/{label:.*}", //
            this.configServerPrefix + "/{application}-{profiles}.json", //
            this.configServerPrefix + "/{label}/{application}-{profiles}.json", //
            this.configServerPrefix + "/{application}-{profiles}.properties", //
            this.configServerPrefix + "/{application}/{name}-{profiles}.properties", //
            this.configServerPrefix + "/{application}-{profiles}.yml", //
            this.configServerPrefix + "/{application}-{profiles}.yaml", //
            this.configServerPrefix + "/{label}/{application}-{profiles}.yml", //
            this.configServerPrefix + "/{label}/{application}-{profiles}.yaml", //
            this.configServerPrefix + "/{application}/{profiles}/{label}/**", //
        }).access("@applicationConfigSecurity.checkAuthenticationName(#application)")//
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
    userDetailsService.setConfigSecurity(this.configSecurity);
    userDetailsService.setConfigServerSecurityProperties(this.configServerSecurityProperties());
    userDetailsService.setEnvironmentController(this.environmentController);
    return userDetailsService;
  }
}
