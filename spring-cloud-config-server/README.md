#spring-cloud-config-server
spring-cloud-config-server


[![Build Status](https://travis-ci.org/cloud-ready/spring-cloud-config.svg?branch=master)](https://travis-ci.org/cloud-ready/spring-cloud-config)


## Local demostration

1. Create a local docker network
`docker network create --driver=bridge --ipv6 --ipam-driver=default --subnet=172.16.238.0/24 --subnet=2001:3984:3989::/64 local-network`

2. Start up
`docker-compose up`

3. Get application config info (HTTP access)
> default admin username/password is `admin/admin_pass`

config repositories for demo (default) instance of config-server:
- [health-check](https://github.com/spring-cloud-config-server/health-check-config)
> repo for config-server's health check
- [demo-app](https://github.com/spring-cloud-config-server/demo-app-config)
> application config repo, default branch (label) is develop
- [demo-app-common](https://github.com/spring-cloud-config-server/demo-app-common-config)
> parent config repo of demo-app, default branch (label) is develop

config-server supports following url patterns ({profiles} is a comma splitted list, {label} is git ref name)

- /{application}/{profiles}[/{label}]
```bash
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app/production.env,ut.env/develop
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app/production.env,ut.env
```

/{application}-{profiles}.json
/{label}/{application}-{profiles}.json
```bash
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app-production.env,ut.env.json
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/develop/demo-app-production.env,ut.env.json
```

/{application}-{profiles}.properties
/{label}/{application}-{profiles}.properties
```bash
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app-production.env,ut.env.properties
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/develop/demo-app-production.env,ut.env.properties
```

/{application}-{profiles}.yml
/{application}-{profiles}.yaml
/{label}/{application}-{profiles}.yml
/{label}/{application}-{profiles}.yaml
```bash
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app-production.env,ut.env.yml
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/demo-app-production.env,ut.env.yaml

curl -s -X GET -u admin:admin_pass http://config-server:8888/config/develop/demo-app-production.env,ut.env.yml
curl -s -X GET -u admin:admin_pass http://config-server:8888/config/develop/demo-app-production.env,ut.env.yaml
```


4. Encrypt/Decrypt string

4.1. Encrypt
Any one can access `/encrypt` endpoint
```bash
curl -s -X POST http://config-server:8888/config/encrypt -H 'Accept: application/json' -d mysecret
```

4.2. Decrypt
Only admin can access `/decrypt` decrypt
```bash
curl -s -X POST -u admin:admin_pass http://config-server:8888/config/decrypt -H 'Accept: application/json' -d $(curl -s -X POST http://config-server:8888/config/encrypt -H 'Accept: application/json' -d mysecret)
```

5. Runtime refresh (update) application's config

5.1. git push -> call application instances's `/refresh` manually

Call application's `/refresh` endpoint after push new config files onto git service
Only beans that annotated with `@RefreshScope` annotation will be refreshed.
```bash
# contents in '[]' is ignorable, depends on application instance's config
curl -s -X GET [-u user:password] http[s]://host[:port][/server.context-path][/management.context-path]/refresh
```

5.2. git push hook on git service -> config-server's `monitor` endpoint -> cloud-bus (MQ) -> application instances automatically

5.2.1. Setup hook for gitlab repositories
Enable `Push events`, turn `SSL verification` off.
URL like `http[s]://hook_user_name:hook_user_password@config-server-host:8888/config/monitor`.

Enable hook data extractor in config-server's application.yml.
```yaml
spring.cloud:
  config:
    server:
      git:
      monitor:
        gitlabrepo.enabled: ${SPRING_CLOUD_CONFIG_SERVER_MONITOR_GITLABREPO_ENABLED:false}
```

TODO doc


6. Other endpoints
- http://config-server:8888/

- http://config-server:8888/config/deployKeyPublic

- http://config-server:8888/manage/info
- http://config-server:8888/manage/health


## Extended features
This config-server extends spring-cloud's default config-server.

- Custom Feature 1. Custom parent git config repository
> Users can ref a parent config repository in their own config file.

Note:
+ It is recommended to use a encrypted `spring.cloud.config.parent.password` in config file.
+ Do not use a plain text parent password or password encrypted by  `/encrypt` endpoint.
> Anyone who copied it will get privilege to access parent config repository.
+ You can use different `spring.cloud.config.parent.password` in auth-label-develop, auth-profile-staging.env, auth-profile-production.env configs to protect sensible properties.
> If you put password in different environment profiles, you need to provide the application (user) name and environment profile name (where the password stored), join them with a '@' as username.
> i.e. curl -X GET -u application@staging.env http://config-server:8888/config/application/staging.env
+ Use `/encryptParentPassword` to generate a encrypted and signed parent password (token), only admin can do this.
```bash
curl -s -X POST http://config-server:8888/config/encryptParentPassword \
-H "Content-Type: application/x-www-form-urlencoded" \
-H "Accept: application/json" \
-u admin:admin_pass \
-d "application=demo-app&parentApplication=demo-app-common&parentPassword=demo-app-common_pass"
```

user's application.yml example:
```yaml
spring.cloud.config.parent:
  application: demo-app-common
  label: '{label}' # must quote, if not the 'spring.cloud.config.parent.label: {label}' will becomes 'spring.cloud.config.parent.label.label: ', tested on spring-boot:1.5.12.RELEASE
  #password: demo-app-common_pass
  password: '{token}eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbmNyeXB0ZWQiOiJBUUJTNkhOdUkrRU5vZVgrM0N0Y0p1OTFPYUREbmhSWS8xOERRYmpUbjFkWncxbzZqODFacktxc05ZUXhvNW9XRmp6Ung0RWxUOEVUcmxodTRnVzh4Z3R0L21jaEc4bEZkMkFMcVU3aEpkQmlDT0hZdVhBYThmZ1NScDNKUUdvUmVxeTVJYVJEdHVOcWNLQlJRRUlYYi9PRzhHR2xEaHQyY3UrMFFEdTNoeEhFdVZOenJRaGYvV2RlM2lTUjlZU3ovSmp6K2RMSGhIdEJtbEVEZXExKzZKaERpVE1yYWdoNFk5WUxPTmlkVFRtUG1hdUtreUxqYnJkUFNWUEtKdFN1dTdyem9NT3Y2cnp2dEpwZTJQYTZJai9ob1pDNHcxQUd2TmNYZmNIQlJEeHZucFZWRGJLWTRYKzUxUXhSMmUxL3hJSWtrRXR0L251Y1hmSlJWSkE3NEdiTDY5dUU2WGFTODc3dmhwdEdPa1lDWTkwWFZhSXVQOEhsK2ZmOGc1TlJWSjJOZGNnS3FEOEN4VmNCTzBXVHQxZjJIVDZ3TDlGZ1R4anI1QnNUSHBVSmZZcHBEdHVNS094VFhqV1Z4Tk93RUFDbC9GdnFpQXNNYk1yYUp6TjFKUGNNb2U1bThjQjR5T2NUVzBKa3F0dUpSWGExNmViVFBCbHlxcGpOYjhxYXZxREIwdlRVWFBEWVB0Kzc0MHBYYUNsYmVBVGRTZDR2cGp5MGtCcXRMcFF2TVZ5RzJDdThuSndzYjVpZmxlbVJlR0tnV2VFTk9weW83b2p6dlRERk1DZ1djd0krR0FUSXNBR2ZMT0dJSFZablNSZ1dqUHRMdlpIZk9ZdmlWckgrYmdvPSIsImlzcyI6ImNvbmZpZy1zZXJ2ZXIiLCJleHAiOjE2ODU5MDEwNzJ9.u9n89MGKXnmkiIGsctmpcUWEgoB6CFCpIMCJX32cmdg'
```

Test parent password
```bash
curl -s -X GET -u demo-app:demo-app_pass http://config-server:8888/config/demo-app/production.env,ut.env/develop
curl -s -X GET -u demo-app:demo-app_pass http://config-server:8888/config/demo-app/production.env,ut.env/master
```

- Custom Feature 2. Password pre git config repository
> Users can specify a `spring.cloud.config.password: <password>` in config file to protect their config file url endpoints.

Note:
+ config-server support load `spring.cloud.config.password` form environment profiles or default profile.
+ You can use different `spring.cloud.config.password` in auth-label-develop, auth-profile-staging.env, auth-profile-production.env configs to protect sensible properties.
> If you put password in different environment profiles, you need to provide the application (user) name and environment profile name (where the password stored), join them with a '@' as username.
> i.e. curl -X GET -u application@staging.env http://config-server:8888/config/application/staging.env
+ config-server will use parent's `spring.cloud.config.password` if child's `spring.cloud.config.password` absent and it has a parent.
+ It is recommended to use a encrypted `spring.cloud.config.password` in config file.

user's application.yml example
```yaml
spring.cloud.config.password: demo-app_pass
```

- Custom Feature 3. Git deploy key in file or in classpath
> `spring.cloud.config.git.deploy-key: classpath:<filename> or file:<filename>`

The property in application.yml specifies the private key, public key is privateKey + ".pub".

config-server's application.yml example:
```yaml
spring.cloud:
  config:
      git:
        deploy-key: ${SPRING_CLOUD_CONFIG_SERVER_GIT_DEPLOYKEY:classpath:default-deploy_key}
```

- Custom Feature 4. Security on web-hook endpoint
> Use a pair of different username and password than admin's.

config-server's application.yml example:
```yaml
security:
  basic.enabled: ${SPRING_SECURITY_ENABLED:true}
  hook:
    name: ${SPRING_SECURITY_HOOK_NAME:hook}
    password: ${SPRING_SECURITY_HOOK_PASSWORD:hook_pass}
```


## Deploy config-server

1. Requirements for config repositories

1.1. All config repositories should under same group. i.e. like `spring-cloud-config-server` in this doc
> on gitlab, on github it is user or organization

1.2. All config repositories must use same name pattern
> {application}-config, {application} is same as application's `spring.application.name` value

Parent config repository follow the same rule no matter there are or are no application instances for parent '{application}'.

1.3. There should be a `health-check-config` repository for config-server health check

1.4. All repositories can be accessed with same deploy key
> Github can't meet this requirement, but it is ok, because repositories on github are publicly available

Do not use the default deploy key comes with local demostration.
> Default one is 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJexpGshox4d2mRhYIjOjxlAmcF9k9fKzlr2ylKS32LwMrVeKY+XyV06YvX0FE0uwj3DSp2Vai2e8kEylRDhQmuV1ZjjA08P9/j9SacFuzY8TfncdUwsQ3wxmBjmlpQoODUad7v0ld0r1AfttqbfGJr8L5gPzxvoA96K+6PkYyzUwbStJiW0ruNEVOb5LgN/v90LWMorwXj2Y/fu+i5OWp+iCTrQ6ltC6xQ/f3MyRMbfUxW3cXNp9UkdVkFDJ4Le/5poim5yPi6d2vjG8z7h5hM7M+H7q72hVoH9Rx0yzp55jOSRMXDGU138pK6HQFU/mCw9yaT0OwGK5IdvaX+ryd configserver@home1.cn'

Generate a new deploy key for your staging or production environment.
```bash
ssh-keygen -t rsa -b 2048 -f production-deploy_key -q -N "" -C "config-server@your_domain"
```

2. Encrypt key and keystore

2.1. Symmetric encryption
Easy but not safe enough, not recommended.

config-server's bootstrap.yml or application.yml
```yaml
encrypt:
  key: changeme
```

2.2. Asymmetric encryption
Much more safer than symmetric encryption, recommended.

If you are using java8 or lower versions, 
you need to download '(JCE)Unlimited Strength Jurisdiction Policy Files' (2 jars) from java official site, 
then put them into `jre/lib/security`.

Note: 
+ Create separate keystore and use different password in develop, staging and production environment.
+ config-server can not read keystore encoded by `pkcs12` format, `jks` is supported, 
so do not use `keytool`'s `-deststoretype pkcs12` option

config-server's bootstrap.yml or application.yml
```yaml
encrypt:
  key-store:
    alias: ${ENCRYPT_KEYSTORE_ALIAS:key_alias}
    secret: ${ENCRYPT_KEYSTORE_SECRET:key_pass}
    password: ${ENCRYPT_KEYSTORE_PASSWORD:store_pass}
    location: ${ENCRYPT_KEYSTORE_LOCATION:classpath:default-keystore.jks}
```

2.2.1. Build-in default-keystore.jks is for non-production environments.
It was generated by a command like this:

```bash
keytool -genkeypair \
-alias key_alias \
-deststoretype jks \
-dname "CN=home1,OU=home1,O=home1,L=Beijing,S=Beijing,C=CN" \
-keyalg RSA -keysize 2048 -validity 3650 \
-keypass key_pass \ 
-storepass store_pass \
-keystore default-keystore.jks
```

To list key pairs in jks file, run `keytool -list -storepass store_pass -keystore src/main/resources/default-keystore.jks`.

2.2.2. Generate a keystore and keys for production environment.

Random password is more secure.

Example for dname: `CN=jsvede.bea.com, OU=DRE, O=BEA, L=Denver, ST=Colorado, C=US`

```bash
DNAME="CN=your_cn,OU=your_ou,O=your_o,L=your_l,S=your_s,C=your_c"
ENCRYPT_KEYSTORE_PASSWORD="$(openssl rand -base64 32)"
ENCRYPT_KEYSTORE_SECRET="$(echo `</dev/urandom tr -dc A-Za-z0-9 | head -c16`)"
keytool -genkeypair \
-alias config-key-default \
-deststoretype jks \
-dname "${DNAME}" \
-keyalg RSA -keysize 2048 -validity 3650 \
-keypass ${ENCRYPT_KEYSTORE_SECRET} \
-storepass ${ENCRYPT_KEYSTORE_PASSWORD} \
-keystore production-keystore.jks
```

Change the value of option `alias` and `keypass` then run `keytool` again can create and save multiple key pairs into same jks file.  
To list key pairs in jks file, run `keytool -list -storepass ${ENCRYPT_KEYSTORE_PASSWORD} -keystore production-keystore.jks`.


## Appendix

A. Details of git repository access

org.springframework.cloud.config.server.environment.AbstractScmEnvironmentRepository.getLocations
org.springframework.cloud.config.server.environment.JGitEnvironmentRepository.createGitClient, copyRepository, cloneToBasedir, configureCommand

B. [config properties](https://cloud.spring.io/spring-cloud-static/Dalston.SR4/multi/multi__appendix_compendium_of_configuration_properties.html)

## TODO
Rate limit on /encrypt endpoint
