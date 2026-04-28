FROM amazoncorretto:17-alpine-jdk

ENV SPRING_PROFILES_ACTIVE production
ENV ANTIVIRUS_ENABLED true

USER 1001
EXPOSE 9000

CMD java -jar /data/app.jar

COPY build/libs/document-upload.jar /data/app.jar
