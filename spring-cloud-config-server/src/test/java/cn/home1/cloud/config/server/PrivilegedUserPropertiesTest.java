package cn.home1.cloud.config.server;

import static cn.home1.cloud.config.server.util.Consts.DOT_ENV;

import cn.home1.cloud.config.server.security.PrivilegedUserProperties;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.Charset;
import java.util.Base64;

@Ignore
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigServer.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class PrivilegedUserPropertiesTest {


  private String firstAppName = "my-config-test";
  private String firstAppPassword = "my-config-test";

  private String secondAppName = "rocketmq-console";

  ///////////////

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private PrivilegedUserProperties privilegedUserProperties;

  @Value("${spring.cloud.config.server.prefix:}")
  private String configServerPrefix;


  private HttpEntity<Void> firstAppAuthHttpEntity;
  private HttpEntity<Void> adminAuthHttpEntity;
  private String host;

  private final Charset UTF8 = Charset.forName("UTF-8");


  @Before
  public void before() {
    this.host = "http://localhost:" + port;
    log.info(host);

    this.firstAppAuthHttpEntity = structHttpEntityWithAuthHeaders(this.firstAppName, this.firstAppPassword);

    this.adminAuthHttpEntity = structHttpEntityWithAuthHeaders( //
        this.privilegedUserProperties.getAdminName(),
        this.privilegedUserProperties.getAdminPassword()
    );
  }

  private <T> HttpEntity<T> structHttpEntityWithAuthHeaders(final String name, final String password) {
    HttpHeaders firstAppAuthHeaders = new HttpHeaders();
    String firstAppNamePwdPair = name + ":" + password;
    String base64EncodeAuthStr = new String(Base64.getEncoder().encode(firstAppNamePwdPair.getBytes(UTF8)), UTF8);
    firstAppAuthHeaders.add("Authorization", "Basic " + base64EncodeAuthStr);
    return new HttpEntity<>(firstAppAuthHeaders);
  }

  @Test
  public void anonymityHasNoPrivilegeTest() {
    String requestUrl = host + "/" + firstAppName + "/development" + DOT_ENV;
    log.info(requestUrl);
    ResponseEntity<String> result = this.restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
    log.info(result.getStatusCode().toString());
    Assert.assertTrue(result.getStatusCode() == HttpStatus.UNAUTHORIZED);
  }


  @Test
  public void differentUserHasNoPrivilegeTest() {
    String requestUrl = host + this.configServerPrefix + "/" + secondAppName + "/development" + DOT_ENV;
    log.info(requestUrl);
    ResponseEntity<String> result =
        this.restTemplate.exchange(requestUrl, HttpMethod.GET, firstAppAuthHttpEntity, String.class);
    log.info(result.getStatusCode().toString());
    Assert.assertTrue(result.getStatusCode() == HttpStatus.FORBIDDEN);
  }


  @Test
  public void currentUserHasPrivilegeTest() {
    String requestUrl = host + this.configServerPrefix + "/" + firstAppName + "/development" + DOT_ENV;
    log.info(requestUrl);
    ResponseEntity<String> result =
        this.restTemplate.exchange(requestUrl, HttpMethod.GET, firstAppAuthHttpEntity, String.class);
    log.info(result.getStatusCode().toString());
    Assert.assertTrue(result.getStatusCode() == HttpStatus.OK);
  }

  @Test
  public void adminUserHasPrivilegeTest() throws Exception {
    String requestUrl = host + this.configServerPrefix + "/" + firstAppName + "/development" + DOT_ENV;
    log.info(requestUrl);
    ResponseEntity<String> result =
        this.restTemplate.exchange(requestUrl, HttpMethod.GET, adminAuthHttpEntity, String.class);
    log.info(result.getStatusCode().toString());
    Assert.assertTrue(result.getStatusCode() == HttpStatus.OK);
  }
}
