FROM openjdk:8u191-jre-alpine3.9

COPY target/uberjar/salttoday.jar /opt/salttoday.jar

EXPOSE 443

CMD ["java", "-jar", "/opt/salttoday.jar"]
