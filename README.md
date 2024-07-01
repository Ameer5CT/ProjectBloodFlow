# ProjectBloodFlow

Android app linked with Arduino device to see the blood flow and oxygen percentage of the patient with a telegram bot to send a single record or a full month report to the doctor as an Excel file.



## The YouTube video:

[![](https://img.youtube.com/vi/ch2DE4zOfs4/0.jpg)](https://youtu.be/ch2DE4zOfs4)


### How it works:
The server side:
I focused on trying to make everything without any need for the server so the only time that you need the server is when a new doctor starts the bot to get his ID that is required to send it to the patient app.

The doctor's side:
I tried to make it very easy and simple for the doctor, All he needs to do is start the bot and send his unique ID to the patient, Then he will receive the reports as normal messages from the bot with the patient's name on every report, Single report will be sent as a message and full month report will be sent as an Excel file in the bot messages as well.

The patient's side:
The patient will have the real device (Arduino with sensor) and the Android app, He needs to add his doctor's ID in the app and also connect it to the Arduino through Bluetooth, Then every time he checks his BPM he can send it directly to the doctor with one click or keep it in the app history to send it later as a full report of a specific period.


### Setup:

Components:-

Arduino UNO R3

MAX30100 Heart rate oximeter sensor

128x64 OLED Display

HC-05 Bluetooth module

Breadboard

3 (220 Ohm) Resistors

Wires


Map:


![](https://raw.githubusercontent.com/Ameer5CT/ProjectBloodFlow/main/Arduino/Screenshot%202022-06-23.png)


Note: To use this source code you need to create your own bot (It's free) and use its TOKEN instead of mine.
