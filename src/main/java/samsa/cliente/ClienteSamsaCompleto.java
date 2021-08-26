package samsa.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import samsa.cliente.modelo.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

public class ClienteSamsaCompleto {
    // URL do Samsa
    final String URL_BASE = "http://lakota:9001";

    // Atividades definidas para auditoria no sistema
    final Map<String, String> atividades = Map.of(
            "LOGIN_SUCESSO", "Login no sistema realizado com sucesso",
            "LOGIN_FALHA", "Login ou senha incorretos ao se tentar logar",
            "SENHA_ALTERADA", "Solicitacao de mudanca de senha",
            "CADASTRO_SISTEMA", "Novo sistema cadastrado",
            "ALTERACAO_SISTEMA", "Sistema existente alterado"
    );

    // Cliente http nativo do Java (11 em diante)
    final HttpClient clienteHttp = HttpClient.newHttpClient();

    // Serializador JSON
    final ObjectMapper mapeadorJson = new ObjectMapper();

    // No exemplo, esse timeout é usado em todos os pontos, mas cada chamada pode ter um timeout diferente
    // IMPORTANTE: Se o timeout ocorrer, o HttpRequest gera excecao de runtime. Precisa interceptar sempre!
    final long TIMEOUT = 1000;

    // Simula uma interacao completa com o SAMSA, da verificacao do
    // sistema ao cadastro de atividades e envio e consulta de eventos
    private void executar() throws Exception {
        // Presumindo que sistema sabe o seu código
        String COD_SISTEMA = "ACESSO";

        // Verificar se sistema existe; se não existir, precisa solicitar externamente e nada a fazer aqui
        if (sistemaExiste(COD_SISTEMA)) System.out.printf("Sistema %s encontrado.%n", COD_SISTEMA);
        else return;

        // Cadastrar atividades para o sistema
        for (var item : atividades.entrySet()) {
            // Para cada item no mapa de atividades, criar obj Atividade e cadastrar
            String codAtividade = item.getKey();
            String descAtividade = item.getValue();
            Atividade atividade = new Atividade()
                    .setCodAtividade(codAtividade)
                    .setDescAtividade(descAtividade)
                    .setCodSistema(COD_SISTEMA);

            // Enviar requisicao para cadastro
            cadastrarAtividade(atividade);
        }

        // Verificar se atividades foram realmente cadastradas
        for (String codAtividade : atividades.keySet()) {
            if (!atividadeExiste(COD_SISTEMA, codAtividade))
                System.out.printf("Atividade %s não encontrada.%n", codAtividade);
        }

        // Montar objeto Evento
        Evento evento = criarEvento();

        // Enviar Evento
        SituacaoEnvio sitEnvio = enviarEvento(evento);

        // A situacao ACEITO indica que evento passou na validacao; caso contrário 'erros' vem preenchido
        if (sitEnvio.getCodSituacao() == CodSituacaoEnvio.ACEITO)
            System.out.printf("Evento enviado. Código %s %n", sitEnvio.getCodEvento());
        else
            System.out.printf("Evento não enviado. Erros: %s %n", sitEnvio.getErros());

        // Verificar chegada do evento no Kafka, usando código do evento
        SituacaoRecebimento sitRecebimento = consultarEvento(sitEnvio.getCodEvento());

        // A situacao CONFIRMADO indica que evento foi gravado no Kafka
        if (sitRecebimento.getCodSituacao() == CodSituacaoRecebimento.CONFIRMADO)
            System.out.printf("Evento recebido no Kafka. Timestamp: %s", sitRecebimento.getDataHoraConfirmacao());
    }

