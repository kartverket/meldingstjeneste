FROM  dhi.io/eclipse-temurin:25-alpine3.23@sha256:9a1468f69531dcd1604d7a9865a8826b5f94fc07e066417981c07d12ee737f98

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
