FROM openjdk:8-alpine

ENV SPRING_PROFILES_ACTIVE production
ENV ANTIVIRUS_ENABLED true

EXPOSE 9000

CMD java -jar /data/app.jar

ADD build/libs/document-upload.jar /data/app.jar
