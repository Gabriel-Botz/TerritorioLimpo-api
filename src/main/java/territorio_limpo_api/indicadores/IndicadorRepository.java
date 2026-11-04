package territorio_limpo_api.indicadores;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IndicadorRepository {

	private static final String SQL = """
			SELECT total_ocorrencias, received, in_analysis, pending_analysis, forwarded_or_awaiting,
			       forwarded, awaiting_service, resolved, unresolved, high_prioridade_pending,
			       total_encaminhamentos, encaminhamentos_with_protocol, ocorrencias_with_encaminhamento
			FROM v_indicadores
			""";

	private final JdbcTemplate jdbcTemplate;

	public IndicadorRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public IndicadoresResponse load() {
		return jdbcTemplate.queryForObject(SQL, (resultSet, rowNumber) -> new IndicadoresResponse(
				resultSet.getLong("total_ocorrencias"),
				resultSet.getLong("received"),
				resultSet.getLong("in_analysis"),
				resultSet.getLong("pending_analysis"),
				resultSet.getLong("forwarded_or_awaiting"),
				resultSet.getLong("forwarded"),
				resultSet.getLong("awaiting_service"),
				resultSet.getLong("resolved"),
				resultSet.getLong("unresolved"),
				resultSet.getLong("high_prioridade_pending"),
				resultSet.getLong("total_encaminhamentos"),
				resultSet.getLong("encaminhamentos_with_protocol"),
				resultSet.getLong("ocorrencias_with_encaminhamento")));
	}
}
