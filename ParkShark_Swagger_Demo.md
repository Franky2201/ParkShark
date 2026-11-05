# ParkShark — Swagger demo draaiboek

Gebaseerd op ParkShark3.5.zip, de bijgeleverde api-docs.json, README en de eerdere DEMO2-seed. Alle 17 HTTP-operaties uit deze OpenAPI worden gebruikt, zonder invoice-stories. Reken op ongeveer 15–20 minuten inclusief uitleg; sla herhaalde filtervarianten over voor een kortere demo. Verwachte resultaten zijn afgeleid uit de code; deze flow is niet uitgevoerd tegen jullie draaiende applicatie/database.

## Vooraf: drie concrete aandachtspunten

1. **Oude parking-contacten aanpassen.** De DEMO2-parkings verwijzen naar Managers, terwijl `ParkingLot.contactPerson` nu een `Member` is. Daardoor kan ook `GET /parking-lots` mislukken, zelfs als je een nieuwe correcte parking aanmaakt. Voer de controle en eventueel de gerichte correctie onderaan uit vóór de demo. De director van een division blijft wel Ned/Tyrion/Daenerys; verander hun rollen niet.
2. **Allocation JSON:** gebruik `licensePlate`, niet `licencePlate`. Het handgeschreven Swagger POST-voorbeeld is verouderd. Bij memberregistratie heet hetzelfde concept juist `license_plate`.
3. **Member-detail lekt een hash:** `GET /person/{id}` retourneert de entity met `getPasswordHash()`. Verberg dit vóór de presentatie, liefst met een response-DTO zonder wachtwoord/hash. Als kleine tijdelijke correctie: zet `@com.fasterxml.jackson.annotation.JsonIgnore` op `Person.getPasswordHash()`. Controleer daarna dat het veld uit de JSON verdwenen is; de getter blijft bruikbaar voor authenticatie.

## Accounts en wisselen

| Naam | Login | Rol / gebruik |
|---|---|---|
| Ned | `ned@parkshark-demo.example` | MANAGER: beheer en overzichten |
| Lara | `lara.demo01@parkshark.example` | Nieuw lid, tijdens de demo aangemaakt; eerst BRONZE, daarna GOLD |
| Brienne | `brienne@parkshark-demo.example` | Bestaand USER-lid: volle parking en eigenaarscontrole |

Voor alle drie: **`ParkSharkDemo123!`**. Dit zijn uitsluitend demoaccounts. Ned heeft person-id `810002`; Brienne `810009`.

In Swagger: **Authorize → Logout → nieuwe username/password → Authorize → Close**. Gebruik de centrale Basic Auth; vul niet nog apart een Authorization-parameter in. De OpenAPI heeft al een globale `basicAuth` security requirement. Een extra Authorization-invoerveld op enkele routes is dus niet nodig. Controleer bij twijfel de gegenereerde curl op de meegestuurde header.

Registreer Lara eerst zonder ingelogd account: de controller voor POST /person vereist geen authenticatie, ook al toont de globale Swagger-configuratie een slotje. Geen ADMIN nodig voor deze flow.

Gebruik één Swagger-tab en groepeer per account. Wisselvolgorde: **uitgelogd → Ned → Lara → Brienne → Lara → Ned**.

## ID-kladhulp

| Symbool in dit draaiboek | Waar vind je het? | Noteer waarde |
|---|---|---|
| MEMBER_ID | Lara in GET /person/members, zoeken op e-mail | … |
| DIVISION_ID | id uit POST /division | … |
| SUBDIVISION_ID | id uit POST /division/{parentDivisionId}/subdivisions | … |
| LOT_ID | id uit POST /parking-lots | … |
| ALLOCATION_A | id van Lara's eerste, normale parksessie | … |
| ALLOCATION_B | id van Lara's tweede, Gold-huurautosessie | … |
| ALLOCATION_C | id van Lara's derde sessie, na vrijmaken van de plaats | … |

**Swagger vervangt deze symbolen niet automatisch.** Vul in pathvelden het echte getal in. In JSON gebruiken we `0` als duidelijk vervangpunt: vervang die waarde telkens door het genoemde ID vóór Execute. Voorspel nieuwe IDs niet op basis van de seed; sequences kunnen door eerdere tests verder staan.

## 1 — Registreren zonder account: ME1 + ML1

**POST /person** — geen login — verwacht **201**, lege responsebody.

```json
{
  "name": {"firstName": "Lara", "lastName": "Demo"},
  "phoneNumber": "011123456",
  "mobileNumber": "0470123456",
  "email": "lara.demo01@parkshark.example",
  "password": "ParkSharkDemo123!",
  "address": {
    "street": "Presentatiestraat",
    "number": "1",
    "postal_code": {"zip_code": "3000", "label": "Leuven Demo01"}
  },
  "license_plate": {"number": "DEMO-LARA-01", "country": "Belgium"}
}
```

