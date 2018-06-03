package ai.advance.cloud.config.server.util;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = PRIVATE)
public abstract class EnvironmentUtils {

  public static String getEnvProperty(final Environment environment, final String name) {
    final String result;
    if (environment == null || name == null) {
      result = null;
    } else {
      final List<PropertySource> propertySources = environment.getPropertySources();
      result = propertySources != null ? //
          propertySources.stream()
              .filter(propertySource -> getProperty(propertySource, name) != null)
              .map(propertySource -> getProperty(propertySource, name))
              .findFirst()
              .orElse(null) :
          null;
    }
    return result;
  }

  private static String getProperty(final PropertySource propertySource, final String key) {
    final String result;

    final Map<?, ?> source = propertySource.getSource();
    if (source == null) {
      result = null;
    } else {
      final Object valueObj = source.get(key);
      result = valueObj == null ? null : "" + valueObj;
    }

    return result;
  }

  public static String getApplicationName(final Environment environment) {
    return getEnvProperty(environment, "spring.application.name");
  }

  public static String getConfigPassword(final Environment environment) {
    return getEnvProperty(environment, "spring.cloud.config.password");
  }

  public static String getParentConfigPassword(final Environment environment) {
    return getEnvProperty(environment, "spring.cloud.config.parent.password");
  }

  private static final Pattern VARIABLE_SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

  public static String getParentApplication(final Environment environment) {
    if (environment == null) {
      return null;
    }

    final String evnPropertyValue = getEnvProperty(environment, "spring.cloud.config.parent.application");
    if (evnPropertyValue == null) {
      return null;
    }

    final Matcher matcher = VARIABLE_SUBSTITUTION_PATTERN.matcher(evnPropertyValue);

    String result = evnPropertyValue;
    while (matcher.find()) {
      final String name = matcher.group(1);
      final String value = System.getProperty(name);
      result = result.replaceAll("\\$\\{" + name + "\\}", value == null ? "" : value);
    }

    return result;
  }

  public static String getParentLabel(final Environment environment, final String label) {
    final String evnPropertyValue = getEnvProperty(environment, "spring.cloud.config.parent.label");
    return evnPropertyValue != null && label != null ? evnPropertyValue.replace("{label}", label) : label;
  }
}
