package territorio_limpo_api.historico;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaStatusHistoricoRepository extends JpaRepository<OcorrenciaStatusHistorico, Long> {

	List<OcorrenciaStatusHistorico> findByOcorrenciaIdOrderByAlteradoEmAsc(UUID ocorrenciaId);
}
