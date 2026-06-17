FROM eclipse-temurin:25-jre-alpine-3.23@sha256:f10d6259d0798c1e12179b6bf3b63cea0d6843f7b09c9f9c9c422c50e44379ec

RUN apk update && apk upgrade

ENV USER_ID=150 \
    USER_NAME=apprunner \
    TZ=Europe/Oslo

RUN addgroup -g ${USER_ID} ${USER_NAME} \
    && adduser -u ${USER_ID} -G ${USER_NAME} -D ${USER_NAME}

WORKDIR /app

COPY --chown=${USER_ID}:${USER_ID} /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER ${USER_NAME}
EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]