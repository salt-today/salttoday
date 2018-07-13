FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/salttoday.jar /salttoday/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/salttoday/app.jar"]
