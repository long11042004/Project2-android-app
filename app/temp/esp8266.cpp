#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <DHT.h> 

// --------- 1. CẤU HÌNH WIFI & SERVER ---------
const char* ssid = "38 Tan Lac";       
const char* password = "0366091667";     

const char* mqtt_server = "1fbec7bb18444301835d103a20af6aea.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_user = "Project2";
const char* mqtt_pass = "LinhLongKhiemIT2";

// --------- 2. CÁC TOPIC MQTT (ĐÃ SỬA CHO KHỚP ANDROID) ---------
const char* topic_dat = "vuon/do_am";            // [ĐÃ SỬA] Khớp với Android
const char* topic_trang_thai = "vuon/may_bom";   // [ĐÃ SỬA] Khớp với Android

const char* topic_nhiet = "vuon/nhiet_do";       // Giữ nguyên
const char* topic_kk = "vuon/do_am_kk";          // Giữ nguyên
const char* topic_lenh = "vuon/may_bom/lenh";    // Giữ nguyên

// --------- 3. KHAI BÁO CHÂN ---------
#define DHTPIN 4      // D2
#define DHTTYPE DHT22 
DHT dht(DHTPIN, DHTTYPE);

const int sensorPin = A0; 
const int relayPin = 5;   // D1

// --------- 4. CÀI ĐẶT NGƯỠNG ---------
int nguongKho = 40;     
int nguongMua = 90;      

bool cheDoTuDong = true;  
unsigned long lastMsg = 0;

WiFiClientSecure espClient;
PubSubClient client(espClient);

// --- HÀM XỬ LÝ LỆNH TỪ APP ---
void callback(char* topic, byte* payload, unsigned int length) {
  String message = "";
  for (int i = 0; i < length; i++) message += (char)payload[i];
  Serial.print("Lenh nhan duoc: "); Serial.println(message);

  if (String(topic) == topic_lenh) {
    if (message == "ON") {
      cheDoTuDong = false; 
      digitalWrite(relayPin, HIGH);
      client.publish(topic_trang_thai, "THU CONG: DANG BAT"); // Gửi về topic mới
    } 
    else if (message == "OFF") {
      cheDoTuDong = false; 
      digitalWrite(relayPin, LOW);
      client.publish(topic_trang_thai, "THU CONG: DA TAT");
    }
    else if (message == "AUTO") {
      cheDoTuDong = true;  
      client.publish(topic_trang_thai, "DA BAT CHE DO TU DONG");
    }
  }
}

void setup_wifi() {
  delay(10);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500); Serial.print(".");
  }
  espClient.setInsecure();
}

void reconnect() {
  while (!client.connected()) {
    String clientId = "ESP8266-" + String(random(0xffff), HEX);
    if (client.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
      client.subscribe(topic_lenh);
      client.publish(topic_trang_thai, "He thong Online");
    } else {
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, LOW); 
  
  dht.begin(); 
  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop();

  unsigned long now = millis();
  if (now - lastMsg > 3000) { 
    lastMsg = now;

    // 1. Đọc dữ liệu
    float h = dht.readHumidity();    
    float t = dht.readTemperature(); 
    int datRaw = analogRead(sensorPin);
    int datPercent = map(datRaw, 1023, 400, 0, 100); 
    if (datPercent < 0) datPercent = 0;
    if (datPercent > 100) datPercent = 100;

    if (isnan(h) || isnan(t)) {
      Serial.println("Loi doc DHT22!");
      return;
    }

    // 2. Gửi dữ liệu (Topic đã được sửa ở trên)
    client.publish(topic_dat, String(datPercent).c_str());
    client.publish(topic_nhiet, String(t).c_str());
    client.publish(topic_kk, String(h).c_str());

    Serial.print("Dat: "); Serial.print(datPercent); Serial.print("% | ");
    Serial.print("KK: "); Serial.print(h); Serial.print("% | ");
    Serial.print("Nhiet: "); Serial.print(t); Serial.println("C");

    // 3. LOGIC TỰ ĐỘNG
    if (cheDoTuDong == true) {
      if (datPercent < nguongKho) {
        if (h < nguongMua) {
          digitalWrite(relayPin, HIGH);
          client.publish(topic_trang_thai, "AUTO: DANG TUOI");
        } else {
          digitalWrite(relayPin, LOW);
          client.publish(topic_trang_thai, "AUTO: MUA -> KHONG TUOI");
        }
      } else {
        digitalWrite(relayPin, LOW);
        client.publish(topic_trang_thai, "AUTO: DAT DU AM");
      }
    }
  }
}