

config options:
https://cloud.spring.io/spring-cloud-static/Dalston.SR4/multi/multi__appendix_compendium_of_configuration_properties.html

config-server's application.yml
```yaml
security:
  basic.enabled: ${SECURITY_BASIC_ENABLED:true}
  user:
    name: ${SECURITY_USER_NAME:admin}
    password: ${SECURITY_USER_PASSWORD:admin_pass}
  hook:
    name: ${SECURITY_HOOK_NAME:hook}
    password: ${SECURITY_HOOK_PASSWORD:hook_pass}

spring.cloud:
  config:
    server:
      default-application-name: application
      git:
        delete-untracked-branches: true
        deploy-key: # custom property
          private: ${SPRING_CLOUD_CONFIG_SERVER_DEPLOYKEY:classpath:default_deploy_key}
          public: ${SPRING_CLOUD_CONFIG_SERVER_DEPLOYKEY:classpath:default_deploy_key.pub}
      monitor:
        gitlabrepo.enabled: ${SPRING_CLOUD_CONFIG_SERVER_MONITOR_GITLABREPO_ENABLED:false}
```


application.yml:
```yaml

spring.cloud.config.parent:
  application: demo-app-common
  label: '{label}' # must quote, if not the 'spring.cloud.config.parent.label: {label}' will becomes 'spring.cloud.config.parent.label.label: ', tested on spring-boot:1.5.12.RELEASE
  password: demo-app-common_pass

spring.cloud.config.password: demo-app_pass

```

config-server load `spring.cloud.config.password` with out profile, so do not define it under profile.

use parent config's `spring.cloud.config.password` if child's `spring.cloud.config.password` absent.

test:
http://config-server.local:8888/
http://config-server.local:8888/config/deployKeyPublic
http://config-server.local:8888/config/demo-app/production.env/develop
http://config-server.local:8888/config/demo-app/it.env/develop
http://config-server.local:8888/manage/info
http://config-server.local:8888/manage/health

## TODO
rate limit on /decrypt endpoint
