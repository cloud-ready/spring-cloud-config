package ai.advance.cloud.config.server;

import static ai.advance.cloud.config.server.util.ResourceUtils.findResourceFile;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.util.FileCopyUtils.copyToString;

import lombok.SneakyThrows;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerHealthIndicator;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;

import ai.advance.cloud.config.server.ssh.CustomJschConfigSessionFactory;

@RestController
@EnableConfigServer
@EnableEurekaClient
@SpringBootApplication
public class ConfigServer {

  private static final String DATA_DIRECTORY = System.getProperty("user.home") + "/.data/config-server";

  @Autowired
  private Environment environment;

  @Value("${spring.cloud.config.server.git.deploy-key.public}")
  private String deployKeyPublic;

  @ResponseBody
  @RequestMapping(path = {"/", "${spring.cloud.config.server.prefix:}/"}, method = RequestMethod.GET)
  public String index() {
    return "Visit https://github.com/cloud-ready/spring-cloud-config-server for more info.";
  }

  @SneakyThrows
  @ResponseBody
  @RequestMapping(path = "${spring.cloud.config.server.prefix:}/deployKeyPublic", method = RequestMethod.GET)
  public String getDeployKeyPublic() {
    final String path = findResourceFile(this.deployKeyPublic, ConfigServer.DATA_DIRECTORY);
    return copyToString(new FileReader(new File(path)));
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
      final String path = findResourceFile(location, ConfigServer.DATA_DIRECTORY);
      SshSessionFactory.setInstance(new CustomJschConfigSessionFactory(path));
    }
  }

  public static void main(final String... args) {
    SpringApplication.run(ConfigServer.class, args);
  }
}
