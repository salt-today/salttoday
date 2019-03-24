FROM openjdk:8u191-jre-alpine3.9

COPY target/uberjar/salttoday.jar /opt/salttoday.jar

EXPOSE 80

CMD ["java", "-Dconf=\"/opt/prod-config.edn\"", "-jar", "/opt/salttoday.jar"]
