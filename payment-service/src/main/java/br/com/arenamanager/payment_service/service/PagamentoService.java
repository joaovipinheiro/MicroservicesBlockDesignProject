package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.client.PlayerClient;
import br.com.arenamanager.payment_service.client.PlayerDTO;
import br.com.arenamanager.payment_service.dto.EventoPagamentoAprovado;
import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.model.StatusPagamento;
import br.com.arenamanager.payment_service.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PlayerClient playerClient;
    private final PagamentoPublisherService pagamentoPublisherService;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            PlayerClient playerClient,
                            PagamentoPublisherService pagamentoPublisherService) {
        this.pagamentoRepository = pagamentoRepository;
        this.playerClient = playerClient;
        this.pagamentoPublisherService = pagamentoPublisherService;
    }

    @Transactional
    public Pagamento criarPagamento(Long usuarioId, Long torneioId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero!");
        }

        Pagamento pagamento = new Pagamento(usuarioId, torneioId, valor);
        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento = pagamentoRepository.save(pagamento);

        // Comunicação síncrona com Player Service para obter os dados do jogador
        PlayerDTO jogador = playerClient.obterJogadorPorId(usuarioId);

        // Criação do payload limpo para o Kafka
        EventoPagamentoAprovado evento = new EventoPagamentoAprovado(
                pagamento.getId(),
                jogador.nome(),
                jogador.email(),
                torneioId,
                valor
        );

        pagamentoPublisherService.publicarPagamentoAprovado(evento);

        return pagamento;
    }

    public Pagamento buscarPorId(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado com o ID: " + id));
    }
}