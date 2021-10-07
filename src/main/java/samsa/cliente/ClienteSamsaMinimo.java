package samsa.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import samsa.cliente.modelo.CodSituacaoEnvio;
import samsa.cliente.modelo.Evento;
import samsa.cliente.modelo.SituacaoEnvio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static java.lang.System.out;

/**
 * Exemplo mínimo de acesso a API do SAMSA. Presume existência do
 * sistema e das atividades auditadas, e não verifica o recebimento no Kafka
 */
public class ClienteSamsaMinimo {
    // Simula uma interacao simples com o SAMSA, presumindo que sistema e atividades estejam OK.
    // Não verifica se evento de fato chegou ao Kafka
    void executar() throws Exception {
        // Montar objeto Evento
        Evento evento = criarEvento();

        // Enviar objeto Evento (serializado para JSON)
        SituacaoEnvio sitEnvio = enviarEvento(evento);

        // Verificar retorno do envio: ACEITO -> passou na validacao; REJEITADO -> 'erros' vem preenchido
        if (sitEnvio.getCodSituacao() == CodSituacaoEnvio.ACEITO)
            out.printf("Evento enviado. Código: %s %n", sitEnvio.getCodEvento());
        else
            out.printf("Evento não enviado. Erros: %s %n", sitEnvio.getErros());
    }

    // Simula criacao de evento. Nos sistemas, feito onde o request/usuário estejam acessíveis
    Evento criarEvento() {
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
                .setUsuarioSiglaLotacao("SENUIT") // lotacao do usuário atual
                .setPropriedades(Map.of("CPF", "189.183.238-31")); // propriedades adicionais específicas
    }

    // Realiza o envio do evento, serializado para JSON
    SituacaoEnvio enviarEvento(Evento evento) throws Exception {
        // Cliente http nativo do Java (11 em diante)
        HttpClient clienteHttp = HttpClient.newHttpClient();

        // Serializador/Desserializador JSON (Jackson)
        ObjectMapper mapeadorJson = new ObjectMapper();

        // Serializar evento para JSON
        String eventoJson = mapeadorJson.writeValueAsString(evento);

        // URL de envio
        String URL = "http://localhost:9001/api/evento";

        // Montar requisição HTTP para envio de evento
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofMillis(1000)) // Tempo máximo de espera para as requisições
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventoJson))
                .build();

        // Efetuar requsição e obter resposta HTTP com JSON (estrutura de SituacaoEnvio)
        HttpResponse<String> resp = clienteHttp.send(req, HttpResponse.BodyHandlers.ofString());

        // Desserializar o JSON e retornar o objeto SituacaoEnvio
        return mapeadorJson.readValue(resp.body(), SituacaoEnvio.class);
    }

    // Incluído apenas para facilitar testes
    public static void main(String[] args) throws Exception {
        new ClienteSamsaMinimo().executar();
    }
}
