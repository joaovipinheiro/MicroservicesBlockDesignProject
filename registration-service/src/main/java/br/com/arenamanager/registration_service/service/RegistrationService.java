package br.com.arenamanager.registration_service.service;

import br.com.arenamanager.registration_service.client.PaymentClient;
import br.com.arenamanager.registration_service.client.PlayerClient;
import br.com.arenamanager.registration_service.client.TournamentClient;
import br.com.arenamanager.registration_service.dto.TournamentResponseDTO;
import br.com.arenamanager.registration_service.domain.model.Registration;
import br.com.arenamanager.registration_service.domain.model.RegistrationStatus;
import br.com.arenamanager.registration_service.dto.PaymentRequest;
import br.com.arenamanager.registration_service.dto.RegistrationRequest;
import br.com.arenamanager.registration_service.dto.RegistrationResponse;
import br.com.arenamanager.registration_service.exception.BusinessException;
import br.com.arenamanager.registration_service.exception.ResourceNotFoundException;
import br.com.arenamanager.registration_service.repository.RegistrationRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final PaymentClient paymentClient;
    private final PlayerClient playerClient;
    private final TournamentClient tournamentClient;
    private final MeterRegistry meterRegistry;

    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackRegistration")
    public RegistrationResponse createRegistration(RegistrationRequest request) {
        validatePlayer(request.getPlayerId());
        validateTournament(request.getTournamentId());
        validateNoDuplicate(request.getPlayerId(), request.getTournamentId());

        Registration registration = new Registration();
        registration.setPlayerId(request.getPlayerId());
        registration.setTournamentId(request.getTournamentId());
        registration.setMetodoPagamento(request.getMetodoPagamento());
        registration.setValor(request.getValor());
        registration.setStatus(RegistrationStatus.AGUARDANDO_PAGAMENTO);

        Registration saved = registrationRepository.save(registration);

        PaymentRequest paymentRequest = new PaymentRequest(
                saved.getPlayerId(),
                saved.getTournamentId(),
                saved.getValor()
        );

        paymentClient.processPayment(paymentRequest);

        saved.setStatus(RegistrationStatus.CONFIRMADO);
        registrationRepository.save(saved);

        meterRegistry.counter("registrations.confirmed.total", "service", "registration-service").increment();
        log.info("Inscricao {} confirmada com pagamento.", saved.getId());
        return toResponse(saved);
    }

    public RegistrationResponse fallbackRegistration(RegistrationRequest request, Throwable ex) {
        log.warn("[CIRCUIT BREAKER] payment-service indisponivel. Motivo: {}. Inscricao salva com status AGUARDANDO_PAGAMENTO.", ex.getMessage());

        Registration registration = new Registration();
        registration.setPlayerId(request.getPlayerId());
        registration.setTournamentId(request.getTournamentId());
        registration.setMetodoPagamento(request.getMetodoPagamento());
        registration.setValor(request.getValor());
        registration.setStatus(RegistrationStatus.AGUARDANDO_PAGAMENTO);

        Registration saved = registrationRepository.save(registration);

        meterRegistry.counter("registrations.pending.total", "service", "registration-service").increment();
        return toResponse(saved);
    }

    public List<RegistrationResponse> getAllRegistrations() {
        return registrationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RegistrationResponse getById(Long id) {
        return registrationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inscricao nao encontrada: " + id));
    }

    public List<RegistrationResponse> getByPlayer(Long playerId) {
        return registrationRepository.findByPlayerId(playerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RegistrationResponse> getByTournament(Long tournamentId) {
        return registrationRepository.findByTournamentId(tournamentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RegistrationResponse cancelRegistration(Long id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscricao nao encontrada: " + id));

        if (registration.getStatus() == RegistrationStatus.CANCELADO) {
            throw new BusinessException("Inscricao " + id + " ja esta cancelada.");
        }
        if (registration.getStatus() == RegistrationStatus.CONFIRMADO) {
            throw new BusinessException("Inscricao " + id + " ja foi confirmada e nao pode ser cancelada.");
        }

        registration.setStatus(RegistrationStatus.CANCELADO);
        Registration saved = registrationRepository.save(registration);

        meterRegistry.counter("registrations.cancelled.total", "service", "registration-service").increment();
        log.info("Inscricao {} cancelada.", id);
        return toResponse(saved);
    }

    private void validateNoDuplicate(Long playerId, Long tournamentId) {
        if (registrationRepository.existsByPlayerIdAndTournamentIdAndStatusNot(playerId, tournamentId, RegistrationStatus.CANCELADO)) {
            throw new BusinessException("Jogador " + playerId + " ja possui inscricao ativa no torneio " + tournamentId + ".");
        }
    }

    private void validateTournament(Long tournamentId) {
        try {
            TournamentResponseDTO tournament = tournamentClient.getById(tournamentId);
            if (!"REGISTRO_ABERTO".equals(tournament.status())) {
                throw new BusinessException("Torneio " + tournamentId + " nao esta com inscricoes abertas. Status atual: " + tournament.status());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Torneio nao encontrado: " + tournamentId);
        } catch (Exception e) {
            log.warn("Nao foi possivel validar o torneio {}. Prosseguindo com a inscricao. Motivo: {}", tournamentId, e.getMessage());
        }
    }

    private void validatePlayer(Long playerId) {
        try {
            playerClient.getById(playerId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Jogador nao encontrado: " + playerId);
        } catch (Exception e) {
            log.warn("Nao foi possivel validar o jogador {}. Prosseguindo com a inscricao. Motivo: {}", playerId, e.getMessage());
        }
    }

    private RegistrationResponse toResponse(Registration r) {
        return new RegistrationResponse(
                r.getId(),
                r.getPlayerId(),
                r.getTournamentId(),
                r.getMetodoPagamento(),
                r.getValor(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
