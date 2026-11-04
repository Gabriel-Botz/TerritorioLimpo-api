package territorio_limpo_api.encaminhamento;

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
@Table(name = "ocorrencia_encaminhamento")
public class OcorrenciaEncaminhamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ocorrencia_id", nullable = false)
	private Ocorrencia ocorrencia;

	@Column(name = "destination", nullable = false, length = 200)
	private String destino;

	@Column(name = "channel", nullable = false, length = 100)
	private String canal;

	@Column(name = "referred_at", nullable = false)
	private Instant encaminhadoEm;

	@Column(name = "protocol_number", length = 100)
	private String numeroProtocolo;

	@Column(name = "request_text", nullable = false, length = 8000)
	private String textoSolicitacao;

	@Column(name = "referred_by", nullable = false, length = 255)
	private String encaminhadoPor;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant criadoEm;

	protected OcorrenciaEncaminhamento() {
	}

	public static OcorrenciaEncaminhamento create(Ocorrencia ocorrencia, String destino, String canal,
			Instant encaminhadoEm, String numeroProtocolo, String textoSolicitacao, String encaminhadoPor, Instant now) {
		OcorrenciaEncaminhamento encaminhamento = new OcorrenciaEncaminhamento();
		encaminhamento.ocorrencia = ocorrencia;
		encaminhamento.destino = destino;
		encaminhamento.canal = canal;
		encaminhamento.encaminhadoEm = encaminhadoEm;
		encaminhamento.numeroProtocolo = numeroProtocolo;
		encaminhamento.textoSolicitacao = textoSolicitacao;
		encaminhamento.encaminhadoPor = encaminhadoPor;
		encaminhamento.criadoEm = now;
		return encaminhamento;
	}

	public Long getId() {
		return id;
	}

	public String getDestino() {
		return destino;
	}

	public String getCanal() {
		return canal;
	}

	public Instant getEncaminhadoEm() {
		return encaminhadoEm;
	}

	public String getNumeroProtocolo() {
		return numeroProtocolo;
	}

	public String getTextoSolicitacao() {
		return textoSolicitacao;
	}

	public String getEncaminhadoPor() {
		return encaminhadoPor;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
