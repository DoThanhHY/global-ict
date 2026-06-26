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

# Store device states in memory so commands can mutate them
device_states = {d["id"]: {} for d in DEVICES}

client = mqtt.Client(client_id="python-simulator")
client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
client.tls_set(ca_certs=CA_CERT)
client.connect(BROKER, PORT)
client.loop_start()

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[MQTT] Connected to broker")
        client.subscribe("home/+/command")
        print("[MQTT] Subscribed to home/+/command")
    else:
        print(f"[MQTT] Connection failed, rc={rc}")

def on_message(client, userdata, msg):
    topic = msg.topic
    try:
        payload = json.loads(msg.payload.decode())
        device_id = payload.get("deviceId", topic.split("/")[1])
        action = payload.get("action")
        print(f"[COMMAND] Received on {topic}: action={action}, deviceId={device_id}")

        device = next((d for d in DEVICES if d["id"] == device_id), None)
        if device is None:
            print(f"[COMMAND] Unknown device: {device_id}")
            return

        if device["type"] == "SWITCH":
            if action == "on":
                device_states[device_id]["on"] = True
            elif action == "off":
                device_states[device_id]["on"] = False
            else:
                print(f"[COMMAND] Unknown action '{action}' for SWITCH device")
                return

            data_payload = {
                "deviceId": device_id,
                "on": device_states[device_id]["on"],
                "timestamp": int(time.time() * 1000)
            }
            data_topic = f"home/{device_id}/data"
            client.publish(data_topic, json.dumps(data_payload))
            print(f"[COMMAND] Updated state -> published to {data_topic}: {data_payload}")

        elif device["type"] == "DOOR_SENSOR":
            if action in ("open", "close"):
                device_states[device_id]["open"] = (action == "open")
                data_payload = {
                    "deviceId": device_id,
                    "open": device_states[device_id]["open"],
                    "timestamp": int(time.time() * 1000)
                }
                data_topic = f"home/{device_id}/data"
                client.publish(data_topic, json.dumps(data_payload))
                print(f"[COMMAND] Updated state -> published to {data_topic}: {data_payload}")
            else:
                print(f"[COMMAND] Unknown action '{action}' for DOOR_SENSOR")

        elif device["type"] == "TEMPERATURE_HUMIDITY":
            print(f"[COMMAND] TEMPERATURE_HUMIDITY devices ignore commands")

    except Exception as e:
        print(f"[COMMAND] Error processing message: {e}")

client.on_connect = on_connect
client.on_message = on_message

def publish_data(device: dict):
    topic = f"home/{device['id']}/data"
    state = device_states[device["id"]]

    if device["type"] == "TEMPERATURE_HUMIDITY":
        payload = {
            "deviceId": device["id"],
            "temperature": round(random.uniform(24.0, 35.0), 1),
            "humidity":    round(random.uniform(50.0, 90.0), 1),
            "timestamp":   int(time.time() * 1000)
        }
    elif device["type"] == "DOOR_SENSOR":
        open_state = state.get("open", random.choice([True, False]))
        payload = {
            "deviceId": device["id"],
            "open":      open_state,
            "timestamp": int(time.time() * 1000)
        }
        device_states[device["id"]]["open"] = open_state
    elif device["type"] == "SWITCH":
        on_state = state.get("on", random.choice([True, False]))
        payload = {
            "deviceId": device["id"],
            "on":        on_state,
            "timestamp": int(time.time() * 1000)
        }
        device_states[device["id"]]["on"] = on_state
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