# Etapa 1: Construcción y pruebas con JDK 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copiar configuración Maven y código del backend
COPY backend/pom.xml ./pom.xml
COPY backend/.mvn ./.mvn
COPY backend/mvnw ./mvnw
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY backend/src ./src
# Empaquetar la carpeta única frontend/ dentro de los recursos estáticos del JAR
COPY frontend/ ./src/main/resources/static/

# Ejecutar pruebas unitarias y generar el ejecutable JAR
RUN ./mvnw clean package -B

# Etapa 2: Imagen ligera de producción con JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]