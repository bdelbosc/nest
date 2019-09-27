FROM openjdk:11-jdk-slim

ENTRYPOINT ["java", "-Xmx24m", "-Xms24m", "-Dlog4j.configurationFile=/my-log4j2.xml", "-Xdebug", "-XX:+FlightRecorder", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787", "-jar", "/my-app.jar"]
WORKDIR /
COPY src/main/resources/log4j2.xml /my-log4j2.xml
COPY src/test/resources/bjcp-2015.json /bjcp-2015.json
ARG JAR_FILE=training-nuxeo-importer-1.0-SNAPSHOT.jar
COPY target/${JAR_FILE}            /my-app.jar
