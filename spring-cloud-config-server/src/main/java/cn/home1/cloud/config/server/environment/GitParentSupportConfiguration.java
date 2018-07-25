package cn.home1.cloud.config.server.environment;

import cn.home1.cloud.config.server.security.ConfigSecurity;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@ConditionalOnProperty(prefix = "spring.cloud.config.server.git", name = "parent-support-enabled", havingValue = "true")
@ConditionalOnMissingBean(EnvironmentRepository.class)
@Configuration
public class GitParentSupportConfiguration {

  @Value("${spring.cloud.config.server.default-label:master}")
  private String defaultLabel;

  @Value("${spring.cloud.config.server.git.delete-untracked-branches:true}")
  private Boolean deleteUntrackedBranches;

  @Autowired
  private ConfigurableEnvironment environment;

  @Value("${spring.cloud.config.server.git.force-pull:true}")
  private Boolean forcePull;

  @Value("${spring.cloud.config.server.git.strict-host-key-checking:false}")
  private Boolean strictHostKeyChecking;

  @Value("${spring.cloud.config.server.git.timeout:30}")
  private Integer timeout;

  @Autowired(required = false)
  private TransportConfigCallback transportConfigCallback;

  @Bean
  public ConfigSecurity configSecurity() {
    return new ConfigSecurity();
  }

  @Bean
  @ConditionalOnMissingBean(EnvironmentRepository.class)
  public EnvironmentRepository environmentRepository() {
    final GitParentSupportMultipleJGitEnvironmentRepository repository =
        new GitParentSupportMultipleJGitEnvironmentRepository(this.environment);

    repository.setConfigSecurity(this.configSecurity());

    repository.setDefaultLabel(this.defaultLabel);
    repository.setDeleteUntrackedBranches(this.deleteUntrackedBranches);
    repository.setForcePull(this.forcePull);
    repository.setStrictHostKeyChecking(this.strictHostKeyChecking);
    repository.setTimeout(this.timeout);

    repository.setTransportConfigCallback(this.transportConfigCallback);

    return repository;
  }
}
