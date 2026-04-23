package com.temperomineiro.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.temperomineiro.erp.dto.CatalogDto;
import com.temperomineiro.erp.dto.EstoqueDto;
import com.temperomineiro.erp.dto.MesaDto;
import com.temperomineiro.erp.dto.PaymentDto;
import com.temperomineiro.erp.dto.PedidoDto;
import com.temperomineiro.erp.model.DomainEnums.MesaStatus;
import com.temperomineiro.erp.model.DomainEnums.OrderOrigin;
import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.RoleName;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import com.temperomineiro.erp.model.DomainEnums.UnitMeasure;
import com.temperomineiro.erp.model.Restaurante;
import com.temperomineiro.erp.model.Role;
import com.temperomineiro.erp.model.User;
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
class PedidoFluxoIntegrationTest {

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
    private CaixaService caixaService;

    private Restaurante restaurante;

    @BeforeEach
    void setUp() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder()
                    .name(roleName)
                    .description("Perfil " + roleName.name())
                    .build()));
        }

        restaurante = restauranteRepository.save(Restaurante.builder()
                .nome("Tempero Teste")
                .slug("tempero-fluxo")
                .ativo(true)
                .build());

        User user = userRepository.save(User.builder()
                .restaurante(restaurante)
                .nome("Garcom")
                .email("garcom.fluxo@temperomineiro.com")
                .password(passwordEncoder.encode("Senha@2026"))
                .ativo(true)
                .roles(Set.of(roleRepository.findByName(RoleName.GARCOM).orElseThrow()))
                .build());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    void shouldCreateDeliverAndCloseOrderWithStockConsumption() {
        var categoria = categoriaService.create(new CatalogDto.CategoriaRequest("Pratos", "Pratos da casa", 1, true));
        var estoque = estoqueService.create(new EstoqueDto.EstoqueRequest(
                "Feijao", UnitMeasure.KG, new BigDecimal("10.000"), new BigDecimal("2.000"), new BigDecimal("8.00"), true
        ));
        var produto = produtoService.create(new CatalogDto.ProdutoRequest(
                categoria.id(),
                "Feijao Tropeiro",
                "Tradicional mineiro",
                new BigDecimal("35.00"),
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO9W6h8AAAAASUVORK5CYII=",
                true,
                List.of(new CatalogDto.ReceitaItemRequest(estoque.id(), new BigDecimal("0.500")))
        ));
        var mesa = mesaService.create(new MesaDto.MesaRequest("Mesa 1", 4, MesaStatus.LIVRE, true));

        var pedido = pedidoService.create(new PedidoDto.CriarPedidoRequest(
                mesa.id(),
                "Sem cebola",
                OrderOrigin.SALAO,
                List.of(new PedidoDto.PedidoItemRequest(produto.id(), 2, "Caprichar na couve")),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TipoEntrega.AUTOMATICO,
                FormaPagamento.ONLINE,
                new BigDecimal("3.50")
        ));

        assertNotNull(pedido.id());
        assertEquals(new BigDecimal("70.00"), pedido.total());
        assertNotNull(produto.imagemUrl());

        pedidoService.updateStatus(pedido.id(), new PedidoDto.AtualizarStatusPedidoRequest(PedidoStatus.PRONTO));
        pedidoService.updateStatus(pedido.id(), new PedidoDto.AtualizarStatusPedidoRequest(PedidoStatus.ENTREGUE));
        caixaService.registrarPagamento(mesa.id(), new PaymentDto.RegistrarPagamentoRequest(
                com.temperomineiro.erp.model.DomainEnums.PaymentMethod.PIX,
                new BigDecimal("70.00"),
                "Pagamento integral"
        ));
        caixaService.fecharConta(mesa.id());

        var estoqueAtualizado = estoqueService.list("", 0, 10).content().stream().findFirst().orElseThrow();
        assertEquals(0, estoqueAtualizado.quantidadeAtual().compareTo(new BigDecimal("9.000")));
    }
}
