package ai.advance.cloud.config.server.security;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

import javax.annotation.PostConstruct;

@Getter
@Setter
@Slf4j
public class ConfigServerSecurityProperties {

  @Value("${security.user.name:admin}")
  private String adminName;

  @Value("${security.user.password:}")
  private String adminPassword;

  @Value("${security.hook.name:hook}")
  private String hookName;

  @Value("${security.hook.password:hook_pass}")
  private String hookPassword;

  @PostConstruct
  private void init() {
    if (isBlank(this.adminPassword)) {
      this.adminPassword = UUID.randomUUID().toString();
      log.info("auto generated admin password, username:{}, password:{}", this.adminName, this.adminPassword);
    }

    if (isBlank(this.hookPassword)) {
      this.hookPassword = UUID.randomUUID().toString();
      log.info("auto generated hook password, username:{}, password:{}", this.hookName, this.hookPassword);
    }
  }
}
