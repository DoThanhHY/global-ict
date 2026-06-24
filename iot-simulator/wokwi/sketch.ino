/**
 * Global ICT — IoT Home Device Management
 * Phase 2 — Wokwi ESP32 + DHT22 Firmware
 *
 * Chức năng:
 * - Đọc nhiệt độ/độ ẩm từ DHT22
 * - Publish data lên MQTT broker (HiveMQ public) theo định kỳ
 * - Subscribe topic command để nhận lệnh bật/tắt LED
 *
 * Topic convention (theo CODING_CONVENTION.md):
 *   home/{deviceId}/data     -> publish sensor data
 *   home/{deviceId}/command  -> subscribe để nhận lệnh điều khiển
 *   home/{deviceId}/status   -> publish trạng thái online/offline
 *
 * Thư viện dùng: PubSubClient, DHT sensor library, ArduinoJson
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <ArduinoJson.h>
#include "secrets.h"

// ===== Pin config =====
#define DHT_PIN     15
#define DHT_TYPE    DHT22
#define LED_PIN     2

// ===== Timing config =====
const unsigned long PUBLISH_INTERVAL_MS = 5000;
const unsigned long MQTT_RECONNECT_DELAY_MS = 5000;

// ===== MQTT topics (build từ DEVICE_ID trong secrets.h) =====
String topicData    = String("home/") + DEVICE_ID + "/data";
String topicCommand = String("home/") + DEVICE_ID + "/command";
String topicStatus  = String("home/") + DEVICE_ID + "/status";

// ===== Global objects =====
DHT dht(DHT_PIN, DHT_TYPE);
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

unsigned long lastPublishAt = 0;
bool ledState = false;

// ===== Forward declarations =====
void connectWifi();
void connectMqtt();
void reconnectMqttIfNeeded();
void publishSensorData();
void publishStatus(bool online);
void onMqttMessage(char* topic, byte* payload, unsigned int length);
void handleCommand(const String& payload);

void setup() {
  Serial.begin(115200);
  delay(200);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  dht.begin();

  connectWifi();

  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(onMqttMessage);
  connectMqtt();
}

void loop() {
  reconnectMqttIfNeeded();
  mqttClient.loop();

  unsigned long now = millis();
  if (now - lastPublishAt >= PUBLISH_INTERVAL_MS) {
    lastPublishAt = now;
    publishSensorData();
  }
}

// ===== WiFi =====
void connectWifi() {
  Serial.printf("[WIFI] Connecting to %s...\n", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("[WIFI] Connected. IP address: ");
  Serial.println(WiFi.localIP());
}

// ===== MQTT =====
void connectMqtt() {
  while (!mqttClient.connected()) {
    String clientId = String(DEVICE_ID) + "-" + String(random(0xffff), HEX);
    Serial.printf("[MQTT] Connecting to %s:%d as %s...\n", MQTT_BROKER, MQTT_PORT, clientId.c_str());

    bool connected;
    if (strlen(MQTT_USERNAME) > 0) {
      connected = mqttClient.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD);
    } else {
      connected = mqttClient.connect(clientId.c_str());
    }

    if (connected) {
      Serial.println("[MQTT] Connected to broker");
      mqttClient.subscribe(topicCommand.c_str());
      Serial.printf("[MQTT] Subscribed to %s\n", topicCommand.c_str());
      publishStatus(true);
    } else {
      Serial.printf("[MQTT] Connect failed, rc=%d. Retrying in %lu ms\n",
                    mqttClient.state(), MQTT_RECONNECT_DELAY_MS);
      delay(MQTT_RECONNECT_DELAY_MS);
    }
  }
}

void reconnectMqttIfNeeded() {
  if (!mqttClient.connected()) {
    Serial.println("[MQTT] Connection lost. Reconnecting...");
    connectMqtt();
  }
}

// ===== Publish sensor data =====
void publishSensorData() {
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("[SENSOR] Failed to read from DHT22 sensor");
    return;
  }

  StaticJsonDocument<200> doc;
  doc["deviceId"] = DEVICE_ID;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["timestamp"] = (uint64_t)millis() + (uint64_t)0; // Unix ms giả lập, backend sẽ override bằng thời gian nhận thực tế nếu cần

  char buffer[200];
  size_t n = serializeJson(doc, buffer);

  mqttClient.publish(topicData.c_str(), buffer, n);

  Serial.printf("[SENSOR] Temp: %.1f°C, Humidity: %.1f%% -> published to %s\n",
                temperature, humidity, topicData.c_str());
}

// ===== Publish status (online/offline) =====
void publishStatus(bool online) {
  StaticJsonDocument<150> doc;
  doc["deviceId"] = DEVICE_ID;
  doc["online"] = online;
  doc["timestamp"] = (uint64_t)millis();

  char buffer[150];
  size_t n = serializeJson(doc, buffer);

  mqttClient.publish(topicStatus.c_str(), (const uint8_t*)buffer, n, true); // retain = true
  Serial.printf("[STATUS] Published online=%s to %s\n", online ? "true" : "false", topicStatus.c_str());
}

// ===== MQTT message callback =====
void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String message;
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  Serial.printf("[MQTT] Message received [%s]: %s\n", topic, message.c_str());

  if (String(topic) == topicCommand) {
    handleCommand(message);
  }
}

// ===== Xử lý command bật/tắt LED =====
// Payload mong đợi: {"deviceId":"esp32-001","action":"led","value":"on"}  (value: "on" | "off")
void handleCommand(const String& payload) {
  StaticJsonDocument<200> doc;
  DeserializationError error = deserializeJson(doc, payload);

  if (error) {
    Serial.printf("[COMMAND] Failed to parse JSON: %s\n", error.c_str());
    return;
  }

  const char* action = doc["action"];
  const char* value = doc["value"];

  if (action == nullptr || value == nullptr) {
    Serial.println("[COMMAND] Missing 'action' or 'value' field");
    return;
  }

  if (String(action) == "led") {
    ledState = (String(value) == "on");
    digitalWrite(LED_PIN, ledState ? HIGH : LOW);
    Serial.printf("[COMMAND] LED set to %s\n", ledState ? "ON" : "OFF");
  } else {
    Serial.printf("[COMMAND] Unknown action: %s\n", action);
  }
}