Vertel: “Een nieuw lid registreert zich met contactgegevens en een nummerplaat. Zonder opgegeven membership wordt het automatisch Bronze.”

De email voldoet aan jullie huidige strikte regex. Gebruik voor deze nieuwe registratie niet het oude domein `parkshark-demo.example`: de registratie-regex weigert dat koppelteken, hoewel bestaande seedaccounts er wel mee kunnen inloggen.

## 2 — Ned: leden, organisatie en parking

Log in als **Ned** en blijf voor dit hele blok ingelogd.

### 2.1 Ledenoverzicht — ME2

**GET /person/members** → **200**. Zoek `lara.demo01@parkshark.example` en noteer **MEMBER_ID**.

Vertel: “Het overzicht bevat alleen de afgesproken overzichtsvelden, zoals naam, nummerplaat en contactgegevens.” POST /person gaf zelf geen ID terug; daarom halen we het hier op.

### 2.2 Detail van het nieuwe lid — ME3 + standaard Bronze

**GET /person/{id}**, id = **MEMBER_ID** → **200**.

Toon de registratiegegevens en `membership: "BRONZE"`. Het detail hoort na de vooraf genoemde correctie geen passwordHash te bevatten.

### 2.3 Een divisie maken — DI1

**POST /division** → **201**. Noteer **DIVISION_ID**.

```json
{
  "name": "LIVE01 - ParkShark Vlaanderen",
  "originalName": "Vlaanderen Parking",
  "directorId": 810002
}
```

Vertel: “Een manager maakt een divisie aan met een directeur en de oorspronkelijke bedrijfsnaam.”

### 2.4 Een subdivisie maken — DI4

**POST /division/{parentDivisionId}/subdivisions**, parentDivisionId = **DIVISION_ID** → **201**. Noteer **SUBDIVISION_ID**.

```json
{
  "name": "LIVE01 - Leuven",
  "originalName": "Leuven City Parking",
  "directorId": 810003
}
```

Toon in de response `parentDivision`: deze verwijst naar de divisie uit de vorige stap. Hier is Tyrion directeur, terwijl Ned de aanvraag uitvoert.

### 2.5 Divisies opvragen — DI2 + DI3

| Aanroep | Verwacht | Wat toon je? |
|---|---|---|
| GET /division | 200 | Nieuwe divisies tussen de bestaande mockdata |
| GET /division/{id}, id = SUBDIVISION_ID | 200 | Eén divisie en haar parentDivision |

### 2.6 Een kleine parking maken — PL1 + DIPL1

**POST /parking-lots** → **201**. Vervang `divisionId: 0` door **SUBDIVISION_ID**. Noteer **LOT_ID**.

```json
{
  "name": "LIVE01 - Leuven Demo Parking",
  "category": "UNDERGROUND",
  "maxCapacity": 1,
  "pricePerHour": 2.5,
  "address": {
    "street": "Parkeerstraat",
    "number": "10",
    "postal_code": {"zip_code": "3000", "label": "Leuven Demo01"}
  },
  "contactPersonId": 810009,
  "divisionId": 0
}
```

Brienne is bewust de contactpersoon: de huidige service accepteert alleen Members voor die functie. Deze parking start zonder allocations. Capaciteit 1 maakt de capaciteitsregel heel zichtbaar en vermijdt afhankelijkheid van de actuele bezetting in de seed.

### 2.7 Parkingoverzicht en detail — PL2 + PL3

| Aanroep | Verwacht | Wat toon je? |
|---|---|---|
| GET /parking-lots | 200 | Beperkt overzicht: id, naam, capaciteit, contactemail en telefoon |
| GET /parking-lots/{id}, id = LOT_ID | 200 | Categorie, prijs, adres, contactPersonId en divisie |

**Let op:** `capacity` in het overzicht is de maximumcapaciteit, niet het aantal vrije plaatsen. De bezetting tonen we later via actieve allocations.

## 3 — Lara: parkeren als Bronze, daarna Gold

Wissel naar **lara.demo01@parkshark.example** met hetzelfde demowachtwoord.

### 3.1 Verkeerde nummerplaat als Bronze — PSA1

**POST /allocation** → **400**. Vervang parkingLotId door **LOT_ID**.

```json
{
  "licensePlate": {"number": "DEMO-RENTAL-01", "country": "Belgium"},
  "parkingLotId": 0
}
```

Vertel: “Bronze mag alleen parkeren met de geregistreerde nummerplaat.” Verwachte detailtekst: `Licence plate has to match member if membership is not Gold`.

### 3.2 Correcte nummerplaat — PSA1

**POST /allocation** → **201**. Vervang parkingLotId door **LOT_ID**; noteer **ALLOCATION_A**.

