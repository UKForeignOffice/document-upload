FROM cimg/openjdk:17.0.11

ENV SPRING_PROFILES_ACTIVE production
ENV ANTIVIRUS_ENABLED false

RUN mkdir -p /data
COPY build/libs/document-upload.jar /data/app.jar

USER 1001
EXPOSE 9000

CMD ["java", "-jar", "/data/app.jar"]
