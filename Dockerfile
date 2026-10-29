FROM dhi.io/eclipse-temurin:25-alpine3.24@sha256:7f6fe5ca802ecb2e6de576a47813213949b11a1116030e57151b6c8b94adfa26

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
