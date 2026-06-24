// secrets.h.example
// Copy file này thành secrets.h và điền giá trị thật.
// secrets.h KHÔNG được commit lên git (đã thêm vào .gitignore).

#ifndef SECRETS_H
#define SECRETS_H

// ===== WiFi =====
// Trên Wokwi simulator, dùng network mặc định "Wokwi-GUEST" (không cần password)
#define WIFI_SSID     "Wokwi-GUEST"
#define WIFI_PASSWORD ""

// ===== MQTT Broker =====
#define MQTT_BROKER   "broker.hivemq.com"
#define MQTT_PORT     1883

// Nếu broker yêu cầu auth (chưa cần ở Phase 2, để trống)
#define MQTT_USERNAME ""
#define MQTT_PASSWORD ""

// ===== Device =====
#define DEVICE_ID     "esp32-001"

#endif
