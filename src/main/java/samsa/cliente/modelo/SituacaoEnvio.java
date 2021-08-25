package samsa.cliente.modelo;

import lombok.Data;

import java.util.Map;

@Data
public class SituacaoEnvio {
    CodSituacaoEnvio codSituacao;
    String codEvento;
    Map<String, String> erros;
}
