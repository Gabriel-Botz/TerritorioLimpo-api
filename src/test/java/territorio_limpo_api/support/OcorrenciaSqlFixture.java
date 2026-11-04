package territorio_limpo_api.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Inserções SQL diretas usadas para exercitar as CHECKs do schema, sem passar
 * pelo mapeamento JPA.
 */
public final class OcorrenciaSqlFixture {

	private final JdbcTemplate jdbcTemplate;

	public OcorrenciaSqlFixture(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void insertValidManualLocalizacao() {
		insert(new OcorrenciaRow(UUID.randomUUID(), "MANUAL", null, null, "Rua Exemplo, 100",
				null, "RECEBIDA", null, null, null));
	}

	public int insert(OcorrenciaRow row) {
		String sql = """
				INSERT INTO ocorrencia (
					id, created_at, updated_at, type, estimated_volume, collection_acesso, drenagem_risk,
					latitude, longitude, address_text, referencia_point, localizacao_source, status, prioridade,
					resolved_at, resolved_confirmed_by, resolution_observacao
				) VALUES (
					'%s', now(), now(), 'LIXO_COMUM', 'PEQUENO', 'NORMAL', 'NAO',
					%s, %s, %s, %s, '%s', '%s', 'BAIXA',
					%s, %s, %s
				)
				""".formatted(
				row.id(),
				number(row.latitude()),
				number(row.longitude()),
				literal(row.addressText()),
				literal(row.referenciaPoint()),
				row.localizacaoSource(),
				row.status(),
				timestamp(row.resolvedAt()),
				literal(row.resolvedConfirmedBy()),
				literal(row.resolutionObservacao()));
		return jdbcTemplate.update(sql);
	}

	private static String literal(String value) {
		return value == null ? "NULL" : "'" + value.replace("'", "''") + "'";
	}

	private static String number(BigDecimal value) {
		return value == null ? "NULL" : value.toPlainString();
	}

	private static String timestamp(Instant value) {
		return value == null ? "NULL" : "'" + value + "'";
	}

	public record OcorrenciaRow(
			UUID id,
			String localizacaoSource,
			BigDecimal latitude,
			BigDecimal longitude,
			String addressText,
			String referenciaPoint,
			String status,
			Instant resolvedAt,
			String resolvedConfirmedBy,
			String resolutionObservacao) {
	}
}
