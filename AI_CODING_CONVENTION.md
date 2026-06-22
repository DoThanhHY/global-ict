# 🤖 AI Coding Convention — Global ICT IoT Project

Tài liệu này định nghĩa các quy tắc khi sử dụng AI (Claude, Copilot, Cursor...) để generate code trong project. Mục tiêu: AI output phải nhất quán với codebase, không tạo ra technical debt.

---

## 1. Nguyên tắc chung

- AI **không được tự ý chọn tech stack mới** — chỉ dùng những gì đã có trong project
- AI phải tuân theo `CODING_CONVENTION.md` — naming, structure, patterns
- Mọi code do AI generate phải được **review trước khi commit**
- Không accept code AI nếu chưa hiểu nó làm gì
- AI sinh ra **code đủ dùng**, không over-engineer

---

## 2. Tech Stack được phép dùng

### Backend
```
✅ Spring Boot 3+
✅ Java 21+
✅ Spring Data JPA + Hibernate
✅ Eclipse Paho MQTT
✅ Spring WebSocket + STOMP
✅ Lombok
✅ PostgreSQL
❌ Không dùng thêm: Kafka, Redis, GraphQL, R2DBC... nếu chưa được approve
```

### Frontend
```
✅ Vue 3 (Composition API + <script setup>)
✅ TypeScript
✅ Pinia
✅ Vue Router
✅ Axios
✅ Tailwind CSS v4
✅ @stomp/stompjs + sockjs-client
❌ Không dùng thêm: Vuex, Options API, jQuery, lodash... nếu chưa được approve
```

### IoT / Firmware
```
✅ Arduino C++ cho ESP32
✅ PubSubClient (MQTT)
✅ DHT sensor library
✅ ArduinoJson
❌ Không dùng thêm framework lạ nếu chưa được approve
```

---

## 3. Prompt Rules — Cách yêu cầu AI

### Luôn cung cấp context đủ
```
# ✅ Prompt tốt
"Trong project Spring Boot 3 + Java 21, dùng Lombok và JPA.
Viết DeviceService với method markOnline(String deviceId)
— nếu device chưa có trong DB thì tự tạo mới (auto-register),
nếu có rồi thì update online=true và lastSeen=now().
Dùng @RequiredArgsConstructor, không dùng @Autowired."

# ❌ Prompt tệ
"Viết service để update device status"
```

### Chỉ định rõ pattern muốn dùng
```
# ✅
"Viết Pinia store theo Setup Store style (không dùng Options Store),
có state devices, getter onlineDevices, action fetchAll với loading state."

# ❌
"Viết store quản lý devices"
```

### Yêu cầu AI không tự thêm dependency
```
# ✅ Thêm vào cuối mọi prompt backend
"Chỉ dùng các dependency đã có trong pom.xml, không import thêm thư viện mới."

# ✅ Thêm vào cuối mọi prompt frontend
"Chỉ dùng các package đã có trong package.json, không import thêm thư viện mới."
```

---

## 4. Rules cho từng layer

### 4.1 Entity
```
Yêu cầu AI khi generate entity:
- Dùng @Data @Builder @NoArgsConstructor @AllArgsConstructor (Lombok)
- Dùng @Column(name = "snake_case") explicit
- Enum define trong cùng file entity
- Không dùng @Data cho entity có quan hệ 2 chiều (dùng @Getter @Setter thay thế)
- Timestamp dùng LocalDateTime, không dùng Date hay Instant
```

### 4.2 Repository
```
Yêu cầu AI khi generate repository:
- Extend JpaRepository<Entity, Long>
- Custom query dùng @Query JPQL, không dùng native SQL trừ khi cần thiết
- Pageable import từ org.springframework.data.domain (KHÔNG phải java.awt.print)
- Method name theo Spring Data convention khi có thể
```

### 4.3 Service
```
Yêu cầu AI khi generate service:
- @Service + @RequiredArgsConstructor + @Slf4j
- Không inject repository thẳng vào controller — phải qua service
- Business logic ở service, không ở controller
- Throw exception rõ ràng thay vì return null
- Dùng @Transactional khi cần
```

