# 🏦 Banking Portal REST API

A secure and structured Banking Portal REST API developed using **Spring Boot, Spring Security, JWT, MySQL, JPA/Hibernate, MapStruct, and Maven**.

This backend provides REST APIs for user authentication, account management, PIN management, OTP verification, cash deposit and withdrawal, fund transfers, transaction history, dashboard information, email services, and other banking operations.

---

## 📌 Project Overview

The Banking Portal is designed to simulate the core functionality of a digital banking system.

The backend follows a layered architecture where controllers handle HTTP requests, services contain business logic, repositories communicate with the database, and entities represent persistent data.

The application uses **Spring Security and JWT authentication** to protect secured endpoints and ensure that banking operations can only be performed by authenticated users.

A separate frontend application communicates with this backend through REST APIs.

---

## ✨ Features

### 👤 User Management
- User registration
- User login
- User profile management
- User validation
- Secure password handling
- Account creation associated with registered users

### 🔐 Authentication & Security
- Spring Security
- JWT-based authentication
- Bearer token authentication
- Protected REST endpoints
- Authentication filters
- Unauthorized-access handling
- Password encryption
- Token management

### 🔢 PIN Management
- Create banking PIN
- Update banking PIN
- PIN validation
- Secure PIN-related operations

### 📩 OTP Verification
- OTP generation
- OTP validation
- OTP retry-limit handling
- OTP-based verification
- Email-based OTP functionality

### 💰 Banking Operations
- Cash deposit
- Cash withdrawal
- Balance validation
- Minimum/maximum transaction validation
- Fund transfer between accounts
- Insufficient-balance handling

### 📊 Transactions
- Transaction creation
- Transaction history
- Transaction details
- Deposit transactions
- Withdrawal transactions
- Fund-transfer transactions

### 📈 Dashboard
The backend provides dashboard-related information that can be consumed by the frontend to display account and transaction statistics.

### 📧 Email Services
The application includes email functionality using **Spring Boot Starter Mail** for services such as OTP and account-related communication.

### 🌍 Geolocation
The project contains a geolocation service that can retrieve location-related information using an external geolocation API.

### ⚡ Caching
Caching support is implemented using Spring Cache with **Caffeine** and the project also contains Redis configuration for caching-related functionality.

### 🚨 Exception Handling
The application includes custom exceptions and centralized exception handling for cases such as:
- Account not found
- Invalid PIN
- Invalid OTP
- Invalid token
- Unauthorized access
- Insufficient balance
- Invalid transaction amount
- Fund-transfer errors
- Password-reset errors
- Geolocation errors

---

## 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| Java 17 | Programming language |
| Spring Boot 3.3.1 | Backend framework |
| Spring Web | REST API development |
| Spring Security | Authentication and authorization |
| JWT | Token-based authentication |
| Spring Data JPA | Database interaction |
| Hibernate | ORM |
| MySQL | Relational database |
| MapStruct | Object mapping |
| Lombok | Boilerplate reduction |
| Maven | Build and dependency management |
| Spring Mail | Email functionality |
| Caffeine | Local caching |
| Redis | Cache support |
| Swagger / OpenAPI | API documentation |
| JUnit | Testing |

---

## 🏗️ Project Architecture

The backend follows a layered architecture:

```text
Client / Frontend
       │
       ▼
   Controllers
       │
       ▼
     Services
       │
       ▼
   Repositories
       │
       ▼
      MySQL
```

Supporting components include:

```text
Security
   │
   ├── JWT Authentication
   ├── Authentication Filter
   └── Security Configuration

DTO
   │
   └── Request / Response Objects

Mapper
   │
   └── Entity ↔ DTO Mapping

Exception
   │
   └── Global Exception Handling

Config
   │
   ├── Security
   ├── Cache
   ├── Redis
   ├── CORS
   └── Swagger
```

---

## 📂 Project Structure

```text
backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/webapp/bankingportal/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── .mvn/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── Dockerfile
├── docker/
└── README.md
```

### Main Packages

- `config/` — Application configurations such as security, caching, Redis, CORS and Swagger.
- `controller/` — REST controllers responsible for handling API requests.
- `service/` — Main business logic of the banking application.
- `repository/` — Spring Data JPA repositories used for database operations.
- `entity/` — JPA entities representing database tables.
- `dto/` — Data Transfer Objects used for API requests and responses.
- `mapper/` — MapStruct and custom mapping logic.
- `security/` — JWT authentication and security-related components.
- `exception/` — Custom exceptions used throughout the application.
- `util/` — Reusable utility and validation classes.
- `type/` — Application-specific types used for caching and other functionality.

---

## 🗄️ Database

The application uses **MySQL** as its relational database.

The backend uses:
- Spring Data JPA
- Hibernate
- MySQL Connector
- Entity-based database mapping

Major entities include:

```text
User
Account
Transaction
OtpInfo
Token
PasswordResetToken
```

---

## 🔐 Authentication Flow

