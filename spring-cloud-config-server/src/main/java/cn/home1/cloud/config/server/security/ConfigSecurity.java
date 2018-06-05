package cn.home1.cloud.config.server.security;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.joda.time.DateTime.now;

import com.google.common.collect.ImmutableMap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.server.encryption.TextEncryptorLocator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

@Setter
@Slf4j
public class ConfigSecurity {

  private static final int BCRYPT_STRENGTH = -1;

  private static final Pattern CONCAT_PATTERN = Pattern.compile(":");

  private static final String TOKEN_CLAIM = "encrypted";
  private static final int TOKEN_EXPIRE_DAYS = 365 * 5;
  private static final String TOKEN_ISSUER = "config-server";

  private final PasswordEncoder passwordEncoder;

  private TextEncryptor encryptor;

  private Algorithm hmacAlgorithm;

  @Value("${spring.cloud.config.encrypt.hmac-secret:secret}")
  private String hmacSecret;

  private JWTVerifier hmacVerifier;

  @Value("${security.basic.enabled:true}")
  private Boolean securityEnabled;

  public ConfigSecurity() {
    this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  }


  /**
   * By default, this is a {@link org.springframework.cloud.config.server.encryption.KeyStoreTextEncryptorLocator} instance.
   */
  @Autowired
  public void setEncryptorLocator(final TextEncryptorLocator encryptorLocator) {
    this.encryptor = encryptorLocator.locate(ImmutableMap.of());
  }

  @PostConstruct
  @SneakyThrows
  public void init() {
    this.hmacAlgorithm = Algorithm.HMAC256(this.hmacSecret);
    this.hmacVerifier = JWT.require(this.hmacAlgorithm)
        .withIssuer(TOKEN_ISSUER)
        .build(); //Reusable verifier instance
  }

  /**
   * Generate a password (token) valid only for given application to access a given parent
   *
   * @param application       from child config
   * @param parentApplication from child config
   * @param parentPassword    from child config
   * @return token valid for application only
   */
  public String encryptParentPassword(final String application, final String parentApplication, final String parentPassword) {
    //if (!this.securityEnabled) {
    //  return "";
    //}

    checkArgument(isNotBlank(application), "blank application");
    checkArgument(isNotBlank(application), "blank parentApplication");
    checkArgument(isNotBlank(application), "blank parentPassword");

    // digest/hash
    // random string (length 16)
    final String randomString = random(16, 0, 0, true, true, null, new SecureRandom());
    final String encodedApplication = this.passwordEncoder.encode(application);
    final String encodedParentApplication = this.passwordEncoder.encode(parentApplication);
    final String encodedParentPassword = this.passwordEncoder.encode(parentPassword);

    // concat
    final String plainText = randomString + ":" + encodedApplication + ":" + encodedParentApplication + ":" + encodedParentPassword;

    // encrypt
    final String encrypted = this.encryptor.encrypt(plainText);

    // sign
    final String token = JWT.create()
        .withIssuer(TOKEN_ISSUER)
        .withClaim(TOKEN_CLAIM, encrypted)
        .withExpiresAt(now().plusDays(TOKEN_EXPIRE_DAYS).toDate())
        .sign(this.hmacAlgorithm);

    log.info("Granted parent ({}) config access for application '{}', token: '{}'.", parentApplication, application, token);

    return token;
  }

  /**
   * Verify privilege
   *
   * @param application            from context (URL path)
   * @param parentApplication      from child config
   * @param token                  from child config
   * @param expectedParentPassword from parent config (may need to decrypt before verify)
   * @return whether token, application in token and password in token valid
   */
  public Boolean verifyParentPassword(final String application, final String parentApplication, final String token, final String expectedParentPassword) {
    final Boolean result;
    if (!this.securityEnabled) {
      result = TRUE;
    } else {
      checkArgument(isNotBlank(application), "blank application");
      checkArgument(isNotBlank(token), "blank parentPassword or token");

      final String rawParentPassword = this.decryptProperty(expectedParentPassword);
      if (token.startsWith("{token}")) {
        final String rawToken = token.replace("{token}", "");
        // verify signature
        final String encrypted = this.hmacVerifier.verify(rawToken).getClaim(TOKEN_CLAIM).asString();

        // decrypt
        final String plainText = this.encryptor.decrypt(encrypted);

        // split
        final Iterator<String> parts = CONCAT_PATTERN.splitAsStream(plainText).iterator();
        final String random = parts.next();
        final String encodedApplication = parts.next();
        final String encodedParentApplication = parts.next();
        final String encodedParentPassword = parts.next();

        checkArgument(this.passwordEncoder.matches(application, encodedApplication), "invalid application");
        checkArgument(this.passwordEncoder.matches(parentApplication, encodedParentApplication), "invalid parentApplication");
        checkArgument(this.passwordEncoder.matches(rawParentPassword, encodedParentPassword), "invalid parentApplication");

        result = TRUE;
      } else {
        final String rawToken = this.decryptProperty(token);
        result = rawParentPassword.equals(rawToken);
      }
    }
    return result;
  }

  String decryptProperty(final String value) {
    return decryptProperty(value, this.encryptor);
  }

  static String decryptProperty(final String value, final TextEncryptor encryptor) {
    final String result;
    if (isNotBlank(value) && value.startsWith("{cipher}")) {
      final String base64 = value.replaceAll("\\{[^}]+\\}", "");
      result = encryptor.decrypt(base64);
    } else {
      result = value;
    }
    return result;
  }
}
