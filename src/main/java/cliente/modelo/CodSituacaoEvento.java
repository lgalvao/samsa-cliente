package cliente.modelo;

import lombok.Getter;

@Getter
public enum CodSituacaoEvento {
    ACEITO,
    REJEITADO,
    CONFIRMADO,
    NAO_ENCONTRADO,
    ERRO_INTERNO
}
