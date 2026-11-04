package territorio_limpo_api.historico;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;

@Entity
@Table(name = "ocorrencia_status_historico")
public class OcorrenciaStatusHistorico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ocorrencia_id", nullable = false)
	private Ocorrencia ocorrencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 30)
	private OcorrenciaStatus situacaoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 30)
	private OcorrenciaStatus situacaoNova;

	@Column(name = "changed_by", length = 255)
	private String alteradoPor;

	@Column(name = "observacao", length = 2000)
	private String observacao;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant alteradoEm;

	protected OcorrenciaStatusHistorico() {
	}

	public static OcorrenciaStatusHistorico create(Ocorrencia ocorrencia, OcorrenciaStatus situacaoAnterior,
			OcorrenciaStatus situacaoNova, String alteradoPor, String observacao, Instant now) {
		OcorrenciaStatusHistorico historico = new OcorrenciaStatusHistorico();
		historico.ocorrencia = ocorrencia;
		historico.situacaoAnterior = situacaoAnterior;
		historico.situacaoNova = situacaoNova;
		historico.alteradoPor = alteradoPor;
		historico.observacao = observacao;
		historico.alteradoEm = now;
		return historico;
	}

	public Long getId() {
		return id;
	}

	public OcorrenciaStatus getSituacaoAnterior() {
		return situacaoAnterior;
	}

	public OcorrenciaStatus getSituacaoNova() {
		return situacaoNova;
	}

	public String getAlteradoPor() {
		return alteradoPor;
	}

	public String getObservacao() {
		return observacao;
	}

	public Instant getAlteradoEm() {
		return alteradoEm;
	}
}
