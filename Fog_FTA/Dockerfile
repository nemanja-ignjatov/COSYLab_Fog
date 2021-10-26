FROM openjdk:11.0.8-jre
ARG JAR_FILE=target/*.jar
COPY tntacert.pem tntacert.pem
COPY ${JAR_FILE} fta.jar
RUN mkdir /keystore
RUN chmod +x /fta.jar
ENTRYPOINT ["java","-jar","/fta.jar"]