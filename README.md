# Bank CRM System

A Consumer Relationship Management (CRM) system for banking operations built with Java Swing and MySQL.

## Features
- Multi-role authentication (Manager, Employee, Customer)
- Secure login and registration system
- Role-specific dashboards
- Database integration using JDBC

## Prerequisites
- Java JDK 8 or higher
- MySQL Server
- Eclipse IDE
- MySQL Connector/J

## Project Structure
```
crm_bank/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── crmbank/
│   │   │   │   │   ├── dao/         # Data Access Objects
│   │   │   │   │   ├── model/       # Entity classes
│   │   │   │   │   ├── ui/          # User Interface components
│   │   │   │   │   ├── util/        # Utility classes
│   │   │   │   │   └── Main.java    # Application entry point
│   │   │   └── resources/
│   │   └── lib/                     # External libraries
```

## Database Setup
1. Create a MySQL database named 'crm_bank'
2. Run the SQL scripts in the resources folder to create necessary tables

## Running the Application
1. Open the project in Eclipse
2. Ensure all dependencies are properly configured
3. Run Main.java to start the application 