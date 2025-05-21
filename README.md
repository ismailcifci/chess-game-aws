
# ♔ Online Multiplayer Chess

A real-time multiplayer Chess application built with **Java Swing**, using an **AWS EC2** server for networked gameplay.

---

## 🚀 Features

- **Real-Time Gameplay:** Play Chess online with players from anywhere.
- **Advanced Chess Rules:** Supports castling, en passant, and pawn promotion.
- **Matchmaking System:** Quickly pairs players.
- **User-Friendly Interface:** Interactive UI with intuitive highlighting.
- **AWS Cloud Hosting:** Server-side logic hosted securely on AWS EC2.

---

## 🎯 Technology Stack

- **Language:** Java 21
- **GUI:** Java Swing
- **Networking:** Java Sockets (TCP/IP)
- **Cloud Platform:** Amazon Web Services (EC2)
- **Tools:** NetBeans, Maven, FileZilla

---

## 📦 Project Structure

```
OnlineChess
├── src
│   └── main
│       └── java
│           └── com.mycompany.chess
│               ├── ClientMain.java        // Client application entry
│               ├── ServerMain.java        // Server startup script
│               ├── HomeScreen.java        // Main menu GUI
│               ├── GameScreen.java        // Gameplay interface
│               ├── GameClient.java        // Client-server communication
│               ├── ClientHandler.java     // Individual client management
│               ├── Matchmaker.java        // Player queue management
│               ├── GameSession.java       // Game lifecycle management
│               └── ChessGame.java         // Core chess logic
└── pom.xml                                // Maven build file
```

---

## ⚙️ Installation & Setup

### Server (AWS EC2)

1. **Launch EC2 Instance:**
   - AMI: `Amazon Linux 2023`
   - Type: `t2.micro` or higher

2. **Configure Security Group:**
   - TCP inbound port: `8888`, source: `0.0.0.0/0`

3. **Install Java 21:**
   ```bash
   sudo yum install java-21-amazon-corretto
   ```

4. **Deploy Server Jar via FileZilla:**
   - Export your project as a JAR from NetBeans (Clean and Build).
   - Upload the JAR to EC2.

5. **Run the Server:**
   ```bash
   java -jar ChessServer.jar
   ```
   You should see:
   ```
   Server started on port 8888
   ```

### Client (Local)

1. **Clone/Download Project.**

2. **Update Server IP (`GameClient.java`):**
   ```java
   socket = new Socket("YOUR_EC2_PUBLIC_IP", 8888);
   ```

3. **Run `ClientMain.java` in NetBeans.**

---

## 🚨 Common Issues & Troubleshooting

**Connection Timeout:**
- Ensure port 8888 is open in EC2 Security Groups.
- Double-check EC2 public IP in your client code.

**Java Compatibility:**
- Java 21 required on both client and server.

---

## 🧑‍💻 How to Play

**Start Game:**
- Launch application, click "Find Match".

**In-Game Controls:**
- Select pieces to see valid moves.
- Special chess rules handled automatically.

**Game End:**
- Transparent dialog displays results and replay/menu options.

---

## 📄 Documentation

- Detailed Project Report

---

## 👨‍💻 Contributors

- [Ismail Cifci](mailto:ismail.cifci@stu.fsm.edu.tr)

---

**Happy Chess Playing! ♟️**
