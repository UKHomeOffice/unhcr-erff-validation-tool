FROM amazoncorretto:20-alpine-jdk

ENV APP_HOME=/var/lib/unhcr-validation-tool

RUN mkdir -p ${APP_HOME}
WORKDIR ${APP_HOME}

EXPOSE 8080

COPY target/unhcr-erff-validation-tool-*-full.jar ${APP_HOME}/unhcr-erff-validation-tool-full.jar

ENTRYPOINT java \
    -Xms128m -Xmx128m \
    -jar unhcr-erff-validation-tool-full.jar \
    --web-port=8080