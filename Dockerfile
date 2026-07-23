FROM  dhi.io/eclipse-temurin:26-alpine3.23@sha256:fe31cc53e56ee9dd3ca1268fca813e4bdcad663f0b87a7ab31280f5b12b08e1e

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
