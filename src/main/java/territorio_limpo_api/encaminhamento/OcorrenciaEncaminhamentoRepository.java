package territorio_limpo_api.encaminhamento;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OcorrenciaEncaminhamentoRepository extends JpaRepository<OcorrenciaEncaminhamento, Long> {

	List<OcorrenciaEncaminhamento> findByOcorrenciaIdOrderByEncaminhadoEmDesc(UUID ocorrenciaId);

	@Query("""
			select encaminhamento.ocorrencia.id, count(encaminhamento)
			from OcorrenciaEncaminhamento encaminhamento
			group by encaminhamento.ocorrencia.id
			""")
	List<Object[]> countGroupedByOcorrencia();

	@Query("""
			select encaminhamento.ocorrencia.id, encaminhamento.numeroProtocolo
			from OcorrenciaEncaminhamento encaminhamento
			where encaminhamento.numeroProtocolo is not null
			order by encaminhamento.encaminhadoEm asc
			""")
	List<Object[]> protocolHistoricoOrderedByEncaminhadoEm();
}
