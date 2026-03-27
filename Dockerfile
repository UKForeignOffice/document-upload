FROM cimg/openjdk:17.0.11

ENV SPRING_PROFILES_ACTIVE production
ENV ANTIVIRUS_ENABLED false

EXPOSE 9000

CMD java -jar /data/app.jar

ADD build/libs/document-upload.jar /data/app.jar
