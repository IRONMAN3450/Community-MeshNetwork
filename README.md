# Community WiFi Emergency System

Local Wi‑Fi based community emergency platform for disaster response when cellular networks fail.

Devices connect to the same Wi‑Fi hotspot / LAN. The Spring Boot server stores SOS alerts, incidents, rescue assignments, mesh messages, and network nodes, then serves a real-time dashboard and WebSocket chat.

## Features

- Signup / login with encrypted passwords and session tokens
- Role-based actions for Citizen, Responder, and Admin
- SOS emergency alerts with GPS coordinates
- Incident create / view / archive
- LAN mesh messaging over WebSocket
- Live dashboard statistics from the database
- Rescue team assignment and status workflow
- Network map with SOS, teams, nodes, and incidents
- Editable user profile with emergency contacts

## Requirements

- Java 17+ (Java 26 works)
- Maven 3.9+

## Build and test

```bash
mvn clean test
```

To run the HTTP-level integration smoke test, first start the application and then run `.\scripts\smoke-test.ps1` in PowerShell.

## Run the project

```bash
mvn spring-boot:run
```

Open:

- App: http://localhost:18082
- H2 console is disabled by default. For local-only troubleshooting, set `MESH_H2_CONSOLE_ENABLED=true` before starting and open http://localhost:18082/h2-console. The demo database uses user `sa` with a blank password; never expose the console on a LAN or production network.

## Demo accounts

Demo accounts and sample records are opt-in. To seed them on a new, empty database, set `MESH_SEED_DEMO_DATA=true` before starting the application. For example, in PowerShell:

```powershell
$env:MESH_SEED_DEMO_DATA = "true"
mvn spring-boot:run
```

Default startup does not insert sample accounts, SOS alerts, incidents, or network records. Existing database records are preserved when seeding is disabled.

Seeded demo accounts:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@mesh.local | password123 |
| Responder | responder@mesh.local | password123 |
| Citizen | citizen@mesh.local | password123 |

## Faculty demonstration guide

1. Start the app with demo seeding enabled as above, or use accounts already registered in your database.
2. Open http://localhost:18082 on a laptop.
3. Login as `citizen@mesh.local` / `password123` (new signups always receive the Citizen role).
4. Open **SOS**, select **Medical**, allow location, send SOS.
5. Open **Dashboard** at http://localhost:18082 and show live SOS / incident counts.
6. Login as `responder@mesh.local` / `password123` in another browser/profile.
7. Open **Rescue Tracking**, register a team if needed, assign a team, then click **Update Status** until resolved.
8. Open **Mesh Communication** on two browsers and send messages.
9. Open **Network Map** and show SOS / team / node markers.
10. Optional LAN demo: create a phone hotspot, connect another device, open `http://<host-ip>:18082`.

## LAN / hotspot demo

1. Start the server on one machine.
2. Create a Wi‑Fi hotspot or join the same router network.
3. Find the host IP (`ipconfig` on Windows).
4. On phones/laptops open `http://HOST_IP:18082`.
5. Use Mesh Communication to show local real-time messaging.

## API overview

- `POST /api/accounts/signup`
- `POST /api/accounts/login`
- `POST /api/accounts/logout`
- `GET|PUT /api/accounts/me`
- `POST|GET /api/sos`
- `GET /api/sos/active`
- `GET /api/sos/mine`
- `POST|GET /api/incidents`
- `GET /api/incidents/{id}`
- `PUT /api/incidents/{id}`
- `DELETE /api/incidents/{id}` (archives)
- `GET /api/dashboard`
- `GET /api/map`
- `GET|POST /api/mesh/messages`
- `WS /ws/mesh`
- `GET|POST /api/rescue/teams`
- `POST /api/rescue/assign`
- `GET /api/rescue/assignments`
- `GET|POST /api/nodes`
- `POST /api/nodes/{id}/heartbeat`

Protected write APIs expect header:

```text
X-Auth-Token: <token from login/signup>
```

## Initial administrator

To provision the first administrator in the local H2 database, run `scripts/provision-admin.ps1` from the project folder. It creates `admin@communitymesh.local` for Rudra Mishra with a randomly generated one-time password, and prints that password once. Save it and sign in through `login.html`. The script does not enable demo-data seeding or change an existing administrator's password. The administrator can manage incidents, SOS response status, rescue teams and assignments, and network nodes from the dashboard's operations links.

## Project structure

```text
src/main/java/org/example/mesh2/
  account/     authentication and profile
  auth/        session tokens and interceptor
  sos/         emergency alerts
  incident/    incident management
  rescue/      teams and assignments
  mesh/        messaging + websocket
  node/        network nodes
  dashboard/   stats and map payload
  config/      web, websocket, demo seeder
src/main/resources/static/
  HTML pages, CSS, shared JS client
```

## Notes / limitations

- This is a **LAN / same-Wi‑Fi** emergency system, not a phone-to-phone radio mesh without a local server.
- True offline multi-hop mesh would require native mobile peer APIs; the project demonstrates the practical college-ready LAN approach.
- The application does not dispatch real emergency services. It stores and displays data on the local server; users must use established emergency channels when someone is in danger.
- The live map uses Leaflet assets and OpenStreetMap tiles from external services, so map tiles require internet access even when the app server is on a LAN.
- New public signups are Citizens. Responder/Admin accounts are provisioned by the demo seeder; production use needs a real administrator workflow and secure transport/credentials.
- The H2 console is disabled by default because it uses a blank demo password. Keep it disabled when serving the application to a LAN.
- Demo data is inserted only when `MESH_SEED_DEMO_DATA=true` and the database has no users. Turning seeding off does not remove existing records.
