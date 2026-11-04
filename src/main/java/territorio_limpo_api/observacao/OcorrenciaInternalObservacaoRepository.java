package territorio_limpo_api.observacao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaInternalObservacaoRepository extends JpaRepository<OcorrenciaInternalObservacao, Long> {

	List<OcorrenciaInternalObservacao> findByOcorrenciaIdOrderByCreatedAtAsc(UUID ocorrenciaId);
}
