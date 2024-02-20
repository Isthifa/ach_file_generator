FROM openjdk:17
COPY target/achpayment.jar achpayment.jar
ENTRYPOINT ["java", "-jar", "achpayment.jar"]