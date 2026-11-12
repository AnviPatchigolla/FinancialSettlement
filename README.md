# Privacy-Preserving Financial Settlement System

A Java OOP semester project for managing banks, recording transactions, and performing multilateral net settlement through a simple website or console app.

## Requirements

- Java 11 or newer
- A web browser

## Run the Website

From the project folder:

```bash
javac --add-modules jdk.httpserver backend/*.java
java --add-modules jdk.httpserver -cp backend Server
```

Open [http://localhost:8080](http://localhost:8080). Stop the server with `Ctrl+C`.

Data is stored in memory and resets when the server stops.

## Run the Console App

```bash
java -cp backend ConsoleApp
```
