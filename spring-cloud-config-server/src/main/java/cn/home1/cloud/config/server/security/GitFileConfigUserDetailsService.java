package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.util.Consts.DOT_ENV;
import static cn.home1.cloud.config.server.util.Consts.PRIVILEGE_ENV_PROFILE_;
import static cn.home1.cloud.config.server.util.Consts.PRIVILEGE_ENV_PROFILE_WILDCARD;
import static cn.home1.cloud.config.server.util.EnvironmentUtils.getConfigPassword;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;

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
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * config-server-client use the password configured in GIT. the user name must be the app name.
 */
@Slf4j
public class GitFileConfigUserDetailsService implements UserDetailsService {

  private static final Pattern PATTERN_AT = Pattern.compile("@");

  private static final Collection<? extends GrantedAuthority> ANONYMOUS_AUTHORITY = ImmutableList.of();

  private static final Collection<? extends GrantedAuthority> HOOK_AUTHORITY =
      ImmutableList.of(new SimpleGrantedAuthority("ROLE_" + Role.HOOK.toString()));

  private static final GrantedAuthority USER_AUTHORITY = new SimpleGrantedAuthority( //
      "ROLE_" + Role.USER.toString());

  @Setter
  private ConfigSecurity configSecurity;

  @Setter
  private String defaultLabel;

  @Setter
  private EnvironmentController environmentController;

  @Setter
  private PrivilegedUserProperties privilegedUserProperties;

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    if (username == null) {
      return new User("anonymous", "", ANONYMOUS_AUTHORITY);
    }

    if (username.equals(this.privilegedUserProperties.getAdminName())) {
      final Collection<? extends GrantedAuthority> adminAuthority = this.privilegedUserProperties.getAdminRoles() //
          .stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(toList());
      return new User(username, this.privilegedUserProperties.getAdminPassword(), adminAuthority);
    }

    if (username.equals(this.privilegedUserProperties.getHookName())) {
      return new User(username, this.privilegedUserProperties.getHookPassword(), HOOK_AUTHORITY);
    }

    // username is `spring.application.name` or `spring.application.name@{profile.env}`
    final String application;
    final String profileToAuth;
    final Optional<GrantedAuthority> privilege; // privilege of environment profile: can be specific one, none or anyone
    if (username.contains("@") && username.endsWith(DOT_ENV)) {
      final Iterator<String> parts = PATTERN_AT.splitAsStream(username).iterator();
      application = parts.next();
      profileToAuth = parts.next();
      privilege = Optional.of(new SimpleGrantedAuthority(PRIVILEGE_ENV_PROFILE_ + profileToAuth));
    } else if (username.endsWith("@default")) {
      final Iterator<String> parts = PATTERN_AT.splitAsStream(username).iterator();
      application = parts.next();
      profileToAuth = parts.next();
      privilege = Optional.empty();
    } else {
      application = username;
      profileToAuth = "default";
      privilege = Optional.of(new SimpleGrantedAuthority(PRIVILEGE_ENV_PROFILE_WILDCARD));
    }
    final Collection<GrantedAuthority> authorities = privilege.isPresent() ? //
        ImmutableList.of(USER_AUTHORITY, privilege.get()) : ImmutableList.of(USER_AUTHORITY);


    final String expectedPassword = this.findExpectedPassword(application, profileToAuth);
    if (expectedPassword == null) {
      throw new BadCredentialsException(
          "can not find 'spring.cloud.config.password' application: " + application + ", profile: " + profileToAuth);
    }
    final String rawExpectedPassword = this.configSecurity.decryptProperty(expectedPassword);

    return new User(application, rawExpectedPassword, authorities);
  }

  String findExpectedPassword(final String application, final String profile) {
    final String result;

    if (this.defaultLabel != null) {
      final String found = this.findExpectedPassword(application, profile, this.defaultLabel);
      result = found != null ? found : this.findExpectedPassword(application, profile, null);
    } else {
      result = this.findExpectedPassword(application, profile, null);
    }

    return result;
  }

  String findExpectedPassword(final String application, final String profile, final String label) {
    final Environment environment;

    try {
      environment = this.environmentController.labelled(application, profile, label);
    } catch (final Exception exception) {
      log.warn("error on loadUserByUsername from git configuration file, username: {}", //
          application, exception);
      throw exception;
    }

    if (environment == null) {
      throw new UsernameNotFoundException("can not find the project by username: " + application);
    }

    return getConfigPassword(environment);
  }
}
