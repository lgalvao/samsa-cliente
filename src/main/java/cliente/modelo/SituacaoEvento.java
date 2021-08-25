package cliente.modelo;

import lombok.Data;

import java.util.Map;

@Data
public class SituacaoEvento {
    CodSituacaoEvento codSituacaoEvento;
    String codEvento;
    String dataHoraConfirmacao;
    Map<String, String> erros;
}
