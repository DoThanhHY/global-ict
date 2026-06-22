# 📝 Coding Convention — Global ICT IoT Project

## 1. Nguyên tắc chung

- Ưu tiên **rõ ràng hơn ngắn gọn** — code dễ đọc hơn code clever
- Mỗi file/class/function chỉ làm **một việc** (Single Responsibility)
- Không commit code có **TODO/FIXME** chưa xử lý
- Không hardcode giá trị — dùng **constants hoặc config**

---

## 2. Git

### Branch naming
```
feature/{tên-tính-năng}       # feature/mqtt-subscriber
fix/{tên-lỗi}                 # fix/device-online-status
phase/{số-phase}              # phase/2-wokwi-iot
```

### Commit message (Conventional Commits)
```
feat: thêm MQTT subscriber cho sensor data
fix: sửa lỗi role iot_user không tồn tại
refactor: tách DeviceService thành service riêng
chore: cập nhật docker-compose thêm port 5433
docs: thêm README hướng dẫn chạy project
```

### Quy tắc commit
- Viết tiếng Anh hoặc tiếng Việt nhất quán, không trộn lẫn
- Mỗi commit chỉ làm **một việc**
- Không commit thẳng vào `main`

---

## 3. Backend (Spring Boot / Java)

### Naming
| Thành phần | Convention | Ví dụ |
|-----------|-----------|-------|
| Class | PascalCase | `DeviceService`, `MqttConfig` |
| Method | camelCase | `findLatestByDeviceId` |
| Variable | camelCase | `deviceId`, `sensorData` |
| Constant | UPPER_SNAKE_CASE | `MQTT_TOPIC_PREFIX` |
| Package | lowercase | `com.globalict.iot_backend.service` |
| Table DB | snake_case | `sensor_data`, `devices` |
| Column DB | snake_case | `device_id`, `recorded_at` |

### Package structure
```
com.globalict.iot_backend/
├── config/        # Bean config, MQTT, WebSocket, Security
├── controller/    # REST endpoints — không chứa business logic
├── service/       # Business logic
├── repository/    # JPA repositories
├── entity/        # JPA entities
├── dto/           # Request/Response objects
├── exception/     # Custom exceptions
└── util/          # Utility classes
```

### Rules
```java
// ✅ Dùng DTO, không expose entity thẳng ra API
public List<SensorDataResponse> getByDevice(...) { }

// ❌ Không trả entity trực tiếp
public List<SensorData> getByDevice(...) { }

// ✅ Dùng @RequiredArgsConstructor thay @Autowired
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
}

// ✅ Dùng Optional xử lý null
deviceRepository.findByDeviceId(deviceId)
    .orElseThrow(() -> new RuntimeException("Device not found"));

// ✅ Controller chỉ gọi service, không có logic
@GetMapping
public List<DeviceResponse> getAll() {
    return deviceService.findAll();
}

// ✅ Log đầy đủ ở service layer
@Slf4j
public class MqttSubscriberService {
    log.info("MQTT received [{}]: {}", topic, payload);
    log.error("Failed to process message", e);
}
```

### REST API naming
```
GET    /api/devices              # Lấy danh sách
GET    /api/devices/{id}         # Lấy 1 item
POST   /api/devices              # Tạo mới
PUT    /api/devices/{id}         # Cập nhật toàn bộ
PATCH  /api/devices/{id}         # Cập nhật một phần
DELETE /api/devices/{id}         # Xóa
POST   /api/devices/{id}/command # Action đặc biệt
```

---

## 4. Frontend (Vue 3 + TypeScript)

### Naming
| Thành phần | Convention | Ví dụ |
|-----------|-----------|-------|
| Component | PascalCase | `DeviceCard.vue`, `SensorChart.vue` |
| View | PascalCase + suffix View | `DashboardView.vue` |
| Composable | camelCase + prefix use | `useWebSocket.ts` |
| Store | camelCase + suffix Store | `device.store.ts` |
| API file | camelCase + suffix api | `device.api.ts` |
| Variable/function | camelCase | `deviceStore`, `fetchAll` |
| Type/Interface | PascalCase | `Device`, `SensorPayload` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |

### File structure
```
src/
├── api/           # Axios calls — chỉ gọi HTTP, không có logic
├── composables/   # Reusable logic (useWebSocket, useToast...)
├── components/    # Dùng lại được nhiều nơi
├── views/         # Gắn với route, dùng components
├── stores/        # Pinia stores
├── types/         # TypeScript interfaces
├── router/        # Route config
└── utils/         # Helper functions
```

