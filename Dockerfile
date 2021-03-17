FROM openjdk:13-alpine

EXPOSE 8080

RUN mkdir -p /app

COPY target/scala-2.13/*.jar /app/

WORKDIR /app

CMD java -jar ./app.jar
