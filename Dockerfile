FROM hapiproject/hapi:base as build-hapi

WORKDIR /tmp/hapi-fhir-jpaserver-starter
COPY . .

RUN /tmp/apache-maven-3.6.2/bin/mvn clean install -DskipTests

FROM tomcat:9-jre11
RUN rm -rf /usr/local/tomcat/webapps/*
RUN mkdir -p /data/hapi/lucenefiles && chmod 775 /data/hapi/lucenefiles
COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/target/hapi.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]