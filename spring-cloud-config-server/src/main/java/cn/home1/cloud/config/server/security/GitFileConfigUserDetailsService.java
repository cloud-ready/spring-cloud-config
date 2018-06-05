package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.util.EnvironmentUtils.getConfigPassword;

import com.google.common.collect.ImmutableList;

import cn.home1.cloud.config.server.util.Consts;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

/**
 * config-server-client use the password configured in GIT. the user name must be the app name.
 */
@Slf4j
public class GitFileConfigUserDetailsService implements UserDetailsService {

  private static final Collection<? extends GrantedAuthority> ANONYMOUS_AUTHORITY = ImmutableList.of();

  private static final Collection<? extends GrantedAuthority> ADMIN_AUTHORITY =
      ImmutableList.of(new SimpleGrantedAuthority("ROLE_" + Role.ADMIN.toString()));

  private static final Collection<? extends GrantedAuthority> HOOK_AUTHORITY =
      ImmutableList.of(new SimpleGrantedAuthority("ROLE_" + Role.HOOK.toString()));

  private static final Collection<? extends GrantedAuthority> USER_AUTHORITY =
      ImmutableList.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.toString()));

  @Setter
  private ConfigSecurity configSecurity;

  @Setter
  private EnvironmentController environmentController;

  @Setter
  private ConfigServerSecurityProperties configServerSecurityProperties;

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    if (username == null) {
      return new User("anonymous", "", ANONYMOUS_AUTHORITY);
    }

    if (username.equals(this.configServerSecurityProperties.getAdminName())) {
      return new User(username, this.configServerSecurityProperties.getAdminPassword(), ADMIN_AUTHORITY);
    }

    if (username.equals(this.configServerSecurityProperties.getHookName())) {
      return new User(username, this.configServerSecurityProperties.getHookPassword(), HOOK_AUTHORITY);
    }

    // get environment without profile
    final Environment environment;

    try {
      environment = this.environmentController.defaultLabel(username, Consts.PROFILE_NOT_EXIST);
    } catch (final Exception exception) {
      log.warn("error on loadUserByUsername from git configuration file, application (user) name:{} ", //
          username, exception);
      throw exception;
    }

    if (environment == null) {
      throw new UsernameNotFoundException("can not find the project with the name:" + username);
    }

    final String expectedPassword = getConfigPassword(environment);
    if (expectedPassword == null) {
      throw new BadCredentialsException(
          "can not find the configPassword (spring.cloud.config.configPassword) from environment:" + environment.getName());
    }
    final String rawExpectedPassword = this.configSecurity.decryptProperty(expectedPassword);

    return new User(username, rawExpectedPassword, USER_AUTHORITY);
  }
}
