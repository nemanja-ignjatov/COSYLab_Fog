FROM arm64v8/openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY tntacert.pem tntacert.pem
COPY ${JAR_FILE} ftp.jar
RUN mkdir /keystore
RUN chmod +x /ftp.jar
ENTRYPOINT ["java","-jar","/ftp.jar"]