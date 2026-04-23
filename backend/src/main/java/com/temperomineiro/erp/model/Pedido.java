package com.temperomineiro.erp.model;

import com.temperomineiro.erp.model.DomainEnums.OrderOrigin;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurante_id")
    private Restaurante restaurante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderOrigin origem;

    @Column(length = 1200)
    private String observacoes;

    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaServico;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private TipoEntrega tipoEntrega;

    @Enumerated(EnumType.STRING)
    private StatusEntrega statusEntrega;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanciaEntrega;

    @Column(length = 120)
    private String uberDeliveryId;

    @Column(nullable = false)
    private OffsetDateTime abertoEm;

    @Column
    private OffsetDateTime prontoEm;

    @Column
    private OffsetDateTime entregueEm;

    @Column
    private OffsetDateTime fechadoEm;
}
