package territorio_limpo_api.foto;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import territorio_limpo_api.ocorrencia.Ocorrencia;

@Entity
@Table(name = "ocorrencia_foto")
public class OcorrenciaFoto {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ocorrencia_id", nullable = false)
	private Ocorrencia ocorrencia;

	@Column(name = "storage_path", nullable = false, unique = true, length = 500)
	private String caminhoStorage;

	@Column(name = "content_type", nullable = false, length = 100)
	private String tipoConteudo;

	@Column(name = "size_bytes", nullable = false)
	private long tamanhoBytes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant criadoEm;

	protected OcorrenciaFoto() {
	}

	public static OcorrenciaFoto create(UUID id, Ocorrencia ocorrencia, String caminhoStorage,
			String tipoConteudo, long tamanhoBytes, Instant now) {
		OcorrenciaFoto foto = new OcorrenciaFoto();
		foto.id = id;
		foto.ocorrencia = ocorrencia;
		foto.caminhoStorage = caminhoStorage;
		foto.tipoConteudo = tipoConteudo;
		foto.tamanhoBytes = tamanhoBytes;
		foto.criadoEm = now;
		return foto;
	}

	public UUID getId() {
		return id;
	}

	public Ocorrencia getOcorrencia() {
		return ocorrencia;
	}

	public String getCaminhoStorage() {
		return caminhoStorage;
	}

	public String getTipoConteudo() {
		return tipoConteudo;
	}

	public long getTamanhoBytes() {
		return tamanhoBytes;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
