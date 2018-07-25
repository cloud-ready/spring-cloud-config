package cn.home1.cloud.config.server.monitor;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.monitor.PropertyPathNotificationExtractor;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2017/1/17 yanzhang153
 */
@Slf4j
public abstract class AbstraceNotificationExtractor implements PropertyPathNotificationExtractor {

  private static final Pattern PATH_PATTERN = Pattern.compile("^(https?)://(.+)/(.*)/commit/[a-zA-Z0-9]{7,40}");
  private static final String ALL_APPLICATION_STR = "application";

  @Value("${spring.cloud.config.server.common-config.prefix:home1-oss-common}")
  private String commonConfigPrefix;

  @Value("${spring.cloud.config.server.common-config.suffix:-common}")
  private String commonConfigSuffix;

  protected void addAllPaths(final Set<String> paths, final Map<String, Object> commit, final String name) {
    if (paths.contains(ALL_APPLICATION_STR)) {
      return;
    }
    // example:
    // http://gitlab/configserver/oss-todomvc-app-config/commit/929f67f2b38a6269e7ad63f606c9d89a7d8eb79f
    final String url = (String) commit.get("url");
    log.debug("config server monitor url:{}", url);
    if (StringUtils.isBlank(url)) {
      return;
    }
    final Matcher matcher = PATH_PATTERN.matcher(url);
    if (matcher.matches()) {
      final String repository = matcher.group(3);
      log.debug("will add application pattern, repository name :{}, current result:{}", repository, paths);
      final int lastIndex = repository.lastIndexOf('-');
      if (lastIndex <= 0) {
        log.info("the suffix of repository name is not ends with -xxx (-config), ignored.");
        return;
      }
      final String applicationName = repository.substring(0, lastIndex);
      String pattern = null;
      if (applicationName.startsWith(commonConfigPrefix)) {
        pattern = ALL_APPLICATION_STR;// to all application
      } else {
        pattern = applicationName;
        final int idx = applicationName.indexOf('-', 1);
        if (idx > 0) {
          pattern = applicationName.substring(0, idx);
        }
        pattern = pattern + "*";
      }
      if (paths.add(pattern)) {
        log.info("add refresh pattern {}, applicationName:{}", pattern, applicationName);
      }
    }
  }
}
