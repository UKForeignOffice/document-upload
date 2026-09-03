FROM ibm-semeru-runtimes:open-25-jre

ENV SPRING_PROFILES_ACTIVE production
ENV ANTIVIRUS_ENABLED true

WORKDIR /app
COPY build/libs/*.jar .

ENV SERVER_PORT=9000
EXPOSE $SERVER_PORT

USER www-data
HEALTHCHECK CMD curl -f "http://localhost:$SERVER_PORT/v1/actuator/health" || exit 1
CMD ["java", "-jar", "document-upload-1.0.0.jar"]
