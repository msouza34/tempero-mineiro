package com.temperomineiro.erp.repository;

import com.temperomineiro.erp.model.DomainEnums.PedidoStatus;
import com.temperomineiro.erp.model.Pedido;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto", "itens.produto.categoria"})
    Page<Pedido> findByRestauranteIdAndStatus(Long restauranteId, PedidoStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto", "itens.produto.categoria"})
    Page<Pedido> findByRestauranteIdAndMesaId(Long restauranteId, Long mesaId, Pageable pageable);

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto", "itens.produto.categoria"})
    Page<Pedido> findByRestauranteId(Long restauranteId, Pageable pageable);

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto", "itens.produto.categoria"})
    Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Pedido p
            where p.id = :id
              and p.restaurante.id = :restauranteId
            """)
    Optional<Pedido> findByIdAndRestauranteIdForUpdate(@Param("id") Long id, @Param("restauranteId") Long restauranteId);

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto"})
    List<Pedido> findByRestauranteIdAndMesaIdAndStatusNot(Long restauranteId, Long mesaId, PedidoStatus status);

    @EntityGraph(attributePaths = {"mesa", "usuario", "itens", "itens.produto"})
    List<Pedido> findByRestauranteIdAndStatusInOrderByAbertoEmAsc(Long restauranteId, List<PedidoStatus> statuses);

    long countByRestauranteIdAndStatusIn(Long restauranteId, List<PedidoStatus> statuses);

    @Query("""
            select coalesce(sum(p.total), 0)
            from Pedido p
            where p.restaurante.id = :restauranteId
              and p.status = com.temperomineiro.erp.model.DomainEnums$PedidoStatus.FECHADO
              and p.fechadoEm between :inicio and :fim
            """)
    java.math.BigDecimal sumTotalByPeriod(@Param("restauranteId") Long restauranteId,
                                          @Param("inicio") OffsetDateTime inicio,
                                          @Param("fim") OffsetDateTime fim);

    @Query("""
            select count(p)
            from Pedido p
            where p.restaurante.id = :restauranteId
              and p.status = com.temperomineiro.erp.model.DomainEnums$PedidoStatus.FECHADO
              and p.fechadoEm between :inicio and :fim
            """)
    long countClosedByPeriod(@Param("restauranteId") Long restauranteId,
                             @Param("inicio") OffsetDateTime inicio,
                             @Param("fim") OffsetDateTime fim);

    @Query("""
            select ip.produto.nome, sum(ip.quantidade), sum(ip.total)
            from ItemPedido ip
            join ip.pedido p
            where p.restaurante.id = :restauranteId
              and p.status = com.temperomineiro.erp.model.DomainEnums$PedidoStatus.FECHADO
              and p.fechadoEm between :inicio and :fim
            group by ip.produto.nome
            order by sum(ip.quantidade) desc
            """)
    List<Object[]> topProducts(@Param("restauranteId") Long restauranteId,
                               @Param("inicio") OffsetDateTime inicio,
                               @Param("fim") OffsetDateTime fim,
                               Pageable pageable);
}
