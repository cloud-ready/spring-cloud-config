package cn.home1.cloud.config.server;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.util.FileCopyUtils.copyToString;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import cn.home1.cloud.config.server.security.ConfigSecurity;
import cn.home1.cloud.config.server.ssh.CustomJschConfigSessionFactory;
import cn.home1.cloud.config.server.util.ResourceUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerHealthIndicator;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;

@RestController
@SpringBootApplication
@Slf4j
public class ConfigServer {

  @Configuration
  @EnableAutoConfiguration
  @EnableConfigServer
  //@EnableEurekaClient
  @EnableDiscoveryClient
  protected class ConfigServerConfiguration {

    @Bean
    public ConfigSecurity configSecurity() {
      return new ConfigSecurity();
    }
  }

  private static final String DATA_DIRECTORY = System.getProperty("user.home") + "/.data/config-server";

  @Autowired
  private Environment environment;

  @Value("${spring.cloud.config.server.git.deploy-key.public}")
  private String deployKeyPublic;

  @Autowired
  private ConfigSecurity configSecurity;

  @ResponseBody
  @RequestMapping(path = {"/", "${spring.cloud.config.server.prefix:}/"}, method = GET)
  public String index() {
    return "Visit https://github.com/cloud-ready/spring-cloud-config-server for more info.";
  }

  @SneakyThrows
  @ResponseBody
  @RequestMapping(path = "${spring.cloud.config.server.prefix:}/deployKeyPublic", method = GET)
  public String getDeployKeyPublic() {
    final String path = ResourceUtils.findResourceFile(this.deployKeyPublic, ConfigServer.DATA_DIRECTORY);
    return copyToString(new FileReader(new File(path)));
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

  @Configuration
  public static class HealthIndicatorConfiguration {

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.config.server.health.enabled", matchIfMissing = true)
    public ConfigServerHealthIndicator configServerHealthIndicator(final EnvironmentRepository repository) {
      return new ConfigServerHealthIndicator(repository);
    }
  }

  @Configuration
  @Order(HIGHEST_PRECEDENCE)
  public static class DeployKeyConfiguration {

    @Value("${spring.cloud.config.server.git.deploy-key.private}")
    public void setDeployKeyPrivate(final String location) {
      final String path = ResourceUtils.findResourceFile(location, ConfigServer.DATA_DIRECTORY);
      SshSessionFactory.setInstance(new CustomJschConfigSessionFactory(path));
    }
  }

  public static void main(final String... args) {
    SpringApplication.run(ConfigServer.class, args);
  }
}
