package territorio_limpo_api.ocorrencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID>, JpaSpecificationExecutor<Ocorrencia> {

	Optional<Ocorrencia> findByClientRequestId(UUID clientRequestId);

	Optional<Ocorrencia> findByNumero(Long number);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Ocorrencia o where o.id = :id")
	Optional<Ocorrencia> findByIdForUpdate(@Param("id") UUID id);
}
