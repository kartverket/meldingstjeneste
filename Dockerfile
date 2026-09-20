FROM  dhi.io/eclipse-temurin:25-alpine3.23@sha256:6be6baf6fa075584e65744769541b91050f310675f383e5b82ca9bd130dac3d1

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
