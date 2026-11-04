package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import territorio_limpo_api.support.OcorrenciaSqlFixture;
import territorio_limpo_api.support.OcorrenciaSqlFixture.OcorrenciaRow;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OcorrenciaSchemaTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private OcorrenciaSqlFixture fixture() {
		return new OcorrenciaSqlFixture(jdbcTemplate);
	}

	@Test
	@DisplayName("aceita localização manual só com endereço")
	void acceptsManualLocalizacao() {
		fixture().insertValidManualLocalizacao();

		assertThat(jdbcTemplate.queryForObject("select count(*) from ocorrencia", Long.class)).isOne();
	}

	@Test
	@DisplayName("rejeita apenas uma das coordenadas")
	void rejectsIncompleteCoordinates() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "GPS", new BigDecimal("-22.500000"),
				null, null, null, "RECEBIDA", null, null, null);

		assertThatThrownBy(() -> fixture().insert(row)).isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("rejeita origem GPS sem coordenadas")
	void rejectsGpsWithoutCoordinates() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "GPS", null, null,
				"Rua Exemplo, 100", null, "RECEBIDA", null, null, null);

		assertThatThrownBy(() -> fixture().insert(row)).isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("rejeita registro sem nenhuma localização")
	void rejectsWithoutLocalizacao() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "MANUAL", null, null,
				null, null, "RECEBIDA", null, null, null);

		assertThatThrownBy(() -> fixture().insert(row)).isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("rejeita coordenadas fora da faixa")
	void rejectsCoordinatesOutOfRange() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "GPS", new BigDecimal("-100.000000"),
				new BigDecimal("-22.500000"), null, null, "RECEBIDA", null, null, null);

		assertThatThrownBy(() -> fixture().insert(row)).isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("rejeita resolução sem confirmação registrada")
	void rejectsResolvedWithoutConfirmation() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "MANUAL", null, null,
				"Rua Exemplo, 100", null, "RESOLVIDA", Instant.now(), null, null);

		assertThatThrownBy(() -> fixture().insert(row)).isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("aceita resolução com confirmação registrada")
	void acceptsResolvedWithConfirmation() {
		OcorrenciaRow row = new OcorrenciaRow(UUID.randomUUID(), "MANUAL", null, null,
				"Rua Exemplo, 100", null, "RESOLVIDA", Instant.now(), "admin-sub", "Coleta confirmada.");

		fixture().insert(row);

		assertThat(jdbcTemplate.queryForObject(
				"select resolved_confirmed_by from ocorrencia where status = 'RESOLVIDA'", String.class))
				.isEqualTo("admin-sub");
	}

	@Test
	@DisplayName("rejeita a mesma Idempotency-Key em dois registros")
	void rejectsDuplicatedClientRequestId() {
		UUID requestId = UUID.randomUUID();

		jdbcTemplate.update("""
				INSERT INTO ocorrencia (id, created_at, updated_at, type, estimated_volume, collection_acesso,
					drenagem_risk, address_text, localizacao_source, status, prioridade, client_request_id)
				VALUES (?, now(), now(), 'LIXO_COMUM', 'PEQUENO', 'NORMAL', 'NAO', 'Rua A', 'MANUAL',
					'RECEBIDA', 'BAIXA', ?)
				""", UUID.randomUUID(), requestId);

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO ocorrencia (id, created_at, updated_at, type, estimated_volume, collection_acesso,
					drenagem_risk, address_text, localizacao_source, status, prioridade, client_request_id)
				VALUES (?, now(), now(), 'LIXO_COMUM', 'PEQUENO', 'NORMAL', 'NAO', 'Rua B', 'MANUAL',
					'RECEBIDA', 'BAIXA', ?)
				""", UUID.randomUUID(), requestId))
				.isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("histórico permite primeira linha sem status anterior e com autor nulo")
	void acceptsInitialHistorico() {
		fixture().insertValidManualLocalizacao();
		UUID ocorrenciaId = jdbcTemplate.queryForObject("select id from ocorrencia", UUID.class);

		jdbcTemplate.update("""
				INSERT INTO ocorrencia_status_historico (ocorrencia_id, from_status, to_status, changed_by, observacao, changed_at)
				VALUES (?, NULL, 'RECEBIDA', NULL, NULL, now())
				""", ocorrenciaId);

		assertThat(jdbcTemplate.queryForObject(
				"select from_status from ocorrencia_status_historico", String.class)).isNull();
	}
}
