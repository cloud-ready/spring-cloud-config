package cn.home1.cloud.config.server.security;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

@Getter
@Setter
@Slf4j
public class PrivilegedUserProperties {

  @Autowired
  @Getter(value = PRIVATE)
  private SecurityProperties securityProperties;

  @Value("${security.hook.name:hook}")
  private String hookName;

  @Value("${security.hook.password:hook_pass}")
  private String hookPassword;

  public String getAdminName() {
    return this.securityProperties.getUser().getName();
  }

  public String getAdminPassword() {
    return this.securityProperties.getUser().getPassword();
  }

  public List<String> getAdminRoles() {
    return this.securityProperties.getUser().getRole();
  }

  @PostConstruct
  private void init() {
    if (isBlank(this.getAdminPassword())) {
      this.securityProperties.getUser().setPassword(UUID.randomUUID().toString());
      log.info("auto generated admin password, username:{}, password:{}", this.getAdminName(), this.getAdminPassword());
    }

    if (isBlank(this.hookPassword)) {
      this.hookPassword = UUID.randomUUID().toString();
      log.info("auto generated hook password, username:{}, password:{}", this.hookName, this.hookPassword);
    }
  }
}
