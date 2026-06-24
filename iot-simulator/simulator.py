import paho.mqtt.client as mqtt
import json
import os
import time
import random

BROKER = os.getenv("MQTT_BROKER", "localhost")
PORT = int(os.getenv("MQTT_PORT", "8883"))
MQTT_USERNAME = os.getenv("MQTT_USERNAME", "iot-simulator")
MQTT_PASSWORD = os.getenv("MQTT_PASSWORD", "mqtt-simulator-secret")
CA_CERT = os.getenv("MQTT_CA_CERT", "../docker/mosquitto/certs/ca.crt")

DEVICES = [
    {"id": "esp32-001", "name": "Phòng khách", "type": "TEMPERATURE_HUMIDITY"},
    {"id": "esp32-002", "name": "Phòng ngủ",   "type": "TEMPERATURE_HUMIDITY"},
    {"id": "esp32-003", "name": "Cửa ra vào",  "type": "DOOR_SENSOR"},
    {"id": "esp32-004", "name": "Đèn bếp",     "type": "SWITCH"},
]

client = mqtt.Client(client_id="python-simulator")
client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
client.tls_set(ca_certs=CA_CERT)
client.connect(BROKER, PORT)
client.loop_start()

def publish_data(device: dict):
    topic = f"home/{device['id']}/data"

    if device["type"] == "TEMPERATURE_HUMIDITY":
        payload = {
            "deviceId": device["id"],
            "temperature": round(random.uniform(24.0, 35.0), 1),
            "humidity":    round(random.uniform(50.0, 90.0), 1),
            "timestamp":   int(time.time() * 1000)
        }
    elif device["type"] == "DOOR_SENSOR":
        payload = {
            "deviceId": device["id"],
            "open":      random.choice([True, False]),
            "timestamp": int(time.time() * 1000)
        }
    elif device["type"] == "SWITCH":
        payload = {
            "deviceId": device["id"],
            "on":        random.choice([True, False]),
            "timestamp": int(time.time() * 1000)
        }
    else:
        return

    client.publish(topic, json.dumps(payload))
    print(f"[PUBLISH] {topic} → {payload}")

print("🚀 Simulator started. Sending data every 5s...")

try:
    while True:
        for device in DEVICES:
            publish_data(device)
        time.sleep(5)
except KeyboardInterrupt:
    print("Stopped.")
    client.loop_stop()