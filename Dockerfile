FROM  dhi.io/eclipse-temurin:25-alpine3.23@sha256:3df0f4a21d9d377c68804ba5ca30893e70926fc0c70b5454dd254596b482f060

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
