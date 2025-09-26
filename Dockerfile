FROM eclipse-temurin:25-jdk-alpine@sha256:f4c0b771cfed29902e1dd2e5c183b9feca633c7686fb85e278a0486b03d27369
RUN apk update && apk upgrade

ENV USER_ID=150 \
    USER_NAME=apprunner \
    TZ=Europe/Oslo

RUN addgroup -g ${USER_ID} ${USER_NAME} \
    && adduser -u ${USER_ID} -G ${USER_NAME} -D ${USER_NAME}

COPY --chown=${USER_ID}:${USER_ID} /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER ${USER_NAME}
EXPOSE 8080:8080
ENTRYPOINT ["java","-jar","/app/meldingstjeneste.jar"]
