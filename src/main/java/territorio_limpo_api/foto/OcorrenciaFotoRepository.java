package territorio_limpo_api.foto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcorrenciaFotoRepository extends JpaRepository<OcorrenciaFoto, UUID> {

	List<OcorrenciaFoto> findByOcorrenciaIdOrderByCriadoEmAsc(UUID ocorrenciaId);

	long countByOcorrenciaId(UUID ocorrenciaId);

	Optional<OcorrenciaFoto> findByIdAndOcorrenciaId(UUID id, UUID ocorrenciaId);

	@Query("""
			select foto.ocorrencia.id, count(foto)
			from OcorrenciaFoto foto
			where foto.ocorrencia.id in :ocorrenciaIds
			group by foto.ocorrencia.id
			""")
	List<Object[]> countByOcorrenciaIds(@Param("ocorrenciaIds") Collection<UUID> ocorrenciaIds);

	@Query("""
			select foto.ocorrencia.id, count(foto)
			from OcorrenciaFoto foto
			group by foto.ocorrencia.id
			""")
	List<Object[]> countGroupedByOcorrencia();
}
