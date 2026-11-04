package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;
import territorio_limpo_api.prioridade.PrioridadeCalculator;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OcorrenciaRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private OcorrenciaRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("persiste ocorrência, gera number e lê motivos de prioridade do array")
	void persistsAndReloads() {
		UUID id = UUID.randomUUID();
		Ocorrencia ocorrencia = Ocorrencia.create(id, Instant.now());
		ocorrencia.setTipo(OcorrenciaType.DRENAGEM_OBSTRUIDA);
		ocorrencia.setVolumeEstimado(VolumeEstimado.GRANDE);
		ocorrencia.setAcessoColeta(AcessoColeta.IMPOSSIVEL_CAMINHAO);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.SIM);
		ocorrencia.setOrigemLocalizacao(territorio_limpo_api.ocorrencia.OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		ocorrencia.applyPrioridade(PrioridadeCalculator.calculate(OcorrenciaType.DRENAGEM_OBSTRUIDA,
				VolumeEstimado.GRANDE, AcessoColeta.IMPOSSIVEL_CAMINHAO, RiscoDrenagem.SIM, false));

		repository.saveAndFlush(ocorrencia);
		entityManager.clear();

		Ocorrencia reloaded = repository.findById(id).orElseThrow();

		assertThat(reloaded.getNumero()).isNotNull().isPositive();
		assertThat(reloaded.getStatus()).isEqualTo(OcorrenciaStatus.RECEBIDA);
		assertThat(reloaded.getPrioridade()).isEqualTo(NivelPrioridade.ALTA);
		assertThat(reloaded.getPrioridadeReasons())
				.containsExactly("DRENAGEM_COM_RISCO", "POSSIVEL_OBSTRUCAO", "VOLUME_GRANDE",
						"ACESSO_IMPOSSIVEL_CAMINHAO");
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("busca ocorrência pela Idempotency-Key gravada")
	void findsByClientRequestId() {
		UUID requestId = UUID.randomUUID();
		Ocorrencia ocorrencia = Ocorrencia.create(UUID.randomUUID(), Instant.now());
		ocorrencia.setTipo(OcorrenciaType.LIXO_COMUM);
		ocorrencia.setVolumeEstimado(VolumeEstimado.PEQUENO);
		ocorrencia.setAcessoColeta(AcessoColeta.NORMAL);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.NAO);
		ocorrencia.setOrigemLocalizacao(territorio_limpo_api.ocorrencia.OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		ocorrencia.setClientRequestId(requestId);

		repository.saveAndFlush(ocorrencia);

		assertThat(repository.findByClientRequestId(requestId)).isPresent()
				.get()
				.extracting(Ocorrencia::getClientRequestId)
				.isEqualTo(requestId);
	}
}
