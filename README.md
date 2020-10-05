# einvoice-service   

## Build

Local

| Need to skip bootBuildImage for subproject.

`./gradlew bootBuildImage -x :einvoice-common:bootBuildImage`

CI

| Follow the build steps in travis.yml.

## Execution

| Refer to https://docs.spring.io/spring-boot/docs/2.3.3.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config

`sudo docker run --rm -p 8080:8080 -v /home/joelin/einvoice-service:/config -v /home/joelin/turnkey_working_dir:/turnkey_working_dir -e SPRING_PROFILES_ACTIVE=prod -e SPRING_CONFIG_LOCATION=classpath:/,classpath:/config/,file:./,file:./config/*/,file:./config/,file:/config/ joelin/einvoice-service:latest`
