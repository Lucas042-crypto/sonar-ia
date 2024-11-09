# sonar-ia
SonarQube integrado com chatGPT


Rodar aplicação na pipeline com docker

docker run -d -p 8080:8080 --env SONAR_SENHA=senha --env SONAR_USUARIO=usuario --env SONAR_URL=url --env SONAR_KEY_PROJETO=key --env PATH_PROJETO=/path --env SPRING_AI_OPENAI_API_KEY=token meu-projeto
