# spring-cloud-config

[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=cn.home1%3Aspring-cloud-config&metric=alert_status)](https://sonarcloud.io/dashboard?id=cn.home1%3Aspring-cloud-config)  
[Maven Site (release)](https://cloud-ready.github.io/cloud-ready/release/build-docker/spring-cloud-config/index.html)  
[Maven Site (snapshot)](https://cloud-ready.github.io/cloud-ready/snapshot/build-docker/spring-cloud-config/index.html)  
[Artifacts (release)](https://oss.sonatype.org/content/repositories/releases/cn/home1/spring-cloud-config/)  
[Artifacts (snapshot)](https://oss.sonatype.org/content/repositories/snapshots/cn/home1/spring-cloud-config/)  
[Source Repository](https://github.com/cloud-ready/spring-cloud-config/tree/develop)  
[CI](https://travis-ci.org/cloud-ready/spring-cloud-config)  
[![Build Status](https://travis-ci.org/cloud-ready/spring-cloud-config.svg?branch=develop)](https://travis-ci.org/cloud-ready/spring-cloud-config)  


spring-cloud-config

[spring-cloud-config-server](https://github.com/cloud-ready/spring-cloud-config/tree/develop/spring-cloud-config-server)  
[Docker Hub (spring-cloud-config-server)](https://hub.docker.com/r/cloudready/spring-cloud-config-server/)  


see: http://cloud.spring.io/spring-cloud-static/spring-cloud-config/1.4.3.RELEASE/


## Build this project

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home"
# or
#export JAVA_HOME="/usr/lib/jvm/java-11-openjdk"

./mvnw -e -ntp -U clean install

./mvnw -e -ntp -U clean install sonar:sonar site deploy site-deploy

#./mvnw -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -DgenerateReports=false help:active-profiles clean install spotbugs:spotbugs spotbugs:check pmd:pmd pmd:check
```
