FROM dhi.io/eclipse-temurin:25-alpine3.24@sha256:8da329a70f7794dd59467841e7476fd96a22fa5d3146c36811d3589b5205a5d0

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
