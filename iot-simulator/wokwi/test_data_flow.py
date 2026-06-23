"""
Test script — giả lập phía Backend để verify data flow:
ESP32 (Wokwi) --MQTT--> broker.hivemq.com --> script này

Dùng để test trước khi Backend (Spring Boot) thật được viết.
Tương ứng task: "Test data flow Wokwi → Backend" 

Cài đặt:
    pip install paho-mqtt

Chạy:
    python test_data_flow.py
"""

import json
import time
from datetime import datetime

import paho.mqtt.client as mqtt

MQTT_BROKER = "broker.hivemq.com"
MQTT_PORT = 1883
DEVICE_ID = "esp32-001"

TOPIC_DATA = f"home/{DEVICE_ID}/data"
TOPIC_STATUS = f"home/{DEVICE_ID}/status"
TOPIC_COMMAND = f"home/{DEVICE_ID}/command"


def on_connect(client, userdata, flags, reason_code, properties=None):
    print(f"[MQTT] Connected (reason_code={reason_code})")
    client.subscribe(TOPIC_DATA)
    client.subscribe(TOPIC_STATUS)
    print(f"[MQTT] Subscribed to {TOPIC_DATA}")
    print(f"[MQTT] Subscribed to {TOPIC_STATUS}")


def on_message(client, userdata, msg):
    now = datetime.now().strftime("%H:%M:%S")
    try:
        payload = json.loads(msg.payload.decode())
    except json.JSONDecodeError:
        print(f"[{now}] [WARN] Payload không phải JSON hợp lệ: {msg.payload}")
        return

    if msg.topic == TOPIC_DATA:
        required = {"deviceId", "temperature", "humidity", "timestamp"}
        missing = required - payload.keys()
        if missing:
            print(f"[{now}] [DATA] ❌ Thiếu field: {missing} | payload={payload}")
        else:
            print(
                f"[{now}] [DATA] ✅ device={payload['deviceId']} "
                f"temp={payload['temperature']}°C hum={payload['humidity']}% "
                f"ts={payload['timestamp']}"
            )
    elif msg.topic == TOPIC_STATUS:
        print(f"[{now}] [STATUS] device={payload.get('deviceId')} online={payload.get('online')}")


def send_test_command(client, action: str, value: str):
    """Gửi thử 1 command xuống ESP32 để test LED — dùng cho task 'ESP32 + LED nhận command'."""
    payload = {
        "deviceId": DEVICE_ID,
        "action": action,
        "value": value,
        "timestamp": int(time.time() * 1000),
    }
    client.publish(TOPIC_COMMAND, json.dumps(payload))
    print(f"[COMMAND] Sent {payload} to {TOPIC_COMMAND}")


def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
    client.loop_start()

    print("Đang lắng nghe data từ ESP32... (Ctrl+C để dừng)")
    print("Gõ 'on' hoặc 'off' rồi Enter để test bật/tắt LED, 'q' để thoát.\n")

    try:
        while True:
            cmd = input().strip().lower()
            if cmd in ("on", "off"):
                send_test_command(client, "led", cmd)
            elif cmd == "q":
                break
    except KeyboardInterrupt:
        pass
    finally:
        client.loop_stop()
        client.disconnect()
        print("Đã dừng.")


if __name__ == "__main__":
    main()
