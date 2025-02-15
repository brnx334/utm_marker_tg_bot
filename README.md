

# Telegram Bot with Spring Boot and Docker


## Project Overview
This project is a Telegram bot built using Spring Boot and Docker. The bot collects user data (Full Name, Date of Birth, Gender, Photo) and generates a Word document containing this information.


---


## Getting Started


### Prerequisites
- Java 17
- Maven
- Docker
- ngrok
- Telegram Bot API Token


---


### Running the Project


1. **Build and Run with Docker Compose**:
   Open a terminal in the project root directory and execute:
   ```bash
   docker-compose up --build
   ```
   This will build the application and start both the app and PostgreSQL database containers.


2. **Stop Containers**:
   To stop and remove the containers, use:
   ```bash
   docker-compose down
   ```


---


### Setting Up the Telegram Webhook


1. **Start ngrok**:
   Run ngrok to expose your local server:
   ```bash
   ngrok http 8081
   ```
   Copy the generated ngrok URL (e.g., `https://abcd1234.ngrok.io`).


2. **Register the Webhook**:
   Use the following command to set the webhook for your Telegram bot:
   ```bash
   curl -X POST https://api.telegram.org/bot<TOKEN>/setWebhook -d "url=<NGROK_URL>/api/telegram"
   ```
   Example:
   ```bash
   curl -X POST https://api.telegram.org/bot123456789:ABCdefGHIJKLMNOPQRSTUVWXYZ/setWebhook -d "url=https://abcd1234.ngrok.io/api/telegram"
   ```


3. **Verify the Webhook**:
   Check the webhook status using:
   ```bash
   curl -X GET https://api.telegram.org/bot<TOKEN>/getWebhookInfo
   ```


---


### Environment Configuration


1. **Database Configuration**:
   Update the `application.properties` or `application.yml` file with your database credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://db:5432/mydb
   spring.datasource.username=myuser
   spring.datasource.password=mypassword
   spring.jpa.hibernate.ddl-auto=update
   ```


2. **Customize Ports**:
   If you change the app port in `application.properties` (e.g., `server.port=8081`), ensure it matches the port in `docker-compose.yml`:
   ```yaml
   ports:
     - "8081:8081"
   ```


---


### Testing the Bot


1. Start a conversation with your Telegram bot.
2. Send the required data (Full Name, Date of Birth, Gender, Photo).
3. After completing the registration, the bot will send you a generated Word document.


---


### Notes


- Replace `<TOKEN>` with your actual Telegram bot token.
- Ensure the `/api/telegram` endpoint in the webhook URL matches the one configured in your Spring Boot application.
- Use `docker-compose logs app` or `docker-compose logs db` to view logs if issues arise.


