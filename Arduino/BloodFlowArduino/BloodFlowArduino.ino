#include <Wire.h>
#include "MAX30100_PulseOximeter.h"

#include "Wire.h"
#include "Adafruit_GFX.h"
#include "OakOLED.h"
#define REPORTING_PERIOD_MS 1000
OakOLED oled;

PulseOximeter pox;

uint32_t tsLastReport = 0;

const unsigned char bitmap [] PROGMEM= {
0x00, 0x00, 0x00, 0x00, 0x01, 0x80, 0x18, 0x00, 0x0f, 0xe0, 0x7f, 0x00, 0x3f, 0xf9, 0xff, 0xc0,
0x7f, 0xf9, 0xff, 0xc0, 0x7f, 0xff, 0xff, 0xe0, 0x7f, 0xff, 0xff, 0xe0, 0xff, 0xff, 0xff, 0xf0,
0xff, 0xf7, 0xff, 0xf0, 0xff, 0xe7, 0xff, 0xf0, 0xff, 0xe7, 0xff, 0xf0, 0x7f, 0xdb, 0xff, 0xe0,
0x7f, 0x9b, 0xff, 0xe0, 0x00, 0x3b, 0xc0, 0x00, 0x3f, 0xf9, 0x9f, 0xc0, 0x3f, 0xfd, 0xbf, 0xc0,
0x1f, 0xfd, 0xbf, 0x80, 0x0f, 0xfd, 0x7f, 0x00, 0x07, 0xfe, 0x7e, 0x00, 0x03, 0xfe, 0xfc, 0x00,
0x01, 0xff, 0xf8, 0x00, 0x00, 0xff, 0xf0, 0x00, 0x00, 0x7f, 0xe0, 0x00, 0x00, 0x3f, 0xc0, 0x00,
0x00, 0x0f, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

void onBeatDetected() {
  Serial.println("Beat!");
  oled.drawBitmap( 60, 20, bitmap, 28, 28, 1);
  oled.display();
}

float avgBPM;
float avgSpo2;
int counter = 0;

void setup() {
  Serial.begin(9600);

  oled.begin();
  oled.clearDisplay();
  oled.setTextSize(1);
  oled.setTextColor(1);
  oled.setCursor(0, 0);

  oled.println("Initializing pulse oximeter..");
  oled.display();
  Serial.print("Initializing pulse oximeter..");

  if (!pox.begin()) {
    Serial.println("FAILED");
    oled.clearDisplay();
    oled.setTextSize(1);
    oled.setTextColor(1);
    oled.setCursor(0, 0);
    oled.println("FAILED");
    oled.display();
    for(;;);
  } else {
    Serial.println("SUCCESS");
    oled.clearDisplay();
    oled.setTextSize(1);
    oled.setTextColor(1);
    oled.setCursor(0, 0);
    oled.println("SUCCESS");
    oled.display();
  }
  pox.setOnBeatDetectedCallback(onBeatDetected);
}

void loop() {
  if (counter < 25) {
    pox.update();
    
    if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
      Serial.print("Heart BPM:");
      Serial.print((int)pox.getHeartRate());
      Serial.print("-----");
      Serial.print("Oxygen Percent:");
      Serial.print((int)pox.getSpO2());
      Serial.print("*");
      Serial.println("\n");
      
      oled.clearDisplay();
      oled.setTextSize(1);
      oled.setTextColor(1);

      oled.setCursor(0, 0);
      oled.println("Heart BPM");
      oled.setCursor(0,16);
      oled.println((int)pox.getHeartRate());
      
      oled.setCursor(0, 30);
      oled.println("Spo2");
      oled.setCursor(0,45);
      oled.println((int)pox.getSpO2());
      
      oled.display();
      tsLastReport = millis();

      if (pox.getHeartRate() != 0 && pox.getSpO2() != 0) {
        counter++;
        if (counter == 6) {
          avgBPM = pox.getHeartRate();
          avgSpo2 = pox.getSpO2();
        }
        if (counter > 6) {
          avgBPM += pox.getHeartRate();
          avgBPM /= 2;
          avgSpo2 += pox.getSpO2();
          avgSpo2 /= 2;
        }
      }
    }
  } else {
    if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
      Serial.print("Heart BPM-AVG:");
      Serial.print((int)avgBPM);
      Serial.print("-----");
      Serial.print("Oxygen Percent-AVG:");
      Serial.print((int)avgSpo2);
      Serial.print("*");
      Serial.println("\n");
      
      oled.clearDisplay();
      oled.setTextSize(1);
      oled.setTextColor(1);
      
      oled.setCursor(0, 0);
      oled.println("Heart BPM");
      oled.setCursor(0,16);
      oled.println("AVG: ");
      oled.setCursor(25,16);
      oled.println((int)avgBPM);
      
      oled.setCursor(0, 30);
      oled.println("Spo2");
      oled.setCursor(0,45);
      oled.println("AVG: ");
      oled.setCursor(25,45);
      oled.println((int)avgSpo2);
      
      oled.drawBitmap( 60, 20, bitmap, 28, 28, 1);
      oled.display();
      tsLastReport = millis();
    }
  }
}
