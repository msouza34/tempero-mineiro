package com.temperomineiro.erp.model;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum RoleName {
        ADMIN,
        GERENTE,
        GARCOM,
        COZINHA,
        CAIXA
    }

    public enum MesaStatus {
        LIVRE,
        OCUPADA,
        RESERVADA
    }

    public enum PedidoStatus {
        EM_PREPARO,
        PRONTO,
        ENTREGUE,
        FECHADO,
        CANCELADO
    }

    public enum PaymentMethod {
        DINHEIRO,
        CARTAO,
        PIX
    }

    public enum PaymentStatus {
        PENDENTE,
        CONCLUIDO,
        CANCELADO
    }

    public enum InventoryMovementType {
        ENTRADA,
        SAIDA,
        AJUSTE
    }

    public enum UnitMeasure {
        UNIDADE,
        KG,
        G,
        L,
        ML
    }

    public enum OrderOrigin {
        SALAO,
        CARDAPIO_DIGITAL
    }

    public enum TipoEntrega {
        PROPRIO,
        UBER,
        AUTOMATICO
    }

    public enum FormaPagamento {
        DINHEIRO,
        ONLINE
    }

    public enum StatusEntrega {
        AGUARDANDO,
        EM_PROCESSAMENTO,
        ENVIADO,
        ENTREGUE,
        ERRO
    }
}
