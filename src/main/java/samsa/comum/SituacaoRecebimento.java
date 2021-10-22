package samsa.comum;

import lombok.Data;

import java.util.Map;

@Data
public class SituacaoRecebimento {
    CodSituacaoRecebimento codSituacao;
    String codEvento;
    Map<String, String> erros;
    String dataHoraConfirmacao;
    Evento evento;
}