```json
{
  "licensePlate": {"number": "DEMO-LARA-01", "country": "Belgium"},
  "parkingLotId": 0
}
```

Toon `id`, `memberId`, `parkingLotId`, `startTime`, `endTime: null` en `status: "ACTIVE"`. De member wordt afgeleid uit Basic Auth; het request bevat geen memberId. De server bepaalt de starttijd. Een tijd met `Z` is UTC; in België is die in november één uur vroeger dan de lokale klok, maar hetzelfde tijdstip.

### 3.3 Eigen sessie stoppen — PSA2

**PUT /allocation/stop/{id}**, id = **ALLOCATION_A**, geen body → **200**.

Toon `status: "PASSIVE"`, ingevulde `endTime` en `duration`. De ene plaats is weer vrij.

### 3.4 Membership wijzigen — ML1

**PUT /person/membership** → **200**, lege body in de response.

```json
{"membership": "GOLD"}
```

Dit wijzigt Lara, de ingelogde member. Dezelfde aanvraag herhalen is geen nieuw succes: de code weigert een ongewijzigde membership. In de demo voeren we de wijziging één keer uit.

### 3.5 Dezelfde huurauto werkt nu wel — PSA1 / Gold

Herhaal **exact het request van 3.1** met LOT_ID → nu **201**. Noteer **ALLOCATION_B**.

Vertel: “Dezelfde nummerplaat die als Bronze werd geweigerd, wordt nu als Gold aanvaard.” Toon `licensePlateNumber: "DEMO-RENTAL-01"`. De parking is nu vol.

## 4 — Brienne: twee duidelijke businessregels

Wissel naar **brienne@parkshark-demo.example**.

### 4.1 Volle parking — PSA1

**POST /allocation** → **409**. Gebruik **LOT_ID**.

```json
{
  "licensePlate": {"number": "DEMO2-BRIENNE", "country": "Belgium"},
  "parkingLotId": 0
}
```

De nummerplaat is correct voor Brienne; daardoor is de capaciteit de reden van weigering. Detail: `Parking Lot has not enough capacity left`. Er wordt geen nieuwe allocation aangemaakt.

### 4.2 Andermans sessie stoppen — PSA2

**PUT /allocation/stop/{id}**, id = **ALLOCATION_B**, geen body → **403**.

Vertel: “Een ander lid kan Lara's parkeerbeurt niet beëindigen.” Detail: `Only member that created Allocation, can stop it`. De sessie blijft ACTIVE.

Optioneel, als je ook één zuivere rolcontrole wilt tonen: **GET /division** als Brienne → **403**. Een lid heeft geen managerrechten. Geen extra wachtwoordvarianten nodig.

## 5 — Lara: stoppen maakt opnieuw plaats

Wissel terug naar **Lara**.

| Stap | Aanroep | Verwacht |
|---|---|---|
| 5.1 | PUT /allocation/stop/{id}, id = ALLOCATION_B | 200, PASSIVE, endTime ingevuld |
| 5.2 | Herhaal dezelfde PUT | 409: allocation moet ACTIVE zijn om te stoppen |
| 5.3 | Herhaal POST /allocation van 3.2 met DEMO-LARA-01 en LOT_ID | 201: plaats is opnieuw beschikbaar; noteer ALLOCATION_C |

Laat **ALLOCATION_C actief** voor de managerfilters en de demonstratie van een oplopende duur.

## 6 — Ned: filters en duur — PSA3, PSA4, PSA5, PSA6

Wissel terug naar **Ned**. Alle onderstaande requests verwachten **200**.

In Swagger vul je de queryvelden afzonderlijk in; plak niet de hele querystring in een id-veld. Laat ongebruikte filters echt leeg. De statuswaarden zijn hoofdlettergevoelig: `ACTIVE`, `PASSIVE`, `INVOICED`.

| Stap | Aanroep | Wat toon je? |
|---|---|---|
| 6.1 | GET /person/{id}, id = MEMBER_ID | Lara is nu GOLD |
| 6.2 | GET /allocation, alle filters leeg | Alle allocations, standaard oudste starttijd eerst |
| 6.3 | GET /allocation?limit=2&desc=true | Maximaal twee resultaten, nieuwste starttijd eerst |
| 6.4 | GET /allocation?status=ACTIVE&desc=true | Alleen actieve sessies, waaronder ALLOCATION_C |
| 6.5 | GET /allocation/member/{id}, id = MEMBER_ID, status leeg | Lara's A en B PASSIVE, C ACTIVE |
| 6.6 | Dezelfde member-route met status=PASSIVE | Alleen A en B |
| 6.7 | GET /allocation/parkinglot/{id}, id = LOT_ID, status leeg | Drie succesvolle sessies in deze parking; mislukte aanvragen ontbreken |
| 6.8 | Dezelfde parking-route met status=ACTIVE | Alleen C: één actieve sessie bij maximumcapaciteit één |
| 6.9 | Herhaal 6.8 na enkele seconden | `duration` loopt door voor C |
| 6.10 | Herhaal 6.6 | Duur van gestopte sessies A en B blijft gelijk |

