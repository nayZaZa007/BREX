# Beyond the Red Eclipse: Exodus (BREX)

เกม Space Shooter แนว 2D ที่พัฒนาด้วย Java Swing พร้อมระบบ Boss Fight, Power-ups และการเล่นแบบ Co-op

## 📋 สารบัญ
- [ข้อกำหนดของระบบ](#ข้อกำหนดของระบบ)
- [โครงสร้างโปรเจค](#โครงสร้างโปรเจค)
- [วิธีการรันเกม](#วิธีการรันเกม)
  - [รันใน Visual Studio Code](#1-รันใน-visual-studio-code)
  - [รันใน Apache NetBeans](#2-รันใน-apache-netbeans)
  - [รันด้วย Command Line](#3-รันด้วย-command-line)
- [วิธีเล่น](#วิธีเล่น)
- [ฟีเจอร์ของเกม](#ฟีเจอร์ของเกม)
- [การแก้ปัญหา](#การแก้ปัญหา)

---

## 🎮 ข้อกำหนดของระบบ

### ความต้องการขั้นต่ำ:
- **Java Development Kit (JDK)**: 8 หรือสูงกว่า
- **RAM**: 2 GB ขึ้นไป
- **ระบบปฏิบัติการ**: Windows, macOS, หรือ Linux
- **หน้าจอ**: ความละเอียด 1000x700 พิกเซล ขึ้นไป

### เครื่องมือที่แนะนำ:
- Visual Studio Code พร้อม Extension Pack for Java
- หรือ Apache NetBeans 12.0 ขึ้นไป

---

## 📁 โครงสร้างโปรเจค

```
BREX/
├── src/                        # ไฟล์ Source Code หลัก
│   ├── App.java               # Main entry point
│   ├── SpaceGame.java         # Game Engine หลัก
│   ├── Player.java            # ตัวละครผู้เล่น
│   ├── Boss.java              # Boss Enemy
│   ├── entities/              # Entity Classes
│   ├── interfaces/            # Interface Definitions
│   └── managers/              # Game Managers
├── bin/                       # ไฟล์ที่ Compile แล้ว (.class)
│   ├── Pic/                   # รูปภาพ (sprites, backgrounds)
│   │   └── Ani/              # Animations
│   └── Sound/                 # เสียงเกม
│       ├── BMG/              # Background Music
│       └── SFX/              # Sound Effects
├── save/                      # เอกสาร Documentation
└── README.md                  # ไฟล์นี้
```

**หมายเหตุสำคัญ**: ไฟล์รูปภาพและเสียงต้องอยู่ใน `bin/` folder เพื่อให้เกมโหลดได้ถูกต้อง

---

## วิธีการรันเกม

### 1. รันใน Visual Studio Code

#### ขั้นตอนที่ 1: ติดตั้ง Extensions ที่จำเป็น
1. เปิด VS Code
2. กดไปที่ Extensions (Ctrl+Shift+X)
3. ค้นหาและติดตั้ง:
   - **Extension Pack for Java** (จาก Microsoft)
     - ประกอบด้วย: Language Support, Debugger, Test Runner, Maven, Project Manager

#### ขั้นตอนที่ 2: เปิดโปรเจค
1. เปิด VS Code
2. เลือก `File` > `Open Folder...`
3. เลือกโฟลเดอร์ `BREX`
4. รอให้ Java Extension โหลดโปรเจคเสร็จ (ดูที่มุมขวาล่าง)

#### ขั้นตอนที่ 3: ตรวจสอบ Java Runtime
1. กด `Ctrl+Shift+P` เพื่อเปิด Command Palette
2. พิมพ์ `Java: Configure Java Runtime`
3. ตรวจสอบว่ามี JDK 8+ ติดตั้งอยู่

#### ขั้นตอนที่ 4: รันเกม (3 วิธี)

**วิธีที่ 1: ใช้ Run Button**
1. เปิดไฟล์ `src/App.java`
2. คลิกปุ่ม `Run` ที่มุมขวาบน (สัญลักษณ์ ▶️)
3. หรือคลิกขวาที่ไฟล์และเลือก `Run Java`

**วิธีที่ 2: ใช้ Menu Run**
1. เปิดไฟล์ `src/App.java`
2. เลือก `Run` > `Run Without Debugging` (Ctrl+F5)

**วิธีที่ 3: ใช้ Terminal**
```powershell
# Compile
javac -d bin src/*.java src/entities/*.java src/interfaces/*.java src/managers/*.java

# Run
java -cp bin App
```

#### การแก้ปัญหาใน VS Code:
- **ถ้าไม่มี Run Button**: ติดตั้ง Extension Pack for Java
- **ถ้า Build ไม่สำเร็จ**: Clean Java workspace (Ctrl+Shift+P > `Java: Clean Java Language Server Workspace`)
- **ถ้าหารูปภาพไม่เจอ**: ตรวจสอบว่าโฟลเดอร์ `bin/Pic/` และ `bin/Sound/` มีไฟล์ครบ

---

### 2. รันใน Apache NetBeans

#### ขั้นตอนที่ 1: สร้างโปรเจคใหม่
1. เปิด NetBeans
2. เลือก `File` > `New Project`
3. เลือก `Java` > `Java Application` > Next
4. ตั้งชื่อโปรเจค: `BREX`
5. **ยกเลิกการเลือก** "Create Main Class" (เพราะเรามีอยู่แล้ว)
6. กด `Finish`

#### ขั้นตอนที่ 2: Import Source Code
1. ใน Projects Panel คลิกขวาที่โปรเจค `BREX`
2. เลือก `Properties`
3. ไปที่ `Sources` > กำหนด `Source Package Folders` ไปที่โฟลเดอร์ `src` ของคุณ
4. กด `OK`

**หรือ Copy Files แบบ Manual:**
1. คัดลอกไฟล์ทั้งหมดจากโฟลเดอร์ `src/` ไปยัง `src/` ของโปรเจค NetBeans
2. คัดลอกโฟลเดอร์ `bin/Pic/` และ `bin/Sound/` ไปยังโฟลเดอร์ `build/classes/` ของโปรเจค

#### ขั้นตอนที่ 3: ตั้งค่า Main Class
1. คลิกขวาที่โปรเจค `BREX`
2. เลือก `Properties`
3. ไปที่ `Run`
4. กำหนด `Main Class` เป็น: `App`
5. กด `OK`

#### ขั้นตอนที่ 4: รันเกม
**วิธีที่ 1: ใช้ Menu**
- กด `F6` หรือเลือก `Run` > `Run Project`

**วิธีที่ 2: ใช้ Toolbar**
- คลิกปุ่มสีเขียว ▶️ (Run Project) บน toolbar

**วิธีที่ 3: ใช้ Context Menu**
- คลิกขวาที่โปรเจค `BREX` > เลือก `Run`

#### การ Build โปรเจค:
```
Clean: F11 หรือ Build > Clean Project
Build: Shift+F11 หรือ Build > Build Project
Clean and Build: Shift+F11 (หลังจาก Clean)
```

#### การแก้ปัญหาใน NetBeans:
- **ถ้า Compile Error**: คลิกขวาที่โปรเจค > `Clean and Build`
- **ถ้าหารูปภาพไม่เจอ**: Copy โฟลเดอร์ `Pic/` และ `Sound/` ไปที่ `build/classes/`
- **ถ้า Main Class ไม่เจอ**: ตรวจสอบว่าตั้งค่า Main Class เป็น `App` ถูกต้อง

---

### 3. รันด้วย Command Line

#### บน Windows (PowerShell/CMD):
```powershell
# 1. Navigate to project directory
cd C:\Users\USER\Documents\test2\BREX

# 2. Compile ทุกไฟล์
javac -d bin src\*.java src\entities\*.java src\interfaces\*.java src\managers\*.java

# 3. Run
java -cp bin App
```

#### บน macOS/Linux (Terminal):
```bash
# 1. Navigate to project directory
cd ~/Documents/BREX

# 2. Compile ทุกไฟล์
javac -d bin src/*.java src/entities/*.java src/interfaces/*.java src/managers/*.java

# 3. Run
java -cp bin App
```

---

## วิธีเล่น

### การควบคุม - Player 1:
- **W, A, S, D** หรือ **Arrow Keys**: เคลื่อนที่
- **F**: ใช้ความสามารถ
- **J** : Toggle Co-op Mode
- **ESC**: หยุดเกมชั่วคราว (Pause)

### การควบคุม - Player 2 (Co-op Mode):
- **Arrow Keys**: เคลื่อนที่
- **RShift**: ใช้ความสามารถ

### เมนูหลัก:
- ใช้ **Arrow Keys** + **Enter** เพื่อเลือก

### หน้าจอเกม:
- **แถบ HP (สีแดง)**: เลือดของผู้เล่น
- **Score**: คะแนนสะสม
- **Level**: ระดับปัจจุบัน

---

## ฟีเจอร์ของเกม

### โหมดการเล่น:
- 🎮 **Single Player**: เล่นคนเดียว
- 👥 **Co-op Mode**: เล่น 2 คน (Local Multiplayer) 
- 🚀 **Level Selection**: เลือกเลเวลที่ต้องการเล่น
- ✈️ **Spacecraft Selection**: เลือกยานรบที่ต้องการ

### ระบบเกม:
-  **Enemy System**: ศัตรูหลายประเภท
-  **Boss Fights**: การต่อสู้กับบอส (พร้อมเลเซอร์และกระสุนพิเศษ)
-  **Power-ups**: ไอเทมเพิ่มพลัง (HP, Damage, Fire Rate, Shield)
-  **Particle Effects**: เอฟเฟกต์การระเบิดและความเสียหาย
-  **Sound System**: เสียง BGM และ SFX
-  **Save System**: บันทึกการตั้งค่า

### การตั้งค่า:
- 🔊 ปรับระดับเสียง (BGM และ SFX แยกกัน)
- 🖥️ โหมดเต็มหน้าจอ (Fullscreen)
---

## 🔧 การแก้ปัญหา

### ปัญหาทั่วไป:

**1. เกมไม่เปิด / ขึ้น Error "Could not find or load main class"**
- ตรวจสอบว่า compile แล้วโดยใช้ `-d bin`
- ตรวจสอบว่า Main Class คือ `App` (ไม่ใช่ SpaceGame)
- ลองรัน: `java -cp bin App` แทน `java App`

**2. ไม่มีรูปภาพ / เสียง**
- ตรวจสอบว่าโฟลเดอร์ `bin/Pic/` และ `bin/Sound/` มีไฟล์ครบถ้วน
- ใน NetBeans: copy resources ไปที่ `build/classes/`
- path ต้องเป็น: `bin/Pic/Ani/`, `bin/Sound/BMG/`, `bin/Sound/SFX/`

**3. เกมช้า / lag**
- ปิดโปรแกรมอื่นๆ ที่ใช้ทรัพยากร
- ตรวจสอบว่า Java ใช้ JDK ที่เหมาะสม
- ลดความละเอียดหน้าจอ

**4. Compile Error**
- ตรวจสอบเวอร์ชัน JDK (ต้อง 8+)
- Clean และ Build ใหม่
- ตรวจสอบว่าไฟล์ทุกไฟล์อยู่ครบ

**5. เกมค้างตอนเปิด**
- กด Alt+Tab เช็คว่าหน้าต่างเกมอยู่หลังหรือไม่
- ลอง Run ใหม่
- เช็ค Task Manager ว่ามี Java process ค้างอยู่หรือไม่

### ตรวจสอบ Java Version:
```powershell
java -version
javac -version
```

ควรแสดงผล JDK 8 หรือสูงกว่า

---

## 📝 หมายเหตุสำหรับนักพัฒนา

### Dependencies:
- Java Swing (GUI Framework)
- Java AWT (Graphics)
- javax.sound.sampled (Audio System)
- javax.imageio (Image Loading)

### Architecture:
- **Entity-Component Pattern**: ใช้ entities, interfaces, และ managers
- **Game State Management**: มีระบบจัดการสถานะเกม
- **Collision Detection**: ระบบตรวจจับการชน
- **Resource Management**: โหลดและจัดการ assets

### ไฟล์สำคัญ:
- `App.java`: Entry point ของเกม
- `SpaceGame.java`: Game loop และ logic หลัก
- `Player.java`: ตรรกะของผู้เล่น
- `Boss.java`: AI และพฤติกรรมของบอส

---


## 🎮 สนุกกับการเล่น!

**Beyond the Red Eclipse: Exodus** - ผจญภัยในอวกาศและกำจัดผู้รุกราน!

---

*README นี้อัพเดทล่าสุด: พฤศจิกายน 2025*
