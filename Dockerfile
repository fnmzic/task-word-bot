# Используем базовый образ с Java 19
FROM eclipse-temurin:19-jdk-jammy

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы проекта
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Собираем проект с помощью Maven
RUN ./mvnw clean package -DskipTests

# Указываем команду для запуска бота
CMD ["java", "-jar", "target/your-bot-name.jar"]