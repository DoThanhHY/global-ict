package com.globalict.iot_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.ca-cert-path:}")
    private String caCertPath;

    @Bean
    public MqttClient mqttClient() throws Exception {
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        if (brokerUrl.startsWith("ssl://")) {
            if (caCertPath == null || caCertPath.isBlank()) {
                throw new IllegalStateException(
                    "mqtt.broker-url is ssl:// but MQTT_CA_CERT_PATH env var is not set. " +
                    "Set it to the absolute path of docker/mosquitto/certs/ca.crt"
                );
            }
            options.setSocketFactory(buildSslContext(caCertPath).getSocketFactory());
            log.info("MQTT TLS enabled — broker: {}", brokerUrl);
        }

        client.connect(options);
        return client;
    }

    private SSLContext buildSslContext(String caCertPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (FileInputStream fis = new FileInputStream(caCertPath)) {
            caCert = (X509Certificate) cf.generateCertificate(fis);
        }
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
}
