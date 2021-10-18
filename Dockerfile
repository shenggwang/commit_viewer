ARG JAVA_VERSION=11
ARG MAVEN_VERSION=3.6.3
#FROM maven:${MAVEN_VERSION}-jdk-${VERSION} as BUILD
FROM maven:${MAVEN_VERSION}-adoptopenjdk-${JAVA_VERSION} as BUILD

COPY . /opt/server
WORKDIR /opt/server
RUN mvn install -T 4 -DskipTests

#FROM openjdk:${VERSION}-jre
FROM openjdk:${JAVA_VERSION}-jre-slim-stretch
ENV JAR='server-1.0.0-jar-with-dependencies.jar'
ENV DEBUG='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5000'

COPY --from=BUILD /opt/server/target/classes /opt/server/classes
COPY --from=BUILD /opt/server/target/${JAR} /opt/server/${JAR}
WORKDIR /opt/server

ENTRYPOINT ["/bin/sh", "-c", "java ${DEBUG} -jar ${JAR}"]