De verwachte aantallen in deze nieuwe member/parking gelden als alleen deze flow erop is uitgevoerd. Globale lijsten bevatten ook de bestaande seed en eerdere teamrequests: voorspel daar geen totaal aantal. Bij gelijke starttijd gebruikt het algemene overzicht id als tweede sortering. De per-member/per-parkingroutes hebben geen gegarandeerde sortering; vertel daar alleen iets over de filtering.

Vertel: “De manager kan het volledige gebruik bekijken, beperken en filteren per status, lid of parking. Een actieve parkeerduur groeit mee; een gestopte duur ligt vast.”

Een `INVOICED` filter kan bestaande seedrecords tonen, maar bewijst geen werkende facturatie. We laten dit buiten deze demo.

## Na de demo en herhalen

Om de nieuwe parking vrij achter te laten: log als Lara in en stop **ALLOCATION_C**. Geen data verwijderen nodig.

Voor een volgende volledige repetitie maak je een nieuwe registratie en organisatie: vervang `01` door `02` in email, nummerplaat, namen en **postcode-label**. Het huidige registratiepad maakt telkens een nieuwe postal_code aan, terwijl (zip_code, label) uniek is. Zonder nieuw label kan registratie dus op een bestaande combinatie botsen. De parkingservice hergebruikt de combinatie wel; daarom mag de parking dezelfde combinatie gebruiken als de net geregistreerde Lara.

Nieuwe email: `lara.demo02@parkshark.example`, nummerplaat `DEMO-LARA-02`, label `Leuven Demo02`, organisatienamen met `LIVE02`. Pas het login en alle Lara-allocationrequests mee aan. Bewaar de nieuwe response-IDs opnieuw. Voer niet blind de hele seed opnieuw uit: die is geen resetscript.

## Vooraf: oude DEMO2-contactpersonen controleren en herstellen

Dit SQL-blok is bedoeld voor de ontwikkel/mockdatabase en is **niet uitgevoerd**. Het wijzigt alleen de zes oude DEMO2-parkings met de verwachte oude managerreferentie. Het laat de allocations, directors en personen zelf intact.

Eerst controleren:

```sql
SELECT pl.id, pl.name, pl.contact_person_id, p.role
FROM parkshark.parking_lot pl
JOIN parkshark.person p ON p.id = pl.contact_person_id
WHERE p.role <> 'USER'
ORDER BY pl.id;
```

Als dit de oude DEMO2-parkings oplevert, pas hun contacten aan naar bestaande Members:

```sql
BEGIN;

UPDATE parkshark.parking_lot AS pl
SET contact_person_id = mapping.member_id
FROM (VALUES
    (810001, 810002, 810009),
    (810002, 810003, 810010),
    (810003, 810004, 810007),
    (810004, 810002, 810009),
    (810005, 810002, 810009),
    (810006, 810003, 810010)
) AS mapping(lot_id, old_manager_id, member_id)
JOIN parkshark.person AS p
  ON p.id = mapping.member_id AND p.role = 'USER'
WHERE pl.id = mapping.lot_id
  AND pl.name LIKE 'DEMO2%'
  AND pl.contact_person_id = mapping.old_manager_id
RETURNING pl.id, pl.name, pl.contact_person_id;

COMMIT;
```

Brienne wordt contact voor de North-parkings, Davos voor de Crownlands-parkings, Sansa voor Dragonstone. Voer de controlequery opnieuw uit. Als andere parkings met niet-USER contacten overblijven, moeten die ook afgestemd worden op de huidige entitymapping voordat je een volledig parkingoverzicht kunt vertrouwen. Dit is een aanpassing aan de **huidige implementatie**, geen uitspraak dat de README een Member-contactpersoon vereist.

## Korte checklist voor de presentator

- Oude contacten afgestemd op de Member-mapping; GET /parking-lots vooraf geprobeerd.
- PasswordHash uit member-detail gehaald.
- Verse LIVE01-gegevens, of consequent een nieuwe suffix gebruikt.
- Ned en Brienne kunnen inloggen met het demowachtwoord.
- Nieuwe IDs direct genoteerd; `0` in de JSON vervangen.
- `license_plate` bij registratie, `licensePlate` bij allocation.
- Max capaciteit van de nieuwe parking = 1; niemand anders gebruikt die tijdens de demo.
- Verwachte 400/403/409 presenteren als gecontroleerd businessgedrag.
- Geen claims over berekende prijzen/facturatie: deze flow toont membership en parkeerbeheer.
