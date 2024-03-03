FROM amazoncorretto:21.0.2-alpine3.19
COPY target/*.jar worker-app.jar

EXPOSE 28099

ENTRYPOINT ["java","-jar","/worker-app.jar"]
