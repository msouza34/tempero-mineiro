package com.temperomineiro.erp.dto;

import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public final class EntregaDto {

    private EntregaDto() {
    }

    public record IniciarEntregaRequest(
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento,
            @DecimalMin("0.00") BigDecimal distanciaEntrega
    ) {
    }

    public record EntregaResponse(
            Long pedidoId,
            TipoEntrega tipoEntregaSolicitada,
            TipoEntrega tipoEntregaExecutada,
            FormaPagamento formaPagamento,
            StatusEntrega statusEntrega,
            BigDecimal distanciaEntrega,
            String uberDeliveryId,
            boolean fallbackAcionado,
            String mensagem
    ) {
    }
}
