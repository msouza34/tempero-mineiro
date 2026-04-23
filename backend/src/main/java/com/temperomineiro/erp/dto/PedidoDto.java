package com.temperomineiro.erp.dto;

import com.temperomineiro.erp.model.DomainEnums.OrderOrigin;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class PedidoDto {

    private PedidoDto() {
    }

    public record PedidoItemRequest(
            @NotNull Long produtoId,
            @NotNull @Min(1) Integer quantidade,
            String observacoes
    ) {
    }

    public record CriarPedidoRequest(
            @NotNull Long mesaId,
            String observacoes,
            @NotNull OrderOrigin origem,
            @NotEmpty @Valid List<PedidoItemRequest> itens,
            @DecimalMin("0.00") BigDecimal desconto,
            @DecimalMin("0.00") BigDecimal taxaServico,
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento,
            @DecimalMin("0.00") BigDecimal distanciaEntrega
    ) {
    }

    public record AtualizarStatusPedidoRequest(@NotNull PedidoStatus status) {
    }

    public record ItemPedidoResponse(
            Long id,
            Long produtoId,
            String produtoNome,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal total,
            String observacoes
    ) {
    }

    public record PedidoResponse(
            Long id,
            Long mesaId,
            String mesaNome,
            Long usuarioId,
            String usuarioNome,
            PedidoStatus status,
            OrderOrigin origem,
            String observacoes,
            List<ItemPedidoResponse> itens,
            BigDecimal subtotal,
            BigDecimal desconto,
            BigDecimal taxaServico,
            BigDecimal total,
            TipoEntrega tipoEntrega,
            StatusEntrega statusEntrega,
            FormaPagamento formaPagamento,
            BigDecimal distanciaEntrega,
            String uberDeliveryId,
            OffsetDateTime abertoEm,
            OffsetDateTime prontoEm,
            OffsetDateTime entregueEm,
            OffsetDateTime fechadoEm
    ) {
    }
}
