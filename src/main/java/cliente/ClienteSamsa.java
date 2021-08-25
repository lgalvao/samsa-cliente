package cliente;

import cliente.modelo.CodSituacaoEvento;
import cliente.modelo.Evento;
import cliente.modelo.SituacaoEvento;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

public class ClienteSamsa {
    final static Gson gson = new Gson();
    final static HttpClient httpClient = HttpClient.newHttpClient();
    final static long TIMEOUT_ENVIO = 500;
    final static long TIMEOUT_CONSULTA = 200;

    public static void main(String[] args) throws Exception {
        // Montar objeto Evento
        Evento evento = criarEvento();

        // Enviar evento
        SituacaoEvento situacaoEnvio = enviarEvento(evento);

        // A situacao ACEITO indica que evento passou na validacao
        // Se não passar ou houver problemas internos, o campo 'erros' vem preenchido
        if (situacaoEnvio.getCodSituacaoEvento() != CodSituacaoEvento.ACEITO) {
            System.out.println("Erros no envio do evento: " + situacaoEnvio.getErros());
            return;
        }

        // Extrair o código do evento enviado e verificar a chegada no Kafka
        String codEvento = situacaoEnvio.getCodEvento();
        SituacaoEvento situacaoRecebimento = verificarEvento(codEvento);

        // A situação CONFIRMADO indica que o evento foi gravado no Kafka
        if (situacaoRecebimento.getCodSituacaoEvento() == CodSituacaoEvento.CONFIRMADO) {
            System.out.println("Evento recebido no Kafka. Timestamp: " + situacaoRecebimento.getDataHoraConfirmacao());
        }
    }

    private static SituacaoEvento verificarEvento(String codEvento) throws IOException, InterruptedException {
        // Montar requisição HTTP para consultae evento enviado
        HttpRequest requisicao1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9001/consultar-evento/" + codEvento))
                .timeout(Duration.ofMillis(TIMEOUT_CONSULTA))
                .GET()
                .build();

        // Enviar requisição e obter resposta HTTP com o JSON de retorno
        HttpResponse<String> respConsulta = httpClient.send(requisicao1, HttpResponse.BodyHandlers.ofString());

        // Desserializar o JSON e retornar o objeto SituacaoEnvio
        return gson.fromJson(respConsulta.body(), SituacaoEvento.class);
    }

    private static SituacaoEvento enviarEvento(Evento evento) throws IOException, InterruptedException {
        // Serializar para JSON
        String eventoJson = gson.toJson(evento);

        // Montar requisição HTTP para envio de evento
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9001/enviar-evento"))
                .timeout(Duration.ofMillis(TIMEOUT_ENVIO))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventoJson))
                .build();

        // Obter resposta HTTP com o JSON retornado
        HttpResponse<String> respEnvioEvento = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());

        // Desserializar o JSON e retornar o objeto SituacaoEnvio
        return gson.fromJson(respEnvioEvento.body(), SituacaoEvento.class);
    }

    private static Evento criarEvento() {
        return new Evento()
                .setCodSistema("SIGMA")
                .setCodAtividade("PLANILHA_EXPORTADA")
                .setDescricao("Exportacao de planilha")
                .setDestinoIp("122.141.31.0")
                .setDestinoUrl("https://sigma.tre-pe.jus.br/sigma/planilha.xhtml")
                .setDestinoPorta("80")
                .setDataHora(OffsetDateTime.now().toString())
                .setOrigemIp("231.244.41.41")
                .setOrigemHorasFuso("-03:00")
                .setUsuarioLogin("0427614398111")
                .setUsuarioNome("Epaminondas Silveira")
                .setUsuarioSiglaLotacao("SEJAR")
                .setPropriedades(Map.of("CPF", "189.183.238-31"));
    }
}