### Rules
```vue
<!-- ✅ Luôn dùng <script setup lang="ts"> -->
<script setup lang="ts">
// ✅ defineProps với TypeScript
const props = defineProps<{ device: Device }>()

// ✅ defineEmits với TypeScript
const emit = defineEmits<{
  (e: 'delete', id: number): void
}>()

// ✅ Không dùng any
const data = ref<Device[]>([])

// ❌ Tránh
const data = ref<any>([])
</script>

<!-- ✅ Key luôn có khi dùng v-for -->
<DeviceCard v-for="device in devices" :key="device.id" />

<!-- ❌ Không dùng v-if và v-for cùng element -->
<div v-for="d in devices" v-if="d.online" />

<!-- ✅ Tách ra -->
<template v-for="d in devices" :key="d.id">
  <div v-if="d.online" />
</template>
```

### Pinia store pattern
```ts
// ✅ Dùng Setup Store (Composition API style)
export const useDeviceStore = defineStore('device', () => {
  const devices = ref<Device[]>([])        // state
  const onlineDevices = computed(...)      // getter
  async function fetchAll() { }           // action
  return { devices, onlineDevices, fetchAll }
})
```

### API layer
```ts
// ✅ API file chỉ gọi HTTP, return data trực tiếp
export const deviceApi = {
  getAll: () => http.get<Device[]>('/devices').then(r => r.data),
}

// ✅ Xử lý error ở store, không ở API
async function fetchAll() {
  loading.value = true
  try {
    devices.value = await deviceApi.getAll()
  } catch (e) {
    console.error('Failed to fetch devices', e)
  } finally {
    loading.value = false
  }
}
```

---

## 5. MQTT

### Topic naming
```
home/{deviceId}/data         # Device gửi data lên
home/{deviceId}/command      # Backend gửi lệnh xuống
home/{deviceId}/status       # Device báo trạng thái online/offline
```

### Payload format (JSON)
```json
{
  "deviceId": "esp32-001",
  "temperature": 28.5,
  "humidity": 72.3,
  "timestamp": 1718000000000
}
```

### Rules
- Timestamp luôn dùng **Unix milliseconds**
- Payload luôn có `deviceId`
- QoS: `1` cho command, `0` cho sensor data thường xuyên

---

## 6. ESP32 / Arduino (C++)

### Naming
```cpp
// Constants: UPPER_SNAKE_CASE
const char* MQTT_BROKER = "broker.hivemq.com";
const int   MQTT_PORT   = 1883;

// Functions: camelCase
void publishSensorData();
void onMessageReceived(char* topic, byte* payload, unsigned int length);

// Variables: camelCase
float temperature = 0.0;
bool  isConnected = false;
```

### Rules
```cpp
// ✅ Tách hàm rõ ràng
void setup() {
  connectWifi();
  connectMqtt();
  setupSensors();
}

void loop() {
  mqttClient.loop();
  publishSensorData();
  delay(5000);
}

// ✅ Log Serial đầy đủ để debug
Serial.println("[MQTT] Connected to broker");
Serial.printf("[SENSOR] Temp: %.1f°C, Humidity: %.1f%%\n", temp, hum);
```

---

## 7. Database

### Naming
```sql
-- Tables: snake_case, số nhiều
CREATE TABLE devices (...);
CREATE TABLE sensor_data (...);
CREATE TABLE alert_thresholds (...);

-- Columns: snake_case
device_id, recorded_at, created_at, is_online

-- Index: idx_{table}_{column}
CREATE INDEX idx_sensor_data_device_id ON sensor_data(device_id);
CREATE INDEX idx_sensor_data_recorded_at ON sensor_data(recorded_at);
```

---

## 8. Environment & Config

```
# ✅ Không commit file chứa secret
.env
application-prod.properties
*.key, *.pem, *.crt

# ✅ Luôn có file .env.example hoặc application.properties.example
mqtt.broker-url=tcp://localhost:1883
mqtt.client-id=spring-boot-backend
jwt.secret=YOUR_SECRET_HERE
```

---

## 9. Security (Phase 7)

- Không log **password, token, private key**
- Validate mọi input từ MQTT payload trước khi lưu DB
- CORS chỉ cho phép domain cụ thể, không dùng `*` trên production
- JWT secret tối thiểu **256-bit**, lưu trong env variable
- MQTT credentials không hardcode trong firmware — dùng `#define` từ file config riêng
