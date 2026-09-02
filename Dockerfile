FROM  dhi.io/eclipse-temurin:25-alpine3.23@sha256:329e4962ea4a8d7fdb926afdce4f90f8b023c014eca322ef5839dd05d7b92363

ENV TZ=Europe/Oslo

WORKDIR /app

COPY /build/libs/meldingstjeneste-all.jar /app/meldingstjeneste.jar

USER nonroot

EXPOSE 8080:8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "meldingstjeneste.jar"]
