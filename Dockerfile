FROM java:8-alpine

ADD target/basic-pedestal-sqs-example-0.0.1-SNAPSHOT-standalone.jar /basic-pedestal-sqs-example/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/basic-pedestal-sqs-example/app.jar"]
