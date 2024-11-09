# Use uma imagem base do OpenJDK
FROM openjdk:17-jdk-slim

# Instalar o Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Diretório de trabalho dentro do container
WORKDIR /app

# Copie os arquivos do seu projeto (pasta local) para dentro do container
COPY . /app/

# Defina as variáveis de ambiente com placeholders para serem substituídos na execução
ENV SONAR_SENHA={{SENHA}} \
    SONAR_USUARIO={{USUARIO}} \
    SONAR_URL={{URL}} \
    SONAR_KEY_PROJETO={{KEY_PROJETO}} \
    PATH_PROJETO={{PATH_PROJETO}} \
    SPRING_AI_OPENAI_API_KEY={{TOKEN}}

# Execute o Maven para compilar o projeto (ajuste o comando conforme seu projeto)
RUN mvn clean package -DskipTests

# Expõe a porta em que o Spring Boot rodará
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "/app/target/SonarIA.jar"]
