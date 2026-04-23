package com.temperomineiro.erp.controller;

import com.temperomineiro.erp.dto.CommonDto;
import com.temperomineiro.erp.dto.EntregaDto;
import com.temperomineiro.erp.dto.PedidoDto;
import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.service.EntregaService;
import com.temperomineiro.erp.service.PedidoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final EntregaService entregaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM','COZINHA','CAIXA')")
    public CommonDto.PageResponse<PedidoDto.PedidoResponse> list(
            @RequestParam(required = false) PedidoStatus status,
            @RequestParam(required = false) Long mesaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return pedidoService.list(status, mesaId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM','COZINHA','CAIXA')")
    public PedidoDto.PedidoResponse getById(@PathVariable Long id) {
        return pedidoService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM')")
    public PedidoDto.PedidoResponse create(@Valid @RequestBody PedidoDto.CriarPedidoRequest request) {
        return pedidoService.create(request);
    }

    @PostMapping("/{id}/itens")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM')")
    public PedidoDto.PedidoResponse addItem(@PathVariable Long id, @Valid @RequestBody PedidoDto.PedidoItemRequest request) {
        return pedidoService.addItem(id, request);
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM')")
    public PedidoDto.PedidoResponse removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return pedidoService.removeItem(id, itemId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM','COZINHA')")
    public PedidoDto.PedidoResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PedidoDto.AtualizarStatusPedidoRequest request) {
        return pedidoService.updateStatus(id, request);
    }

    @PostMapping("/{id}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','GARCOM','CAIXA')")
    public EntregaDto.EntregaResponse iniciarEntrega(@PathVariable Long id,
                                                     @Valid @RequestBody(required = false) EntregaDto.IniciarEntregaRequest request) {
        return entregaService.iniciarEntrega(id, request);
    }
}
