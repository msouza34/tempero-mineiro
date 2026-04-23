package com.temperomineiro.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.temperomineiro.erp.dto.CatalogDto;
import com.temperomineiro.erp.dto.EstoqueDto;
import com.temperomineiro.erp.dto.MesaDto;
import com.temperomineiro.erp.dto.PedidoDto;
import com.temperomineiro.erp.exception.BusinessException;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.MesaStatus;
import com.temperomineiro.erp.model.DomainEnums.OrderOrigin;
import com.temperomineiro.erp.model.DomainEnums.RoleName;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import com.temperomineiro.erp.model.DomainEnums.UnitMeasure;
import com.temperomineiro.erp.model.Restaurante;
import com.temperomineiro.erp.model.Role;
import com.temperomineiro.erp.model.User;
import com.temperomineiro.erp.repository.PedidoRepository;
import com.temperomineiro.erp.repository.RestauranteRepository;
import com.temperomineiro.erp.repository.RoleRepository;
import com.temperomineiro.erp.repository.UserRepository;
import com.temperomineiro.erp.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EntregaServiceIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private EntregaService entregaService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder()
                    .name(roleName)
                    .description("Perfil " + roleName.name())
                    .build()));
        }

        Restaurante restaurante = restauranteRepository.save(Restaurante.builder()
                .nome("Tempero Entrega Teste")
                .slug("tempero-entrega-" + System.nanoTime())
                .ativo(true)
                .build());

        User user = userRepository.save(User.builder()
                .restaurante(restaurante)
                .nome("Administrador Entrega")
                .email("admin.entrega." + System.nanoTime() + "@temperomineiro.com")
                .password(passwordEncoder.encode("Senha@2026"))
                .ativo(true)
                .roles(Set.of(roleRepository.findByName(RoleName.ADMIN).orElseThrow()))
                .build());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    void shouldFallbackToUberWhenProprioFails() {
        Long pedidoId = criarPedido(TipoEntrega.PROPRIO, FormaPagamento.ONLINE, new BigDecimal("9.00"));

        var response = entregaService.iniciarEntrega(pedidoId, null);

        assertTrue(response.fallbackAcionado());
        assertEquals(TipoEntrega.UBER, response.tipoEntregaExecutada());
        assertEquals(StatusEntrega.ENTREGUE, response.statusEntrega());
        assertNotNull(response.uberDeliveryId());
    }

    @Test
    void shouldForceProprioWhenPaymentIsCash() {
        Long pedidoId = criarPedido(TipoEntrega.UBER, FormaPagamento.DINHEIRO, new BigDecimal("3.00"));

        var response = entregaService.iniciarEntrega(pedidoId, null);

        assertFalse(response.fallbackAcionado());
        assertEquals(TipoEntrega.PROPRIO, response.tipoEntregaExecutada());
        assertEquals(StatusEntrega.ENTREGUE, response.statusEntrega());
        assertNull(response.uberDeliveryId());
    }

    @Test
    void shouldBlockIfDeliveryAlreadyStarted() {
        Long pedidoId = criarPedido(TipoEntrega.PROPRIO, FormaPagamento.ONLINE, new BigDecimal("3.00"));
        entregaService.iniciarEntrega(pedidoId, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> entregaService.iniciarEntrega(pedidoId, null));
        assertTrue(exception.getMessage().contains("Entrega ja iniciada"));
    }

    @Test
    void shouldBlockWhenUberDeliveryIdAlreadyExists() {
        Long pedidoId = criarPedido(TipoEntrega.UBER, FormaPagamento.ONLINE, new BigDecimal("4.00"));
        var pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setUberDeliveryId("UBER-EXISTENTE-TESTE");
        pedidoRepository.save(pedido);

        BusinessException exception = assertThrows(BusinessException.class, () -> entregaService.iniciarEntrega(pedidoId, null));
        assertTrue(exception.getMessage().contains("uberDeliveryId"));
    }

    private Long criarPedido(TipoEntrega tipoEntrega, FormaPagamento formaPagamento, BigDecimal distanciaEntrega) {
        String suffix = String.valueOf(System.nanoTime());

        var categoria = categoriaService.create(new CatalogDto.CategoriaRequest(
                "Categoria Entrega " + suffix,
                "Teste de entrega",
                1,
                true
        ));
        var estoque = estoqueService.create(new EstoqueDto.EstoqueRequest(
                "Insumo Entrega " + suffix,
                UnitMeasure.UNIDADE,
                new BigDecimal("50.000"),
                new BigDecimal("5.000"),
                new BigDecimal("2.50"),
                true
        ));
        var produto = produtoService.create(new CatalogDto.ProdutoRequest(
                categoria.id(),
                "Produto Entrega " + suffix,
                "Produto para teste de entrega",
                new BigDecimal("29.90"),
                null,
                true,
                List.of(new CatalogDto.ReceitaItemRequest(estoque.id(), new BigDecimal("1.000")))
        ));
        var mesa = mesaService.create(new MesaDto.MesaRequest("Mesa Entrega " + suffix, 4, MesaStatus.LIVRE, true));

        var pedido = pedidoService.create(new PedidoDto.CriarPedidoRequest(
                mesa.id(),
                "Pedido para teste de entrega",
                OrderOrigin.SALAO,
                List.of(new PedidoDto.PedidoItemRequest(produto.id(), 1, "Sem observacoes")),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                tipoEntrega,
                formaPagamento,
                distanciaEntrega
        ));

        return pedido.id();
    }
}
