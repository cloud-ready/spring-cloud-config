package cn.home1.cloud.config.server.environment;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import cn.home1.cloud.config.server.security.ConfigSecurity;
import cn.home1.cloud.config.server.util.EnvironmentUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Set;

@ConfigurationProperties("spring.cloud.config.server.git")
@Slf4j
public class GitParentSupportMultipleJGitEnvironmentRepository extends MultipleJGitEnvironmentRepository {

    @Setter
    private ConfigSecurity configSecurity;

    public GitParentSupportMultipleJGitEnvironmentRepository(ConfigurableEnvironment environment, final MultipleJGitEnvironmentProperties properties) {
        super(environment, properties);
    }

    @Override
    public Environment findOne(final String application, final String profile, final String label) {
        final Environment currentApplicationEnv = super.findOne(application, profile, label);

        // load parents
        final Set<String> history = newLinkedHashSet();
        history.add(EnvironmentUtils.getApplicationName(currentApplicationEnv));

        for (Environment parentEnv, applicationEnv = currentApplicationEnv; //
             (parentEnv = getParentEnvironment(applicationEnv, label)) != null; //
             applicationEnv = parentEnv) {

            final String parentApplication = EnvironmentUtils.getParentApplication(applicationEnv);

            final String expectedParentPassword = EnvironmentUtils.getConfigPassword(parentEnv);
            if (isNotBlank(expectedParentPassword)) {
                final String token = EnvironmentUtils.getParentConfigPassword(applicationEnv);
                final Boolean hasParentPrivilege = this.configSecurity.verifyParentPassword(application, parentApplication, token, expectedParentPassword);
                if (!hasParentPrivilege) {
                    throw new BadCredentialsException("Invalid access to parent config '" + parentApplication + "'.");
                }
            }

            if (history.contains(parentApplication)) {
                log.warn("circular reference detected! ignore existed parent. application:{}, circular parentApplication:{}",
                    application, parentApplication);
                break;
            } else {
                history.add(parentApplication);
            }

            // application config first.
            // PropertySources[n] will override (cover) the value in PropertySources[n+1] under same key.
            currentApplicationEnv.getPropertySources().addAll(parentEnv.getPropertySources());
        }

        return currentApplicationEnv;
    }


    private Environment getParentEnvironment(final Environment environment, final String label) {
        final String parentApplication = EnvironmentUtils.getParentApplication(environment);
        if (parentApplication == null || isBlank(parentApplication)) {
            return null;
        }
        final String parentLabel = EnvironmentUtils.getParentLabel(environment, label);

        Environment result = null;

        String[] profiles = environment.getProfiles();
        if (profiles == null || profiles.length == 0) {
            profiles = new String[]{"default"};
        }

        for (final String profile : profiles) {
            Environment found;
            try {
                found = super.findOne(parentApplication, profile, parentLabel);
            } catch (final Exception exception) {
                log.warn("error fetch parent exception! parentApplication:{}, profile:{}, parentLabel:{}",
                    parentApplication, profile, parentLabel, exception);
                throw exception;
            }

            if (result == null) {
                result = found;
            } else {
                result.getPropertySources().addAll(found.getPropertySources());
            }
        }
        return result;
    }
}
