package com.temperomineiro.erp.service;

import com.temperomineiro.erp.dto.CommonDto;
import com.temperomineiro.erp.dto.PedidoDto;
import com.temperomineiro.erp.dto.PublicDto;
import com.temperomineiro.erp.exception.BusinessException;
import com.temperomineiro.erp.exception.ResourceNotFoundException;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.MesaStatus;
import com.temperomineiro.erp.model.DomainEnums.OrderOrigin;
import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import com.temperomineiro.erp.model.ItemPedido;
import com.temperomineiro.erp.model.Mesa;
import com.temperomineiro.erp.model.Pedido;
import com.temperomineiro.erp.model.Produto;
import com.temperomineiro.erp.model.User;
import com.temperomineiro.erp.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaService mesaService;
    private final ProdutoService produtoService;
    private final AuthContextService authContextService;
    private final NotificationService notificationService;
    private final PageMapperService pageMapperService;

    @Transactional(readOnly = true)
    public CommonDto.PageResponse<PedidoDto.PedidoResponse> list(PedidoStatus status, Long mesaId, int page, int size) {
        Long restauranteId = authContextService.getRestauranteId();
        var pageable = PageRequest.of(page, size);
        var result = status != null
                ? pedidoRepository.findByRestauranteIdAndStatus(restauranteId, status, pageable)
                : mesaId != null
                ? pedidoRepository.findByRestauranteIdAndMesaId(restauranteId, mesaId, pageable)
                : pedidoRepository.findByRestauranteId(restauranteId, pageable);

        return pageMapperService.toPageResponse(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PedidoDto.PedidoResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public PedidoDto.PedidoResponse create(PedidoDto.CriarPedidoRequest request) {
        User currentUser = authContextService.getCurrentUserEntity();
        Mesa mesa = mesaService.getEntity(request.mesaId());
        Pedido pedido = createOrder(
                currentUser.getRestaurante().getId(),
                mesa,
                currentUser,
                request.origem(),
                request.observacoes(),
                request.itens(),
                defaultValue(request.desconto()),
                defaultValue(request.taxaServico()),
                defaultTipoEntrega(request.tipoEntrega()),
                defaultFormaPagamento(request.formaPagamento()),
                defaultValue(request.distanciaEntrega())
        );
        return toResponse(pedido);
    }

    @Transactional
    public PedidoDto.PedidoResponse createPublicOrder(String restaurantSlug, PublicDto.PublicOrderRequest request) {
        Mesa mesa = mesaService.getByPublicTokenAndRestaurantSlug(request.mesaToken(), restaurantSlug);
        List<PedidoDto.PedidoItemRequest> items = request.itens().stream()
                .map(item -> new PedidoDto.PedidoItemRequest(item.produtoId(), item.quantidade(), item.observacoes()))
                .toList();
        Pedido pedido = createOrder(
                mesa.getRestaurante().getId(),
                mesa,
                null,
                OrderOrigin.CARDAPIO_DIGITAL,
                request.observacoes(),
                items,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TipoEntrega.AUTOMATICO,
                FormaPagamento.ONLINE,
                BigDecimal.ZERO
        );
        return toResponse(pedido);
    }

    @Transactional
    public PedidoDto.PedidoResponse addItem(Long pedidoId, PedidoDto.PedidoItemRequest request) {
        Pedido pedido = getEntity(pedidoId);
        ensureEditable(pedido);
        pedido.getItens().add(buildItem(pedido, request, pedido.getRestaurante().getId()));
        recalculateTotals(pedido);
        Pedido saved = pedidoRepository.save(pedido);
        notificationService.publish(saved.getRestaurante().getId(), "PEDIDO_ATUALIZADO", "Pedido atualizado com novo item.", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PedidoDto.PedidoResponse removeItem(Long pedidoId, Long itemId) {
        Pedido pedido = getEntity(pedidoId);
        ensureEditable(pedido);
        boolean removed = pedido.getItens().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Item do pedido não encontrado.");
        }
        if (pedido.getItens().isEmpty()) {
            throw new BusinessException("O pedido não pode ficar sem itens.");
        }
        recalculateTotals(pedido);
        Pedido saved = pedidoRepository.save(pedido);
        notificationService.publish(saved.getRestaurante().getId(), "PEDIDO_ATUALIZADO", "Item removido do pedido.", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PedidoDto.PedidoResponse updateStatus(Long pedidoId, PedidoDto.AtualizarStatusPedidoRequest request) {
        Pedido pedido = getEntity(pedidoId);
        pedido.setStatus(request.status());
        if (request.status() == PedidoStatus.PRONTO) {
            pedido.setProntoEm(OffsetDateTime.now());
            notificationService.publish(pedido.getRestaurante().getId(), "PEDIDO_PRONTO", "Pedido pronto para entrega.", pedido.getId());
        } else if (request.status() == PedidoStatus.ENTREGUE) {
            pedido.setEntregueEm(OffsetDateTime.now());
            notificationService.publish(pedido.getRestaurante().getId(), "PEDIDO_ENTREGUE", "Pedido entregue na mesa.", pedido.getId());
        } else if (request.status() == PedidoStatus.CANCELADO) {
            notificationService.publish(pedido.getRestaurante().getId(), "PEDIDO_CANCELADO", "Pedido cancelado.", pedido.getId());
        }
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public Pedido getEntity(Long id) {
        return pedidoRepository.findByIdAndRestauranteId(id, authContextService.getRestauranteId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Pedido> getOpenOrdersByMesa(Long restauranteId, Long mesaId) {
        return pedidoRepository.findByRestauranteIdAndMesaIdAndStatusNot(restauranteId, mesaId, PedidoStatus.FECHADO);
    }

    @Transactional(readOnly = true)
    public List<PedidoDto.PedidoResponse> getKitchenOrders() {
        return pedidoRepository.findByRestauranteIdAndStatusInOrderByAbertoEmAsc(
                        authContextService.getRestauranteId(),
                        List.of(PedidoStatus.EM_PREPARO, PedidoStatus.PRONTO)
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    private Pedido createOrder(Long restauranteId,
                               Mesa mesa,
                               User user,
                               OrderOrigin origem,
                               String observacoes,
                               List<PedidoDto.PedidoItemRequest> itemRequests,
                               BigDecimal desconto,
                               BigDecimal taxaServico,
                               TipoEntrega tipoEntrega,
                               FormaPagamento formaPagamento,
                               BigDecimal distanciaEntrega) {
        if (!mesa.isAtiva()) {
            throw new BusinessException("Mesa inativa não pode receber pedidos.");
        }

        Pedido pedido = Pedido.builder()
                .restaurante(mesa.getRestaurante())
                .mesa(mesa)
                .usuario(user)
                .status(PedidoStatus.EM_PREPARO)
                .origem(origem)
                .observacoes(observacoes)
                .subtotal(BigDecimal.ZERO)
                .desconto(desconto)
                .taxaServico(taxaServico)
                .total(BigDecimal.ZERO)
                .tipoEntrega(tipoEntrega)
                .statusEntrega(StatusEntrega.AGUARDANDO)
                .formaPagamento(formaPagamento)
                .distanciaEntrega(distanciaEntrega)
                .abertoEm(OffsetDateTime.now())
                .itens(new ArrayList<>())
                .build();

        for (PedidoDto.PedidoItemRequest itemRequest : itemRequests) {
            pedido.getItens().add(buildItem(pedido, itemRequest, restauranteId));
        }

        recalculateTotals(pedido);
        mesa.setStatus(MesaStatus.OCUPADA);
        if (mesa.getAbertaEm() == null) {
            mesa.setAbertaEm(OffsetDateTime.now());
        }

        Pedido saved = pedidoRepository.save(pedido);
        notificationService.publish(restauranteId, "PEDIDO_ENVIADO", "Novo pedido enviado para a cozinha.", saved.getId());
        return saved;
    }

    private ItemPedido buildItem(Pedido pedido, PedidoDto.PedidoItemRequest request, Long restauranteId) {
        Produto produto = produtoService.getEntity(request.produtoId(), restauranteId);
        if (!produto.isDisponivel()) {
            throw new BusinessException("Produto " + produto.getNome() + " está indisponível.");
        }
        BigDecimal total = produto.getPreco().multiply(BigDecimal.valueOf(request.quantidade()));
        return ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(request.quantidade())
                .precoUnitario(produto.getPreco())
                .total(total)
                .observacoes(request.observacoes())
                .build();
    }

    private void recalculateTotals(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setSubtotal(subtotal);
        pedido.setTotal(subtotal.add(defaultValue(pedido.getTaxaServico())).subtract(defaultValue(pedido.getDesconto())));
    }

    private void ensureEditable(Pedido pedido) {
        if (pedido.getStatus() == PedidoStatus.FECHADO || pedido.getStatus() == PedidoStatus.CANCELADO || pedido.getStatus() == PedidoStatus.ENTREGUE) {
            throw new BusinessException("Este pedido não pode mais ser alterado.");
        }
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private TipoEntrega defaultTipoEntrega(TipoEntrega tipoEntrega) {
        return tipoEntrega == null ? TipoEntrega.AUTOMATICO : tipoEntrega;
    }

    private FormaPagamento defaultFormaPagamento(FormaPagamento formaPagamento) {
        return formaPagamento == null ? FormaPagamento.ONLINE : formaPagamento;
    }

    public PedidoDto.PedidoResponse toResponse(Pedido pedido) {
        return new PedidoDto.PedidoResponse(
                pedido.getId(),
                pedido.getMesa().getId(),
                pedido.getMesa().getNome(),
                pedido.getUsuario() != null ? pedido.getUsuario().getId() : null,
                pedido.getUsuario() != null ? pedido.getUsuario().getNome() : "Cardápio digital",
                pedido.getStatus(),
                pedido.getOrigem(),
                pedido.getObservacoes(),
                pedido.getItens().stream()
                        .map(item -> new PedidoDto.ItemPedidoResponse(
                                item.getId(),
                                item.getProduto().getId(),
                                item.getProduto().getNome(),
                                item.getQuantidade(),
                                item.getPrecoUnitario(),
                                item.getTotal(),
                                item.getObservacoes()
                        ))
                        .toList(),
                pedido.getSubtotal(),
                pedido.getDesconto(),
                pedido.getTaxaServico(),
                pedido.getTotal(),
                pedido.getTipoEntrega(),
                pedido.getStatusEntrega(),
                pedido.getFormaPagamento(),
                pedido.getDistanciaEntrega(),
                pedido.getUberDeliveryId(),
                pedido.getAbertoEm(),
                pedido.getProntoEm(),
                pedido.getEntregueEm(),
                pedido.getFechadoEm()
        );
    }
}