    private void cadastrarAtividade(Atividade atividade) throws Exception {
        // Serializar atividade para JSON
        String atividadeJson = mapeadorJson.writeValueAsString(atividade);

        // Montar requisição HTTP
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/cadastrar-atividade"))
                .timeout(Duration.ofMillis(TIMEOUT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(atividadeJson))
                .build();

        // Enviar requisição e tratar código da resposta
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 201)
            System.out.printf("Atividade %s cadastrada.%n", atividade.getCodAtividade());
        else
            System.out.println("Erro no cadastro de atividade " + atividade.getCodAtividade());
    }

    // Simula a criacao de um evento. Nos sistemas, isso vai precisar ser realizado
    // num local em que o request e o usuário logado estejam acessíveis
    private Evento criarEvento() {
        return new Evento()
                .setCodSistema("ACESSO") // Sigla fixa para cada sistema
                .setCodAtividade("LOGIN_FALHA") //Tipo de atividade realizada
                .setDescricao("Falha em login de usuário...") // Descricao livre do evento
                .setDestinoIp("122.141.31.0") // request.getLocalAddr()
                .setDestinoUrl("https://sigma.tre-pe.jus.br/sigma/planilha.xhtml") // request.getRequestURL()
                .setDestinoPorta("80") // request.getServerPort()
                .setDataHora(OffsetDateTime.now().toString()) // OffsetDateTime.now()
                .setOrigemIp("231.244.41.41") // request.getRemoteAddr()
                .setOrigemHorasFuso("-03:00") // horas de fuso no cliente (não tem forma de obter do request)
                .setUsuarioLogin("0427614398111") // login do usuário atual
                .setUsuarioNome("Epaminondas Silveira") // nome do usuário atual
                .setUsuarioSiglaLotacao("SEJAR") // lotacao do usuário atual
                .setPropriedades(Map.of("CPF", "189.183.238-31")); // propriedades adicionais específicas
    }

    private SituacaoEnvio enviarEvento(Evento evento) throws Exception {
        // Serializar evento para JSON
        String eventoJson = mapeadorJson.writeValueAsString(evento);

        // Montar requisição HTTP para envio de evento
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/enviar-evento"))
                .timeout(Duration.ofMillis(TIMEOUT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventoJson))
                .build();

        // Obter resposta HTTP com o JSON retornado
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());

        // Desserializar o JSON e retornar o objeto SituacaoEnvio
        return mapeadorJson.readValue(resp.body(), SituacaoEnvio.class);
    }

    private SituacaoRecebimento consultarEvento(String codEvento) throws IOException, InterruptedException {
        // Montar requisição HTTP para consultae evento enviado
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/consultar-evento/" + codEvento))
                .timeout(Duration.ofMillis(TIMEOUT))
                .GET()
                .build();

        // Enviar requisição e obter resposta HTTP com o JSON de retorno
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());

        // Desserializar o JSON e retornar o objeto SituacaoEnvio
        return mapeadorJson.readValue(resp.body(), SituacaoRecebimento.class);
    }

    private boolean sistemaExiste(String codSistema) throws Exception {
        // Montar requisição HTTP para consulta de sistemas
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/consultar-sistema/" + codSistema))
                .timeout(Duration.ofMillis(TIMEOUT))
                .GET()
                .build();

        // Obter resposta HTTP com o JSON retornado
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());

        // Se status ok e corpo retornado estiver preenchido, sistema foi encontrado
        return (resp.statusCode() == 200 && !resp.body().isEmpty());
    }

    private boolean atividadeExiste(String codSistema, String codAtividade) throws Exception {
        // Montar requisição HTTP para consulta de sistemas
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/consultar-atividade/sistema/" + codSistema + "/atividade/" + codAtividade))
                .timeout(Duration.ofMillis(TIMEOUT))
                .GET()
                .build();

        // Obter resposta HTTP com o JSON retornado
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());

        // Se status OK e corpo vier preenchido, atividade foi encontrada
        return (resp.statusCode() == 200 && !resp.body().isEmpty());
    }

    public static void main(String[] args) throws Exception {
        new ClienteSamsaCompleto().executar();
    }
}
