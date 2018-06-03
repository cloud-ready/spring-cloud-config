package cn.home1.cloud.config.server.monitor;

import org.springframework.cloud.config.monitor.PropertyPathNotification;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * use url instead of filename.
 * see: {@link org.springframework.cloud.config.monitor.PropertyPathEndpoint#guessServiceName(String)}
 * see: {@link org.springframework.cloud.config.monitor.GitlabPropertyPathNotificationExtractor}
 * see: {@link org.springframework.cloud.config.monitor.EnvironmentMonitorAutoConfiguration}
 * Created by zhanghaolun on 16/11/2.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class GitlabRepositoryPathNotificationExtractor extends AbstraceNotificationExtractor {

  @Override
  public PropertyPathNotification extract( //
      final MultiValueMap<String, String> headers, //
      final Map<String, Object> payload //
  ) {
    final PropertyPathNotification result;
    if ("Push Hook".equals(headers.getFirst("X-Gitlab-Event"))) {
      if (payload.get("commits") instanceof Collection) {
        final Set<String> paths = new HashSet<>();

        @SuppressWarnings("unchecked") final Collection<Map<String, Object>> commits = (Collection<Map<String, Object>>) payload.get("commits");

        for (final Map<String, Object> commit : commits) {
          addAllPaths(paths, commit, "added");
          addAllPaths(paths, commit, "removed");
          addAllPaths(paths, commit, "modified");
        }

        result = !paths.isEmpty() ? new PropertyPathNotification(paths.toArray(new String[0])) : null;
      } else {
        result = null;
      }
    } else {
      result = null;
    }
    return result;
  }
}