```text
User
 │
 ▼
Login API
 │
 ▼
Spring Security
 │
 ▼
Credentials Validation
 │
 ▼
JWT Token Generation
 │
 ▼
Client
 │
 ▼
Authorization: Bearer <token>
 │
 ▼
JWT Authentication Filter
 │
 ▼
Protected API
```

The JWT token is used to authenticate requests to secured endpoints.

---

## 💳 Banking Operation Flow

### Cash Deposit

```text
Client
   ↓
Account Controller
   ↓
Account Service
   ↓
Validate Account & Amount
   ↓
Update Balance
   ↓
Create Transaction
   ↓
Database
```

### Cash Withdrawal

```text
Client
   ↓
Account Controller
   ↓
Account Service
   ↓
Validate PIN
   ↓
Check Balance
   ↓
Withdraw Amount
   ↓
Create Transaction
   ↓
Database
```

### Fund Transfer

```text
Sender
   ↓
Transfer API
   ↓
Validate Authentication
   ↓
Validate Sender Account
   ↓
Validate Receiver Account
   ↓
Check Balance
   ↓
Transfer Amount
   ↓
Create Transactions
   ↓
Database
```

---

## 📩 OTP & Email

The project contains an OTP service responsible for generating and validating OTPs.

The email service uses Spring Mail and SMTP configuration to send email messages.

OTP-related functionality includes:
- OTP generation
- OTP validation
- OTP expiration/validation logic
- Retry-limit handling
- Email delivery

---

## 🌐 API Documentation

The project includes OpenAPI/Swagger support for API documentation.

After starting the application, the available API documentation can be accessed through the configured Swagger/OpenAPI endpoint.

---

## ⚙️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/waniowais03/Banking-Portal.git
```

### 2. Navigate to Backend

```bash
cd Banking-Portal/backend
```

### 3. Configure MySQL

Create a MySQL database named:

```text
bankingapp
```

Then configure the database connection in your local:

```text
src/main/resources/application.properties
```

Do not commit sensitive credentials to GitHub.

Use the provided sample configuration:

```text
src/main/resources/application.properties.sample
```

as a reference.

---

## 📝 Application Configuration

The application requires configuration for:

```properties
server.port=8180

spring.datasource.url=jdbc:mysql://localhost:3306/bankingapp
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

JWT configuration and email configuration should also be provided through local configuration or environment variables.

**Never expose real database passwords, email passwords, API keys, or JWT secrets in a public repository.**

---

## 🚀 Running the Backend

From the `backend` directory:

```bash
./mvnw spring-boot:run
```

If PMD causes a build issue during development:

```bash
./mvnw spring-boot:run -Dpmd.skip=true
```

The application is configured to run on:

```text
http://localhost:8180
```

---

## 🧪 Running Tests

```bash
./mvnw test
```

---

## 🔨 Build the Project

```bash
./mvnw clean package
```

The generated build files will be available inside:

```text
target/
```

---

## 🐳 Docker

The backend contains Docker-related files:

```text
Dockerfile
docker/docker-compose.yml
```

These can be used to containerize and run the backend and its supporting services.

---

## 🔗 Frontend Integration

This repository contains the backend REST API.

A separate frontend application communicates with this backend using HTTP requests.

```text
Frontend
   │
   │ HTTP / REST API
   ▼
Spring Boot Backend
   │
   ├── Spring Security
   ├── Business Logic
   ├── JWT
   └── JPA/Hibernate
          │
          ▼
        MySQL
```

The frontend can consume authentication, account, transaction, transfer, deposit and withdrawal APIs provided by the backend.

---

## 🔒 Security Considerations

The project uses:
- Spring Security
- JWT authentication
- Password encryption
- Bearer token authorization
- Protected endpoints
- Input validation
- Custom security exceptions
- OTP verification

For deployment, sensitive values should be stored using environment variables or secure secret management instead of committing them to source control.

---

## 📌 Future Improvements

Possible future improvements include:
- Improved dashboard charts
- Pagination for transaction tables
- Better token lifecycle management
- Email notification on account login
- Bank statement generation
- Email delivery of bank statements
- Production-ready deployment configuration
- Improved frontend/backend environment configuration
- Additional automated tests

---

## 👨‍💻 Project

**Banking Portal REST API**

Developed as a full-stack banking application with a Spring Boot REST API backend and a separate frontend application.

The project demonstrates practical implementation of:
- REST API development
- Spring Boot
- Spring Security
- JWT authentication
- MySQL database integration
- JPA/Hibernate
- OTP verification
- Email services
- Banking transactions
- Exception handling
- Caching
- API documentation
- Unit testing

---

## 📄 License

This project includes the license and contribution files provided with the project repository.

For contribution guidelines, please refer to:

```text
CONTRIBUTING.md
```

---

## ⭐ Acknowledgement

This project was developed and customized as a practical banking application to understand how a modern backend system can handle authentication, account management, secure transactions, database operations and communication with a separate frontend application.
