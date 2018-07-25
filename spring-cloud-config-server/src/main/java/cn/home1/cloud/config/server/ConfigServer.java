package cn.home1.cloud.config.server;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import cn.home1.cloud.config.server.security.ConfigSecurity;
import cn.home1.cloud.config.server.ssh.DeployKey;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerHealthIndicator;
import org.springframework.cloud.config.server.config.TransportConfiguration.FileBasedSshTransportConfigCallback;
import org.springframework.cloud.config.server.config.TransportConfiguration.PropertiesBasedSshTransportConfigCallback;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.ssh.SshUriProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * see: {@link org.springframework.cloud.config.server.environment.EnvironmentController}
 * see: {@link org.springframework.cloud.config.server.config.ConfigServerMvcConfiguration}
 */
@RestController
@SpringBootApplication
@Slf4j
public class ConfigServer {

  private static final DeployKey DEPLOY_KEY;

  static {
    // set default profiles
    final String profilesFromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
    final String profilesFromProperty = System.getProperty("spring.profiles.active", "");
    if (isBlank(profilesFromEnv) && isBlank(profilesFromProperty)) {
      System.setProperty("spring.profiles.active", "port_nonsecure");
    }

    // set deploy key
    final String deployKeyFromEnv = System.getenv("SPRING_CLOUD_CONFIG_SERVER_GIT_DEPLOYKEY");
    final String deployKeyFromProperty = System.getProperty("spring.cloud.config.server.git.deploy-key", "");
    final String deployKeyLocation;
    if (isNotBlank(deployKeyFromEnv)) {
      deployKeyLocation = deployKeyFromEnv;
    } else if (isNotBlank(deployKeyFromProperty)) {
      deployKeyLocation = deployKeyFromProperty;
    } else {
      deployKeyLocation = System.getProperty("user.home") + "/.ssh/id_rsa";
    }

    final File deployKeyFile = new File(DeployKey.getPrivateKeyPath(deployKeyLocation));
    if (deployKeyFile.exists() && deployKeyFile.canRead()) {
      DEPLOY_KEY = new DeployKey(deployKeyLocation);
      DEPLOY_KEY.setUp(null);
    } else {
      DEPLOY_KEY = null;
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @EnableConfigServer
  //@EnableEurekaClient
  @EnableDiscoveryClient
  protected class ConfigServerConfiguration {

    /**
     * see: {@link org.springframework.cloud.config.server.config.TransportConfiguration}
     */
    @ConditionalOnMissingBean(TransportConfigCallback.class)
    @Bean
    public TransportConfigCallback propertiesBasedSshTransportCallback(final SshUriProperties sshUriProperties) {
      if (ConfigServer.DEPLOY_KEY != null) {
        DEPLOY_KEY.setUp(sshUriProperties);
        return new PropertiesBasedSshTransportConfigCallback(sshUriProperties);
      } else {
        return new FileBasedSshTransportConfigCallback(sshUriProperties);
      }
    }
  }

  @Configuration
  public static class HealthIndicatorConfiguration {

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.config.server.health.enabled", matchIfMissing = true)
    public ConfigServerHealthIndicator configServerHealthIndicator(final EnvironmentRepository repository) {
      return new ConfigServerHealthIndicator(repository);
    }
  }

  @Autowired
  private Environment environment;

  @Autowired
  private ConfigSecurity configSecurity;

  @ResponseBody
  @RequestMapping(path = {"/", "${spring.cloud.config.server.prefix:}/"}, method = GET)
  public String index() {
    return "Visit https://github.com/cloud-ready/spring-cloud-config-server for more info.";
  }

  @ResponseBody
  @RequestMapping(path = "${spring.cloud.config.server.prefix:}/deployKeyPublic", method = GET)
  public ResponseEntity<String> getDeployKeyPublic() {
    final ResponseEntity<String> responseEntity;
    if (ConfigServer.DEPLOY_KEY != null) {
      responseEntity = new ResponseEntity<>(ConfigServer.DEPLOY_KEY.getPublicKey(), HttpStatus.OK);
    } else {
      responseEntity = new ResponseEntity<>("", HttpStatus.NOT_FOUND);
    }
    return responseEntity;
  }

  @SneakyThrows
  @ResponseBody
  @RequestMapping(path = "${spring.cloud.config.server.prefix:}/encryptParentPassword", method = POST,
      consumes = APPLICATION_FORM_URLENCODED_VALUE)
  public String encryptParentPassword(
      @RequestParam("application") final String application,
      @RequestParam("parentApplication") final String parentApplication,
      @RequestParam("parentPassword") final String parentPassword
  ) {
    return this.configSecurity.encryptParentPassword(application, parentApplication, parentPassword);
  }

  public static void main(final String... args) {
    SpringApplication.run(ConfigServer.class, args);
  }
}
