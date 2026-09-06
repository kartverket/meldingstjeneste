FROM  dhi.io/eclipse-temurin:25-alpine3.23@sha256:55712a033e46dca4fb9f5c8f31dc02da54b406bd262995d1dea0cdf3cdcc639b

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
