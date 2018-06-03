package ai.advance.cloud.config.server.environment;

import static ai.advance.cloud.config.server.util.EnvironmentUtils.getApplicationName;
import static ai.advance.cloud.config.server.util.EnvironmentUtils.getConfigPassword;
import static ai.advance.cloud.config.server.util.EnvironmentUtils.getParentApplication;
import static ai.advance.cloud.config.server.util.EnvironmentUtils.getParentConfigPassword;
import static ai.advance.cloud.config.server.util.EnvironmentUtils.getParentLabel;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Set;

import ai.advance.cloud.config.server.util.Consts;

@ConfigurationProperties("spring.cloud.config.server.git")
@Slf4j
public class GitParentSupportMultipleJGitEnvironmentRepository extends MultipleJGitEnvironmentRepository {

  public GitParentSupportMultipleJGitEnvironmentRepository(final ConfigurableEnvironment environment) {
    super(environment);
  }

  @Override
  public Environment findOne(final String application, final String profile, final String label) {
    final Environment currentApplicationEnv = super.findOne(application, profile, label);

    final Set<String> history = newLinkedHashSet();
    history.add(getApplicationName(currentApplicationEnv));

    for (Environment parentEnv, applicationEnv = currentApplicationEnv; //
         (parentEnv = getParentEnvironment(applicationEnv, label)) != null; //
         applicationEnv = parentEnv) {

      final String parentApplication = getParentApplication(applicationEnv);

      final String expectedPassword = getConfigPassword(parentEnv);
      if (isNotBlank(expectedPassword)) {
        final String actualPassword = getParentConfigPassword(applicationEnv);
        if (!expectedPassword.equals(actualPassword)) {
          throw new BadCredentialsException(
              "access parent password for '" + parentApplication + "' is empty or incorrect!");
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
    final String parentApplication = getParentApplication(environment);
    if (parentApplication == null || isBlank(parentApplication)) {
      return null;
    }
    final String parentLabel = getParentLabel(environment, label);

    Environment result = null;

    String[] profiles = environment.getProfiles();
    if (profiles == null || profiles.length == 0) {
      profiles = new String[]{Consts.PROFILE_NOT_EXIST};
    }

    for (final String profile : profiles) {
      Environment found;
      try {
        found = super.findOne(parentApplication, profile, parentLabel);
      } catch (Exception exception) {
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
