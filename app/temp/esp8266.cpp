#include <ESP8266WiFi.h>
#include <PubSubClient.h>

// --------- CẤU HÌNH WIFI ----------
const char* ssid = "38 Tan Lac";
const char* password = "0366091667";

// --------- CẤU HÌNH MQTT (HiveMQ) ----------
const char* mqtt_server = "1fbec7bb18444301835d103a20af6aea.s1.eu.hivemq.cloud";
const int mqtt_port = 8883; 
const char* mqtt_user = "Project2";
const char* mqtt_pass = "LinhLongKhiemIT2";

// --------- CHỦ ĐỀ MQTT (TOPIC) ----------
const char* topic_do_am = "vuon/do_am";          // Gửi độ ẩm lên App
const char* topic_trang_thai = "vuon/may_bom";   // Gửi trạng thái bơm (Đang chạy/Đã tắt)
const char* topic_lenh = "vuon/may_bom/lenh";    // NHẬN lệnh từ App (Topic mới)

// --------- KHAI BÁO CHÂN ----------
const int sensorPin = A0; 
const int relayPin = 5;   // D1

// --------- BIẾN SỐ ----------
unsigned long lastMsg = 0; 

WiFiClientSecure espClient;
PubSubClient client(espClient);

// --- 1. HÀM XỬ LÝ KHI NHẬN TIN NHẮN TỪ APP (QUAN TRỌNG) ---
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Nhan tin nhan tu topic: ");
  Serial.print(topic);
  Serial.print(". Noi dung: ");

  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  Serial.println(message);

  // Kiểm tra nếu đúng là topic lệnh
  if (String(topic) == topic_lenh) {
    if (message == "ON") {
      digitalWrite(relayPin, HIGH); // Bật Relay (Kiểm tra mức High/Low của mạch bạn)
      client.publish(topic_trang_thai, "BOM DANG CHAY"); // Báo lại cho App biết
      Serial.println("-> Da thuc hien lenh BAT");
    } 
    else if (message == "OFF") {
      digitalWrite(relayPin, LOW);  // Tắt Relay
      client.publish(topic_trang_thai, "DA TAT BOM");
      Serial.println("-> Da thuc hien lenh TAT");
    }
  }
}

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Dang ket noi Wifi: ");
  Serial.println(ssid);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWifi Connected!");
  espClient.setInsecure(); 
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Dang ket noi MQTT...");
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    
    if (client.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
      Serial.println("Thanh cong!");
      client.publish(topic_trang_thai, "He thong Online");
      
      // --- 2. ĐĂNG KÝ NHẬN LỆNH ---
      client.subscribe(topic_lenh); 
      Serial.println("Da dang ky topic: vuon/may_bom/lenh");
      
    } else {
      Serial.print("That bai, rc=");
      Serial.print(client.state());
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(relayPin, OUTPUT);
  pinMode(sensorPin, INPUT);
  digitalWrite(relayPin, LOW); 

  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  
  // --- 3. CÀI ĐẶT HÀM CALLBACK ---
  client.setCallback(callback); 
}

void loop() {
  // 1. Giữ kết nối MQTT luôn thông suốt để nhận lệnh
  if (!client.connected()) {
    reconnect();
  }
  client.loop(); 

  // 2. Đọc và gửi dữ liệu mỗi 2 giây
  unsigned long now = millis();
  if (now - lastMsg > 2000) {
    lastMsg = now;
    
    int sensorValue = analogRead(sensorPin); // Đọc giá trị ngầm

    // --- TÍNH TOÁN PHẦN TRĂM ---
    // 1023 là khô, 400 là ướt (bạn chỉnh lại số này nếu thấy chưa chuẩn)
    int phanTram = map(sensorValue, 1023, 400, 0, 100);
    
    // Giới hạn số % chỉ từ 0 đến 100
    if (phanTram < 0) phanTram = 0;
    if (phanTram > 100) phanTram = 100;

    // --- CHỈ HIỂN THỊ PHẦN TRĂM LÊN SERIAL MONITOR ---
    Serial.print("Do am dat: ");
    Serial.print(phanTram);
    Serial.println("%");

    // Gửi lên App điện thoại
    String doAmStr = String(phanTram); 
    client.publish(topic_do_am, doAmStr.c_str());
  }
}