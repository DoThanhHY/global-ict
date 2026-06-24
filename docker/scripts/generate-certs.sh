#!/bin/bash
# Generates a self-signed CA and Mosquitto server certificate for TLS on port 8883.
# Run once before docker-compose up. Output goes to docker/mosquitto/certs/ (gitignored).

set -e

CERTS_DIR="$(dirname "$0")/../mosquitto/certs"
mkdir -p "$CERTS_DIR"

echo "[1/4] Generating CA private key..."
openssl genrsa -out "$CERTS_DIR/ca.key" 2048

echo "[2/4] Generating CA certificate (valid 10 years)..."
openssl req -new -x509 -days 3650 \
  -key "$CERTS_DIR/ca.key" \
  -out "$CERTS_DIR/ca.crt" \
  -subj "/CN=IoT-CA/O=GlobalICT/C=VN"

echo "[3/4] Generating Mosquitto server key + certificate..."
openssl genrsa -out "$CERTS_DIR/server.key" 2048
openssl req -new \
  -key "$CERTS_DIR/server.key" \
  -out "$CERTS_DIR/server.csr" \
  -subj "/CN=localhost/O=GlobalICT/C=VN"
openssl x509 -req -days 3650 \
  -in "$CERTS_DIR/server.csr" \
  -CA "$CERTS_DIR/ca.crt" \
  -CAkey "$CERTS_DIR/ca.key" \
  -CAcreateserial \
  -out "$CERTS_DIR/server.crt"

echo "[4/4] Cleaning up temporary files..."
rm -f "$CERTS_DIR/server.csr" "$CERTS_DIR/ca.srl"

echo ""
echo "Certificates ready in $CERTS_DIR:"
echo "  ca.crt     — share with all clients (backend + simulator)"
echo "  server.crt — Mosquitto broker certificate"
echo "  server.key — Mosquitto private key (keep secret, never commit)"
