FROM eclipse-temurin:21-jre-alpine-3.23@sha256:ad0cdd9782db550ca7dde6939a16fd850d04e683d37d3cff79d84a5848ba6a5a

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
