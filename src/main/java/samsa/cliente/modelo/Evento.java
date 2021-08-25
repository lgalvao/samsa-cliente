package samsa.cliente.modelo;

import lombok.Data;

import java.util.Map;

@Data
public class Evento {
    String codSistema;
    String codAtividade;
    String dataHora;
    String descricao;
    String destinoIp;
    String destinoPorta;
    String destinoUrl;
    String origemIp;
    String origemHorasFuso;
    String usuarioLogin;
    String usuarioNome;
    String usuarioSiglaLotacao;
    Map<String, String> propriedades;
}
