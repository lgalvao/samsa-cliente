package samsa.cliente.modelo;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SituacaoEnvio {
    CodSituacaoEnvio codSituacao;
    String codEvento;
    Map<String, String> erros;
    Timestamp timestamp;
    String status;
    String error;
    String trace;
    String message;
    String path;
}
