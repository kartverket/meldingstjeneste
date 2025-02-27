FROM eclipse-temurin:21-sdk-alpine@sha256:511d5a9217ed753d9c099d3d753111d7f9e0e40550b860bceac042f4e55f715c
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
