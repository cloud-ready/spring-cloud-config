package cn.home1.cloud.config.server.security;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties.KeyStore;
import org.springframework.cloud.bootstrap.encrypt.RsaProperties;
import org.springframework.cloud.config.server.encryption.KeyStoreTextEncryptorLocator;
import org.springframework.cloud.config.server.encryption.TextEncryptorLocator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaAlgorithm;

@Slf4j
public class ConfigSecurityTest {

    private ConfigSecurity configSecurity;
    private TextEncryptor encryptor;

    @Before
    public void setUp() {
        final TextEncryptorLocator encryptorLocator = this.textEncryptorLocator();
        final TextEncryptor encryptor = encryptorLocator.locate(ImmutableMap.of());

        final ConfigSecurity configSecurity = new ConfigSecurity();
        configSecurity.setEncryptor(encryptor);
        configSecurity.setHmacSecret("secret");
        configSecurity.setSecurityEnabled(TRUE);
        configSecurity.init();

        this.configSecurity = configSecurity;
        this.encryptor = encryptor;
    }

    @Test
    public void testVerifyParentPasswordToken() {
        final String application = "application";
        final String parentApplication = "parent-application";
        final String parentPassword = "parent-application_pass";
        final String token = this.configSecurity.encryptParentPassword(application, parentApplication, parentPassword);

        log.info("application: {}, parent-application: {}, parent-application_pass: {}, token: {}", //
            application, parentApplication, parentPassword, token);

        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, "", token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, null, token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword("", parentApplication, token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(null, parentApplication, token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application + "x", parentApplication, token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, parentApplication + "x", token, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, parentApplication, token, parentPassword + "x"));
    }

    @Test
    public void testVerifyNonTokenParentPassword() {
        final String application = "application";
        final String parentApplication = "parent-application";
        final String parentPassword = "parent-application_pass";

        final String encryptedParentPassword = "{cipher}" + this.encryptor.encrypt(parentPassword);

        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, parentPassword, parentPassword));
        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, parentPassword, encryptedParentPassword));
        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, encryptedParentPassword, parentPassword));

        // plain text parent password does not contain application info, can not verify by application.
        assertTrue(this.configSecurity.verifyParentPassword(application, "", parentPassword, parentPassword));
        assertTrue(this.configSecurity.verifyParentPassword(application, null, parentPassword, parentPassword));

        assertFalse(this.configSecurity.verifyParentPassword("", parentPassword, parentPassword, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(null, parentPassword, parentPassword, parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, parentApplication, parentPassword + "x", parentPassword));
        assertFalse(this.configSecurity.verifyParentPassword(application, parentApplication, parentPassword, parentPassword + "x"));
    }

    @Test
    public void testVerifyEmptyParentPassword() {
        final String application = "application";
        final String parentApplication = "parent-application";

        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, "", null));
        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, null, ""));
        assertTrue(this.configSecurity.verifyParentPassword(application, parentApplication, null, null));
        assertFalse(this.configSecurity.verifyParentPassword(application, "", null, null));
        assertFalse(this.configSecurity.verifyParentPassword(application, null, null, null));
        assertFalse(this.configSecurity.verifyParentPassword("", parentApplication, null, null));
        assertFalse(this.configSecurity.verifyParentPassword(null, parentApplication, null, null));
    }

    /**
     * see: {@link org.springframework.cloud.config.server.config.EncryptionAutoConfiguration}
     */
    TextEncryptorLocator textEncryptorLocator() {
        final KeyStore keyStore = new KeyStore();
        keyStore.setAlias("key_alias");
        keyStore.setSecret("key_pass");
        keyStore.setLocation(new ClassPathResource("default-keystore.jks"));
        keyStore.setPassword("store_pass");

        final RsaProperties rsaProperties = new RsaProperties();

        KeyStoreTextEncryptorLocator locator = new KeyStoreTextEncryptorLocator(
            new KeyStoreKeyFactory(keyStore.getLocation(), keyStore.getPassword().toCharArray()),
            keyStore.getSecret(), keyStore.getAlias());
        RsaAlgorithm algorithm = rsaProperties.getAlgorithm();
        locator.setRsaAlgorithm(algorithm);
        locator.setSalt(rsaProperties.getSalt());
        locator.setStrong(rsaProperties.isStrong());
        return locator;
    }
}
