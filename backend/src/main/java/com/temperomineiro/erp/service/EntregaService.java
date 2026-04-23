package com.temperomineiro.erp.service;

import com.temperomineiro.erp.dto.EntregaDto;
import com.temperomineiro.erp.exception.BusinessException;
import com.temperomineiro.erp.exception.ResourceNotFoundException;
import com.temperomineiro.erp.model.DomainEnums.FormaPagamento;
import com.temperomineiro.erp.model.DomainEnums.StatusEntrega;
import com.temperomineiro.erp.model.DomainEnums.TipoEntrega;
import com.temperomineiro.erp.model.Pedido;
import com.temperomineiro.erp.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaService {

    private static final BigDecimal LIMITE_DISTANCIA_PROPRIO_KM = new BigDecimal("5.00");
    private static final BigDecimal LIMITE_DISPONIBILIDADE_KM = new BigDecimal("8.00");
    private static final Duration TEMPO_MAXIMO_ACEITE = Duration.ofMinutes(2);
    private static final int TENTATIVAS_ACEITE = 3;

    private final PedidoRepository pedidoRepository;
    private final AuthContextService authContextService;

    @Transactional
    public EntregaDto.EntregaResponse iniciarEntrega(Long pedidoId, EntregaDto.IniciarEntregaRequest request) {
        Long restauranteId = authContextService.getRestauranteId();
        Pedido pedido = pedidoRepository.findByIdAndRestauranteIdForUpdate(pedidoId, restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado."));

        aplicarDadosDeEntrada(pedido, request);
        return iniciarEntrega(pedido);
    }

    @Transactional
    public EntregaDto.EntregaResponse iniciarEntrega(Pedido pedido) {
        validarStatusInicial(pedido);
        validarUberDeliveryExistente(pedido);

        TipoEntrega tipoSolicitado = pedido.getTipoEntrega() == null ? TipoEntrega.AUTOMATICO : pedido.getTipoEntrega();
        FormaPagamento formaPagamento = pedido.getFormaPagamento() == null ? FormaPagamento.ONLINE : pedido.getFormaPagamento();
        pedido.setFormaPagamento(formaPagamento);

        TipoEntrega tipoExecutado = resolverTipoEntrega(tipoSolicitado, formaPagamento, pedido);
        boolean fallbackAcionado = false;

        pedido.setStatusEntrega(StatusEntrega.EM_PROCESSAMENTO);
        pedidoRepository.save(pedido);

        try {
            executarEntrega(pedido, tipoExecutado);
        } catch (BusinessException ex) {
            if (tipoExecutado == TipoEntrega.PROPRIO) {
                fallbackAcionado = true;
                log.warn("Entrega propria falhou no pedido {}. Iniciando fallback Uber. Motivo: {}", pedido.getId(), ex.getMessage());
                executarFallbackUber(pedido);
                tipoExecutado = TipoEntrega.UBER;
            } else {
                pedido.setStatusEntrega(StatusEntrega.ERRO);
                pedidoRepository.save(pedido);
                throw ex;
            }
        } catch (RuntimeException ex) {
            pedido.setStatusEntrega(StatusEntrega.ERRO);
            pedidoRepository.save(pedido);
            throw new BusinessException("Falha inesperada ao iniciar a entrega.");
        }

        finalizarEntrega(pedido);
        Pedido salvo = pedidoRepository.save(pedido);

        String mensagem = fallbackAcionado
                ? "Entrega iniciada via fallback Uber apos falha no entregador proprio."
                : "Entrega iniciada com sucesso.";

        return new EntregaDto.EntregaResponse(
                salvo.getId(),
                tipoSolicitado,
                tipoExecutado,
                salvo.getFormaPagamento(),
                salvo.getStatusEntrega(),
                salvo.getDistanciaEntrega(),
                salvo.getUberDeliveryId(),
                fallbackAcionado,
                mensagem
        );
    }

    private void aplicarDadosDeEntrada(Pedido pedido, EntregaDto.IniciarEntregaRequest request) {
        if (request == null) {
            if (pedido.getTipoEntrega() == null) {
                pedido.setTipoEntrega(TipoEntrega.AUTOMATICO);
            }
            if (pedido.getFormaPagamento() == null) {
                pedido.setFormaPagamento(FormaPagamento.ONLINE);
            }
            if (pedido.getStatusEntrega() == null) {
                pedido.setStatusEntrega(StatusEntrega.AGUARDANDO);
            }
            if (pedido.getDistanciaEntrega() == null) {
                pedido.setDistanciaEntrega(BigDecimal.ZERO);
            }
            return;
        }

        if (request.tipoEntrega() != null) {
            pedido.setTipoEntrega(request.tipoEntrega());
        } else if (pedido.getTipoEntrega() == null) {
            pedido.setTipoEntrega(TipoEntrega.AUTOMATICO);
        }

        if (request.formaPagamento() != null) {
            pedido.setFormaPagamento(request.formaPagamento());
        } else if (pedido.getFormaPagamento() == null) {
            pedido.setFormaPagamento(FormaPagamento.ONLINE);
        }

        if (request.distanciaEntrega() != null) {
            pedido.setDistanciaEntrega(request.distanciaEntrega());
        } else if (pedido.getDistanciaEntrega() == null) {
            pedido.setDistanciaEntrega(BigDecimal.ZERO);
        }

        if (pedido.getStatusEntrega() == null) {
            pedido.setStatusEntrega(StatusEntrega.AGUARDANDO);
        }
    }

    private void validarStatusInicial(Pedido pedido) {
        StatusEntrega statusAtual = pedido.getStatusEntrega() == null ? StatusEntrega.AGUARDANDO : pedido.getStatusEntrega();
        if (statusAtual != StatusEntrega.AGUARDANDO) {
            throw new BusinessException("Entrega ja iniciada para este pedido. Status atual: " + statusAtual);
        }
    }

    private void validarUberDeliveryExistente(Pedido pedido) {
        if (StringUtils.hasText(pedido.getUberDeliveryId())) {
            throw new BusinessException("Pedido ja possui uberDeliveryId. Nova chamada de entrega nao permitida.");
        }
    }

    private TipoEntrega resolverTipoEntrega(TipoEntrega tipoSolicitado, FormaPagamento formaPagamento, Pedido pedido) {
        if (formaPagamento == FormaPagamento.DINHEIRO) {
            if (tipoSolicitado != TipoEntrega.PROPRIO) {
                log.info("Pedido {} com pagamento em dinheiro. Forcando entrega propria.", pedido.getId());
            }
            return TipoEntrega.PROPRIO;
        }

        if (tipoSolicitado != TipoEntrega.AUTOMATICO) {
            return tipoSolicitado;
        }

        BigDecimal distancia = defaultDistancia(pedido.getDistanciaEntrega());
        boolean entregadorDisponivel = existeEntregadorDisponivel(distancia);
        boolean horarioPico = isHorarioPico();

        if (entregadorDisponivel && distancia.compareTo(LIMITE_DISTANCIA_PROPRIO_KM) < 0 && !horarioPico) {
            return TipoEntrega.PROPRIO;
        }

        if (!entregadorDisponivel || horarioPico) {
            return TipoEntrega.UBER;
        }

        return TipoEntrega.UBER;
    }

    private void executarEntrega(Pedido pedido, TipoEntrega tipoEntrega) {
        if (tipoEntrega == TipoEntrega.PROPRIO) {
            executarEntregaPropria(pedido);
            return;
        }
        executarEntregaUber(pedido);
    }

    public void executarEntregaPropria(Pedido pedido) {
        log.info("Iniciando entrega propria do pedido {}.", pedido.getId());

        boolean aceito = simularAceiteEntregador(pedido);
        if (!aceito) {
            throw new BusinessException("Nenhum entregador aceitou em ate 2 minutos (simulado).");
        }

        pedido.setTipoEntrega(TipoEntrega.PROPRIO);
        pedido.setStatusEntrega(StatusEntrega.ENVIADO);
        log.info("Entrega propria enviada para o pedido {}.", pedido.getId());
    }

    public void executarEntregaUber(Pedido pedido) {
        if (StringUtils.hasText(pedido.getUberDeliveryId())) {
            throw new BusinessException("Pedido ja vinculado a entrega Uber.");
        }

        log.info("Iniciando entrega Uber para o pedido {}.", pedido.getId());
        String uberDeliveryId = "UBER-" + pedido.getId() + "-" + System.currentTimeMillis();
        pedido.setUberDeliveryId(uberDeliveryId);
        pedido.setTipoEntrega(TipoEntrega.UBER);
        pedido.setStatusEntrega(StatusEntrega.ENVIADO);
        log.info("Entrega Uber criada para pedido {} com uberDeliveryId {}.", pedido.getId(), uberDeliveryId);
    }

    private void executarFallbackUber(Pedido pedido) {
        try {
            executarEntregaUber(pedido);
        } catch (BusinessException ex) {
            pedido.setStatusEntrega(StatusEntrega.ERRO);
            pedidoRepository.save(pedido);
            throw new BusinessException("Falha na entrega propria e no fallback Uber: " + ex.getMessage());
        }
    }

    private void finalizarEntrega(Pedido pedido) {
        if (pedido.getStatusEntrega() != StatusEntrega.ENVIADO) {
            throw new BusinessException("Entrega nao pode ser finalizada sem status ENVIADO.");
        }
        pedido.setStatusEntrega(StatusEntrega.ENTREGUE);
        log.info("Pedido {} finalizado com status ENTREGUE.", pedido.getId());
    }

    private boolean simularAceiteEntregador(Pedido pedido) {
        BigDecimal distancia = defaultDistancia(pedido.getDistanciaEntrega());
        for (int tentativa = 1; tentativa <= TENTATIVAS_ACEITE; tentativa++) {
            boolean disponivel = existeEntregadorDisponivel(distancia);
            boolean aceitou = disponivel && distancia.compareTo(LIMITE_DISTANCIA_PROPRIO_KM) < 0;

            log.info(
                    "Tentativa {} de {} para pedido {} (janela total simulada de {}s). Aceite={}",
                    tentativa,
                    TENTATIVAS_ACEITE,
                    pedido.getId(),
                    TEMPO_MAXIMO_ACEITE.toSeconds(),
                    aceitou
            );

            if (aceitou) {
                return true;
            }
            pauseForSimulation();
        }
        return false;
    }

    private boolean existeEntregadorDisponivel(BigDecimal distancia) {
        return distancia.compareTo(LIMITE_DISPONIBILIDADE_KM) < 0;
    }

    private boolean isHorarioPico() {
        LocalTime agora = LocalTime.now();
        boolean picoAlmoco = !agora.isBefore(LocalTime.of(11, 0)) && agora.isBefore(LocalTime.of(14, 0));
        boolean picoJantar = !agora.isBefore(LocalTime.of(18, 0)) && agora.isBefore(LocalTime.of(21, 0));
        return picoAlmoco || picoJantar;
    }

    private BigDecimal defaultDistancia(BigDecimal distanciaEntrega) {
        return distanciaEntrega == null ? BigDecimal.ZERO : distanciaEntrega;
    }

    private void pauseForSimulation() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Processo de busca de entregador interrompido.");
        }
    }
}
