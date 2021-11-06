FROM arm64v8/openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY tntacert.pem tntacert.pem
COPY ${JAR_FILE} ccaa.jar
RUN mkdir /keystore
RUN chmod +x /ccaa.jar
ENTRYPOINT ["java","-jar","/ccaa.jar"]