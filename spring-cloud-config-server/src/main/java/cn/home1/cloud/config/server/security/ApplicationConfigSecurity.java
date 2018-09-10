package cn.home1.cloud.config.server.security;

import static cn.home1.cloud.config.server.util.Consts.DOT_ENV;
import static cn.home1.cloud.config.server.util.Consts.PRIVILEGE_ENV_PROFILE_;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Determine whether let user go further by login user and repository name in URL.
 */
@Slf4j
public class ApplicationConfigSecurity {

    private static final Pattern PATTERN_COMMA = Pattern.compile(",");

    @Autowired
    @Setter
    private PrivilegedUserProperties privilegedUserProperties;

    /**
     * Allow admin user, allow user that name matches repository name
     *
     * @param pathApplication application (repository) name extracted from path
     * @param pathProfiles    profiles extracted from path
     * @return allow access
     */
    public boolean checkAuthentication(final String pathApplication, final String pathProfiles) {
        final boolean result;

        final Collection<String> envProfiles = PATTERN_COMMA.splitAsStream(pathProfiles)
            .filter(profile -> profile.endsWith(DOT_ENV))
            .collect(toSet());

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String authName = auth != null ? auth.getName() : null;

        if (envProfiles.size() <= 1) {
            if (isNotEmpty(authName) && auth.isAuthenticated()) {
                final String adminName = this.privilegedUserProperties.getAdminName();
                if (adminName.equals(authName)) {
                    result = true;
                } else {
                    final boolean applicationMatch = pathApplication != null && pathApplication.equals(authName);
                    final boolean isRequestValid = this.isRequestValid(auth, envProfiles);
                    result = applicationMatch && isRequestValid;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
            log.info("Illegal to access multiple environment profiles ({}) in one request", envProfiles);
        }

        log.debug("'{}' {} privilege to access '{}'", authName, result ? "has" : "has no", pathApplication);
        return result;
    }

    boolean isRequestValid(final Authentication auth, final Collection<String> envProfiles) {
        final boolean result;

        final Optional<String> privilege = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith(PRIVILEGE_ENV_PROFILE_))
            .map(p -> p.replace(PRIVILEGE_ENV_PROFILE_, ""))
            .findFirst(); // one environment profile privilege at most, may be none.

        if (privilege.isPresent() && privilege.get().equals("*")) {
            result = true; // wildcard privilege can access any environment profile
        } else {
            result = privilege
                .map(p -> envProfiles.isEmpty() || envProfiles.contains(p)) // access no or matched environment profile only
                .orElseGet(envProfiles::isEmpty); // can not access any environment profile
        }

        return result;
    }
}