### 4.4 Controller
```
Yêu cầu AI khi generate controller:
- @RestController + @RequestMapping + @RequiredArgsConstructor + @CrossOrigin
- Chỉ gọi service, không có logic
- Trả DTO, không trả Entity
- Dùng ResponseEntity khi cần control HTTP status code
```

### 4.5 Vue Component
```
Yêu cầu AI khi generate component:
- <script setup lang="ts"> — không dùng Options API
- defineProps<Type>() với TypeScript generic
- defineEmits<{ (e: 'event', payload: Type): void }>()
- Không dùng any
- Không dùng v-if và v-for trên cùng element
- Key luôn có khi dùng v-for
- Tailwind classes, không viết inline style
```

### 4.6 Pinia Store
```
Yêu cầu AI khi generate store:
- Setup Store style (không phải Options Store)
- ref() cho state, computed() cho getter, async function cho action
- Loading state cho mọi async action
- Export store name rõ ràng: useDeviceStore, useSensorDataStore
```

### 4.7 MQTT / IoT
```
Yêu cầu AI khi generate firmware:
- Topic format: home/{deviceId}/data hoặc home/{deviceId}/command
- Payload luôn là JSON với field deviceId và timestamp (Unix ms)
- Tách hàm: connectWifi(), connectMqtt(), publishData(), onMessage()
- Serial.print đầy đủ để debug
- Không hardcode credentials
```

---

## 5. Những gì AI không được làm

```
❌ Tạo file mới ngoài package structure đã định nghĩa
❌ Thêm dependency vào pom.xml hoặc package.json mà không hỏi
❌ Dùng @Autowired (dùng constructor injection qua Lombok)
❌ Dùng Options API trong Vue
❌ Trả Entity thẳng từ controller (phải dùng DTO)
❌ Hardcode URL, port, credentials trong code
❌ Dùng System.out.println() trong Java (dùng @Slf4j log)
❌ Bỏ qua error handling — mọi async đều phải có try/catch
❌ Generate migration SQL tự động mà không review
❌ Xóa hoặc sửa code không liên quan đến yêu cầu
```

---

## 6. Checklist review code AI

Trước khi accept code AI generate, kiểm tra:

```
[ ] Import đúng package (đặc biệt Pageable — phải là org.springframework.data.domain)
[ ] Không có dependency mới lạ
[ ] Naming theo convention (camelCase, PascalCase, snake_case đúng chỗ)
[ ] Không có hardcode value (URL, port, secret)
[ ] Có error handling
[ ] Có log đầy đủ ở service layer
[ ] DTO được dùng thay vì Entity ở API response
[ ] TypeScript types đầy đủ, không có any
[ ] Vue component dùng <script setup lang="ts">
[ ] Không có console.log() bị bỏ quên trong FE production code
```

---

## 7. Template Prompt chuẩn

### Backend — Service method
```
Context:
- Spring Boot 3, Java 21, Lombok, JPA
- Package: com.globalict.iot_backend.service
- Chỉ dùng dependency đã có trong pom.xml

Yêu cầu:
[MÔ TẢ YÊU CẦU CỤ THỂ]

Rules:
- @RequiredArgsConstructor, @Slf4j
- Throw exception thay vì return null
- Dùng DTO ở return type nếu expose ra ngoài
```

### Frontend — Vue Component
```
Context:
- Vue 3 + TypeScript + Tailwind CSS v4
- Pinia store đã có: useDeviceStore
- Chỉ dùng package đã có trong package.json

Yêu cầu:
[MÔ TẢ YÊU CẦU CỤ THỂ]

Rules:
- <script setup lang="ts">
- Không dùng any
- Tailwind classes, không inline style
- defineProps và defineEmits với TypeScript
```

### ESP32 Firmware
```
Context:
- Arduino C++ cho ESP32 trên Wokwi
- Thư viện: PubSubClient, DHT, ArduinoJson
- MQTT broker: broker.hivemq.com:1883
- Topic: home/{deviceId}/data

Yêu cầu:
[MÔ TẢ YÊU CẦU CỤ THỂ]

Rules:
- Tách hàm rõ ràng
- Serial.print để debug
- Payload JSON có deviceId và timestamp
```
