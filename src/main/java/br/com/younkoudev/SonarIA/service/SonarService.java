package br.com.younkoudev.SonarIA.service;

import br.com.younkoudev.SonarIA.model.SonarResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.tools.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class SonarService {

    @Value("${sonar.senha}")
    private String senha;

    @Value("${sonar.login}")
    private String login;

    @Value("${sonar.url}")
    private String urlSonar;

    @Value("${sonar.keyProjeto}")
    private String keyProjeto;

    @Value("${path.projeto}")
    private String absolutePath;

      private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private IAService iaService;

    public void getRelatorioSonar() {

        int pageNumber = 1;

        List<SonarResponse.Issue> issues = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + getAuthorization());

        HttpEntity<Object> httpEntity = new HttpEntity<>(null, headers);

        do {
            String url = String.format("""
                    %s
                    ?componentKeys=%s
                    &types=CODE_SMELL
                    &p=%d
                    &ps=100
                    """,urlSonar,keyProjeto,pageNumber);

            ResponseEntity<SonarResponse> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, SonarResponse.class);

            SonarResponse sonarResponse = exchange.getBody();

            if (sonarResponse != null && sonarResponse.getIssues() != null && !sonarResponse.getIssues().isEmpty()) {
                issues.addAll(sonarResponse.getIssues()); // Adiciona os issues encontrados
                pageNumber++; // Incrementa o número da página para a próxima requisição
            } else {
                break; // Sai do loop se não houver mais issues
            }
        } while (true);

        processIssuesInBatches(issues.stream().filter(i -> i.getSeverity().equals("MINOR")).toList());
    }


    private void processIssuesInBatches(List<SonarResponse.Issue> issues) {

        int batchSize = 10; // Tamanho do lote para processamento

        List<List<SonarResponse.Issue>> batches = new ArrayList<>();

        for (int i = 0; i < issues.size(); i += batchSize) {
            batches.add(issues.subList(i, Math.min(i + batchSize, issues.size())));
        }

        List<CompletableFuture<Void>> futures = batches.stream()
                .map(this::processBatch)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    private CompletableFuture<Void> processBatch(List<SonarResponse.Issue> batch) {
        return CompletableFuture.runAsync(() -> {
            batch.forEach(issue -> {

                String project = issue.getProject();
                String component = issue.getComponent()
                        .replace(project, StringUtils.EMPTY)
                        .replace(":", "/");
                String nomeClasse = StringUtils.substringAfterLast(component, "/");
                int startLine = issue.getTextRange().getStartLine();
                String message = issue.getMessage();

                String fileContent;
                try {
                    fileContent = readFile(absolutePath.concat(component));
                } catch (IOException e) {
                    Logger.getLogger(SonarService.class.getName()).severe("Erro ao ler o arquivo: " + e.getMessage());
                    return;
                }

                String mensagem = String.format("""
                    Você é um assistente de programação especializado em Java. Sua tarefa é corrigir o código Java com base na sugestão fornecida pelo SonarQube, mantendo o restante do código exatamente como está.
                           ### Regras:
                            1. **Não modifique** nenhuma parte do código que não seja estritamente necessária para aplicar a sugestão do SonarQube.
                            2. Certifique-se de que o código final permaneça **funcional** e sem erros de compilação.
                            3. Aplique **apenas** a sugestão fornecida. Se não entender como aplicar a sugestão, **mantenha o código original inalterado**.
                    
                            ### Sugestão do SonarQube:
                            %s na linha %d (a partir da palavra 'package').
                    
                            ### Código Java Original:
                            %s
                    
                            ### Instruções:
                            1. Leia a sugestão do SonarQube e identifique o trecho de código relevante.
                            2. Aplique a correção necessária para atender à sugestão, **mantendo o restante do código intocado**.
                            3. Caso não consiga aplicar a sugestão corretamente, **não faça alterações** e retorne o código original.
                            4. Retorne o código completo com **apenas** a correção aplicada.
               """, message, startLine, fileContent);

                Map<String, String> responseIA = iaService.getResponseIA(mensagem);
                try {
                    String completion = responseIA.get("completion")
                            .replace("```java", "")
                            .replace("```", "")
                            .trim();

                    // Validar se a resposta não quebrou o código original
                    if (isCodeValid(completion)) {
                        saveFile(absolutePath.concat(component), completion);
                        System.out.println("Arquivo corrigido com sucesso: " + nomeClasse);
                    } else {
                        System.out.println("Alteração inválida detectada. Mantendo o arquivo original: " + nomeClasse);
                    }

                } catch (IOException e) {
                    Logger.getLogger(SonarService.class.getName()).severe("Erro ao salvar o arquivo: " + e.getMessage());
                }
            });
        });
    }

    private boolean isCodeValid(String javaCode) {
        // Tenta compilar o código para validar a correção
        try {
            // Obtém o compilador do sistema
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                Logger.getLogger(SonarService.class.getName()).severe("Compilador Java não encontrado. Certifique-se de usar o JDK para compilar.");
                return false;
            }

            // Coleta diagnósticos de compilação
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

            // Cria um arquivo temporário para testar a compilação
            Path tempFile = Files.createTempFile("TempJavaClass", ".java");
            Files.write(tempFile, javaCode.getBytes());

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(tempFile.toFile());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

            // Executa a compilação
            boolean success = task.call();
            if (!success) {
                // Registra erros de compilação
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    Logger.getLogger(SonarService.class.getName()).severe("Erro de compilação: " + diagnostic.getMessage(null));
                }
            }

            Files.delete(tempFile); // Limpa o arquivo temporário
            return success;
        } catch (IOException e) {
            Logger.getLogger(SonarService.class.getName()).severe("Erro ao compilar o código: " + e.getMessage());
            return false;
        }
    }

    private String getAuthorization() {
        return Base64.getEncoder().encodeToString((login + ":" + senha).getBytes());
    }

    public static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static void saveFile(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes());
    }
}
