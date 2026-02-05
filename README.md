## Varslingstjeneste
Tjenesten kobler seg opp mot [Altinns Notifications API](https://docs.altinn.studio/notifications/reference/api/). 
Endepunktene til tjenesten er bygget ovenpå dette API-et.

## API-dokumentasjon
Dette prosjektet bruker Swagger for å dokumentere API-et: https://meldingstjeneste.atgcp1-dev.kartverket-intern.cloud/swagger/index.html
Når du kjører opp appen lokalt finner du denne dokumentasjonen på http://localhost:8080/swagger.


## Kjøre med Docker
Dersom man ikke har bygget Docker image må man gjøre dette først.
Naviger til rotnivå i prosjektet (samme nivå som Dockerfile) og
kjør kommandoen under. Dette bygger et Docker image med tag _meldingstjeneste_.
```bash
./gradlew build
docker build -t meldingstjeneste .
```

Deretter må man kjøre applikasjonen. Dette kan gjøres ved å skrive inn kommandoen
```bash
docker run --env-file .env -p 8080:8080 meldingstjeneste
```
Her må man også opprette en `.env` fil med innhold tilsvarende `.env.template` som man finner på rotnivå.
