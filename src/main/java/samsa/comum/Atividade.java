package samsa.comum;

import lombok.Data;

/**
 * Representa a categoria/tipo de um evento.
 */
@Data
public class Atividade {
    String codSistema;
    String codAtividade;
    String descAtividade;
}
