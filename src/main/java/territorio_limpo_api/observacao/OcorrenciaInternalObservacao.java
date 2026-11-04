package territorio_limpo_api.observacao;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import territorio_limpo_api.ocorrencia.Ocorrencia;

@Entity
@Table(name = "ocorrencia_internal_observacao")
public class OcorrenciaInternalObservacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ocorrencia_id", nullable = false)
	private Ocorrencia ocorrencia;

	@Column(name = "author_id", nullable = false, length = 255)
	private String autorId;

	@Column(name = "body", nullable = false, length = 2000)
	private String corpo;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant criadoEm;

	protected OcorrenciaInternalObservacao() {
	}

	public static OcorrenciaInternalObservacao create(Ocorrencia ocorrencia, String autorId, String corpo, Instant now) {
		OcorrenciaInternalObservacao observacao = new OcorrenciaInternalObservacao();
		observacao.ocorrencia = ocorrencia;
		observacao.autorId = autorId;
		observacao.corpo = corpo;
		observacao.criadoEm = now;
		return observacao;
	}

	public Long getId() {
		return id;
	}

	public String getAutorId() {
		return autorId;
	}

	public String getCorpo() {
		return corpo;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
