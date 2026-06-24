# QUICKSTART — Run this project after cloning

Follow these steps **in order**. Do not skip any step.

---

## Prerequisites — Cài đặt trước

| Tool | Check command | Required version |
|------|--------------|-----------------|
| Docker Desktop | `docker --version` | Latest |
| Java | `java --version` | 21+ |
| Python | `python --version` | 3.8+ |
| Node.js | `node --version` | 18+ |
| OpenSSL | `openssl version` | any (pre-installed on Mac) |

---

## Step 1 — Generate SSL certificates (one-time only)

> Chỉ làm 1 lần sau khi clone. Certs được gitignore, không có trong repo.
> Do this once after cloning. Certs are gitignored, not in the repo.

```bash
cd docker/scripts
chmod +x generate-certs.sh
./generate-certs.sh
```

Expected output:
```
[1/4] Generating CA private key...
[2/4] Generating CA certificate (valid 10 years)...
[3/4] Generating Mosquitto server key + certificate...
[4/4] Cleaning up temporary files...
Certificates ready in .../docker/mosquitto/certs:
  ca.crt     — share with all clients (backend + simulator)
  server.crt — Mosquitto broker certificate
  server.key — Mosquitto private key (keep secret, never commit)
```

Verify the 3 files exist:
```bash
ls docker/mosquitto/certs/
# ca.crt   server.crt   server.key
```

---

## Step 2 — Create Mosquitto password file (one-time only)

> File passwd cũng bị gitignore — phải tạo lại sau mỗi lần clone.
> passwd is also gitignored — must recreate after every clone.

```bash
# Create file with first user
mosquitto_passwd -c -b docker/mosquitto/passwd iot-backend mqtt-backend-secret

# Add second user
mosquitto_passwd -b docker/mosquitto/passwd iot-simulator mqtt-simulator-secret
```

Verify:
```bash
ls docker/mosquitto/passwd
# docker/mosquitto/passwd
```

---

## Step 3 — Create application.properties (one-time only)

> application.properties bị gitignore. Copy từ file example.
> application.properties is gitignored. Copy from the example file.

```bash
cp iot-backend/src/main/resources/application.properties.example \
   iot-backend/src/main/resources/application.properties
```

---

## Step 4 — Set environment variables

### Option A — Terminal (Mac/Linux)

```bash
export MQTT_CA_CERT_PATH="/absolute/path/to/global-ict/docker/mosquitto/certs/ca.crt"
```

Replace with your actual path, for example:
```bash
export MQTT_CA_CERT_PATH="/Users/thanhdo/global-ict/docker/mosquitto/certs/ca.crt"
```

> IMPORTANT: This export only lasts for the current terminal session.
> Run it again every time you open a new terminal before starting the backend.

### Option B — IntelliJ IDEA (recommended)

```
1. Top-right → click run config dropdown → "Edit Configurations..."
2. Left panel → Spring Boot → select your app (iot-backend)
3. Tab "Configuration" → find "Environment variables"
4. Click the folder icon on the right → click "+" → add:

   Name : MQTT_CA_CERT_PATH
   Value: /Users/thanhdo/global-ict/docker/mosquitto/certs/ca.crt

5. Click OK → Apply
```

> Setting it in IntelliJ saves it permanently in the run config — no need to export every time.

---

## Step 5 — Start Docker infrastructure

```bash
cd docker
docker-compose up -d
```

Verify both containers are running:
```bash
docker ps
```

Expected output (2 containers):
```
CONTAINER ID   IMAGE                  PORTS
xxxxxxxxxxxx   eclipse-mosquitto:2    0.0.0.0:1883->1883, 0.0.0.0:8883->8883
xxxxxxxxxxxx   postgres:16            0.0.0.0:5433->5432
```

If Mosquitto is not showing port 8883, restart it:
```bash
docker-compose down && docker-compose up -d
```

---

## Step 6 — Start Backend (Spring Boot)

**Terminal:**
```bash
cd iot-backend
./mvnw spring-boot:run
```

**IntelliJ:**
```
Click the green Run button (make sure MQTT_CA_CERT_PATH is set — Step 4B)
```

Backend is ready when you see:
```
INFO  MqttConfig        - MQTT TLS enabled — broker: ssl://localhost:8883
INFO  o.s.b.w.e.tomcat  - Tomcat started on port 8080
```

If you see this error → go back to Step 4:
```
IllegalStateException: mqtt.broker-url is ssl:// but MQTT_CA_CERT_PATH env var is not set.
```

---

## Step 7 — Start IoT Simulator (Python)

Open a **new terminal**:

```bash
# Install dependency (first time only)
pip install paho-mqtt

# Set env vars
export MQTT_CA_CERT="/Users/thanhdo/global-ict/docker/mosquitto/certs/ca.crt"
export MQTT_USERNAME="iot-simulator"
export MQTT_PASSWORD="mqtt-simulator-secret"

cd iot-simulator
python simulator.py
```

Simulator is working when you see:
```
[PUBLISH] home/esp32-001/data → {"deviceId": "esp32-001", "temperature": 45.0, ...}
[PUBLISH] home/esp32-002/data → {"deviceId": "esp32-002", "temperature": 45.0, ...}
```

---

## Step 8 — Start Frontend (Vue 3)

Open a **new terminal**:

```bash
cd frontend
npm install        # first time only
npm run dev
```

Open browser: `http://localhost:5173`

---

## Verify everything is connected

### 1. Check Mosquitto logs — port 8883 must appear
```bash
docker logs iot-mosquitto
```
Look for:
```
Opening ipv4 listen socket on port 1883
Opening ipv4 listen socket on port 8883
```

### 2. Test TLS connection manually
```bash
mosquitto_pub \
  --cafile docker/mosquitto/certs/ca.crt \
  -h localhost -p 8883 \
  -u iot-backend -P mqtt-backend-secret \
  -t "home/test/data" -m '{"test":true}'
```
No error = TLS is working.

### 3. Check backend received data
```bash
curl http://localhost:8080/api/devices
```
Returns a list of devices = backend + MQTT + DB all connected.

---

## Common errors and fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `IllegalStateException: MQTT_CA_CERT_PATH not set` | Env var missing | Step 4 |
| `MqttException` on startup | Wrong cert path or Mosquitto not running | Check Step 1, 2, 5 |
| `ca.crt not found` | Certs not generated | Step 1 |
| `Connection refused :8883` | Docker not running or certs missing in container | Step 5 |
| Port 8883 not in `docker ps` | docker-compose not restarted after config change | `docker-compose down && up -d` |
| Simulator SSL error | `MQTT_CA_CERT` not set | Export before running simulator |

---

## Summary — what runs where

```
localhost:5173   Vue 3 Frontend
localhost:8080   Spring Boot Backend
localhost:1883   Mosquitto (plain text — temporary, will be removed)
localhost:8883   Mosquitto (TLS — main connection)
localhost:5433   PostgreSQL
```
