## Varslingstjeneste
Tjenesten kobler seg opp mot [Altinns Notifications API](https://docs.altinn.studio/notifications/reference/api/). 
Endepunktene til tjenesten er bygget ovenpå dette API-et.

## API-dokumentasjon
Dette prosjektet bruker Swagger for å dokumentere API-et.
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
Verdiene kan hentes fra applikasjons-pod'en i ArgoCD gjennom å skrive `env` i shellet.

## Hot reload
For å skru på hot reload må du først aktivere development mode i ktor, og så sørge for at prosjektet rekompileres ved endring.

### I IntelliJ
1. Aktivere dev mode
    - IntelliJ: Sett io.ktor.development=true med -D flagg i VM options
        ```-Dio.ktor.development=true```

### Med Gradle-skript
Flagget `io.ktor.development` settes i `application {}` i `gradle.build.kts`, og hentes fra gradle ekstra properties. Når du kjører `run`-scriptet må du sette denne med -Pdevelopment. I tilleg må du kjøre `build`-scriptet separat med `-t`.

1. Kjør `gradle -t build --exclude-task test` i applikasjonens rot-mappe.
2. Åpne en ny terminal og kjør `gradle -Pdevelopment=true run` i samme mappe.

### I IntelliJ
Sett `clientId=<clientId>,jwk=<jwk> i environment variables i en run-profile.

### I terminalen
Sett miljøvariablene lokalt når du kjører run-scriptet.
`jwk=<JWK> clientId=<clientId> gradle -Pdevelopment=true run`

## Kjøre tester
Kan kjøres gjennom terminalen ved:
```
jwk=x clientId=x ./gradlew test
```
