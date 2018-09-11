package cn.home1.cloud.config.server.ssh;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.FileCopyUtils.copyToString;

import cn.home1.cloud.config.server.util.Consts;
import cn.home1.cloud.config.server.util.ResourceUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;

import java.io.File;
import java.io.FileReader;

@Slf4j
public class DeployKey {

    private final String privateKeyLocation;

    public DeployKey(final String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public static String getPrivateKeyPath(final String privateKeyLocation) {
        return ResourceUtils.findResourceFile(privateKeyLocation, Consts.DATA_DIRECTORY);
    }

    public void setUp(final MultipleJGitEnvironmentProperties sshUriProperties) {
        final String privateKey = this.getPrivateKey();

        // setup ssh client
        if (sshUriProperties == null) {
            // setup SshSessionFactory as early as possible, requires @Order(HIGHEST_PRECEDENCE) on Configuration
            //SshSessionFactory.setInstance(new CustomJschConfigSessionFactory(file));

            System.setProperty("spring.cloud.config.server.git.private-key", privateKey);
            System.setProperty("spring.cloud.config.server.git.ignore-local-ssh-settings", "true");
        } else {
            log.info("ssh uri properties privateKey is not blank: {}", isNotBlank(sshUriProperties.getPrivateKey()));
            log.info("ssh uri properties is ignore local ssh settings: {}", sshUriProperties.isIgnoreLocalSshSettings());
            log.info("ssh uri properties is strict host key checking: {}", sshUriProperties.isStrictHostKeyChecking());

            sshUriProperties.setPrivateKey(privateKey);
            sshUriProperties.setIgnoreLocalSshSettings(true);
        }
    }

    @SneakyThrows
    public String getPrivateKey() {
        return copyToString(new FileReader(new File(this.getPrivateKeyPath())));
    }

    public String getPrivateKeyPath() {
        return getPrivateKeyPath(this.privateKeyLocation);
    }

    @SneakyThrows
    public String getPublicKey() {
        return copyToString(new FileReader(new File(this.getPublicKeyPath())));
    }

    public String getPublicKeyPath() {
        return this.getPrivateKeyPath() + ".pub";
    }
}
