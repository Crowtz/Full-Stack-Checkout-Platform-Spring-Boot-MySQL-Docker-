# 1. Etapa de Build (Compilação com Maven)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
# Baixa as dependências
RUN mvn dependency:go-offline
COPY src ./src
# Compila e gera o arquivo .jar sem rodar os testes
RUN mvn package -DskipTests

# 2. Etapa de Execução (Ligeira e Otimizada)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copia o .jar gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Cria a pasta de uploads para persistir anexos no container
RUN mkdir -p /app/uploads/attachments

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]