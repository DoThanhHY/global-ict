# 🏠 Global ICT — IoT Home Device Management

Hệ thống quản lý thiết bị IoT trong gia đình, bao gồm ESP32 simulator, MQTT broker, REST API backend và dashboard frontend.

## 📐 Architecture

```
[ESP32 / Python Simulator]
        ↓ MQTT publish
[Mosquitto Broker :1883]
        ↓ subscribe
[Spring Boot Backend :8080]
        ├── MQTT Subscriber
        ├── REST API
        └── WebSocket (real-time)
[PostgreSQL :5432]
        ↓
[Vue 3 + Vite Frontend :5173]
```

## 🗂️ Project Structure

```
global-ict/
├── docker/
│   ├── mosquitto/
│   │   └── mosquitto.conf
│   └── docker-compose.yml
├── iot-simulator/
│   └── simulator.py
├── iot-backend/          ← Spring Boot
└── frontend/             ← Vue 3 + Vite + Pinia
```

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Docker Desktop | Latest |
| Java | 21+ |
| Maven | 3.9+ (hoặc dùng `./mvnw`) |
| Node.js | 18+ |
| Python | 3.8+ |

## 🚀 Hướng dẫn chạy

### Bước 1 — Khởi động Infrastructure (Docker)

```bash
cd docker
docker-compose up -d
```

Kiểm tra containers đang chạy:
```bash
docker ps
# Phải thấy: iot-postgres (5432) và iot-mosquitto (1883)
```

> **Lưu ý:** Nếu máy đã có PostgreSQL local đang chạy port 5432, đổi port trong `docker-compose.yml` thành `5433:5432` và cập nhật `application.properties` tương ứng.

---

### Bước 2 — Chạy Backend (Spring Boot)

```bash
cd iot-backend
./mvnw spring-boot:run
```

Backend khởi động tại `http://localhost:8080`

Kiểm tra hoạt động:
```bash
curl http://localhost:8080/api/devices
# Trả về: []
```

---

### Bước 3 — Chạy IoT Simulator (Python)

```bash
# Cài dependency lần đầu
pip install paho-mqtt

# Chạy simulator
cd iot-simulator
python simulator.py
```

Simulator sẽ gửi data lên MQTT broker mỗi 5 giây, giả lập 4 thiết bị:
- `esp32-001` — Cảm biến nhiệt độ/độ ẩm (Phòng khách)
- `esp32-002` — Cảm biến nhiệt độ/độ ẩm (Phòng ngủ)
- `esp32-003` — Cảm biến cửa (Cửa ra vào)
- `esp32-004` — Công tắc đèn (Đèn bếp)

---

### Bước 4 — Chạy Frontend (Vue 3)

```bash
cd frontend

# Cài dependency lần đầu
npm install

# Chạy dev server
npm run dev
```

Truy cập: `http://localhost:5173`

---

## 🔌 MQTT Topic Convention

| Topic | Chiều | Mô tả |
|-------|-------|-------|
| `home/{deviceId}/data` | ESP32 → Broker → Backend | Gửi sensor data lên |
| `home/{deviceId}/command` | Backend → Broker → ESP32 | Gửi lệnh điều khiển xuống |

Ví dụ payload nhiệt độ:
```json
{
  "deviceId": "esp32-001",
  "temperature": 28.5,
  "humidity": 72.3,
  "timestamp": 1718000000000
}
```

---

## 📡 REST API

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/devices` | Lấy danh sách thiết bị |
| POST | `/api/devices` | Thêm thiết bị mới |
| PUT | `/api/devices/{id}` | Cập nhật thiết bị |
| DELETE | `/api/devices/{id}` | Xóa thiết bị |
| POST | `/api/devices/{deviceId}/command` | Gửi lệnh điều khiển |
| GET | `/api/sensor-data/device/{deviceId}` | Lấy lịch sử sensor |
| GET | `/api/sensor-data/stats` | Thống kê dashboard |

---

## 🛠️ Environment Config

### Backend — `iot-backend/src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/iot_db
spring.datasource.username=iot_user
spring.datasource.password=iot_pass
mqtt.broker-url=tcp://localhost:1883
mqtt.client-id=spring-boot-backend
server.port=8080
```

### Frontend — `frontend/.env` (tạo nếu cần đổi URL)
```env
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080/ws
```

---

## 🐛 Troubleshooting

**`role "iot_user" does not exist`**
→ PostgreSQL local đang conflict với Docker. Đổi port Docker thành `5433:5432` hoặc chạy:
```bash
cd docker && docker-compose down -v && docker-compose up -d
```

**`global is not defined` (Frontend)**
→ Thêm vào `vite.config.ts`:
```ts
define: { global: 'globalThis' }
```

**`@tailwindcss/postcss` error**
→ Cài package và cập nhật `postcss.config.js`:
```bash
npm install -D @tailwindcss/postcss
```

**MQTT không nhận data**
→ Kiểm tra Mosquitto đang chạy: `docker ps` và simulator đã `python simulator.py`
