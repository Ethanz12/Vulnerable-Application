FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM tomcat:9.0-jdk17-temurin-noble
LABEL org.opencontainers.image.description "Deliberately vulnerable campus events app - SECURITY TRAINING LAB ONLY"
RUN rm -rf /usr/local/tomcat/webapps/* \
    && useradd -r -s /bin/false tomcat \
    && chown -R tomcat:tomcat /usr/local/tomcat
COPY --from=build /src/target/campus-events.war /usr/local/tomcat/webapps/campus-events.war
COPY rootwebapp/ /usr/local/tomcat/webapps/ROOT/
RUN chown -R tomcat:tomcat /usr/local/tomcat/webapps
EXPOSE 8080
USER tomcat
CMD ["catalina.sh", "run"]
