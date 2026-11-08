FROM dhi.io/eclipse-temurin:27-alpine3.24@sha256:89798eda4965ed3df1c3bee3a9c385de12e8b2bf55350a728a677a116f1c0fc5

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
