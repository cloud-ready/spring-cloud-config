package ai.advance.cloud.config.server.security;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Determine whether let user go further by login user and repository name in URL.
 */
@Slf4j
public class ApplicationConfigSecurity {

  @Autowired
  @Setter
  private ConfigServerSecurityProperties configServerSecurityProperties;

  /**
   * Allow admin user, allow user that name matches repository name
   *
   * @param pathRepositoryName repository name extracted from path
   * @return allow access
   */
  public boolean checkAuthenticationName(final String pathRepositoryName) {
    final boolean result;

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String authenticationName = authentication != null ? authentication.getName() : null;
    if (authenticationName != null) {
      if (this.configServerSecurityProperties.getAdminName().equals(authenticationName) && authentication.isAuthenticated()) {
        result = true;
      } else {
        result = pathRepositoryName != null && pathRepositoryName.equals(authenticationName);
      }
    } else {
      result = false;
    }

    log.debug("'{}' {} privilege to access '{}'", authenticationName, result ? "has" : "has no", pathRepositoryName);

    return result;
  }
}
