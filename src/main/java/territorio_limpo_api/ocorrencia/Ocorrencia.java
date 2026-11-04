package territorio_limpo_api.ocorrencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import territorio_limpo_api.prioridade.PrioridadeReason;
import territorio_limpo_api.prioridade.PrioridadeResult;

@Entity
@Table(name = "ocorrencia")
public class Ocorrencia {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "number", insertable = false, updatable = false)
	private Long numero;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "updated_at", nullable = false)
	private Instant atualizadoEm;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private OcorrenciaType tipo;

	@Enumerated(EnumType.STRING)
	@Column(name = "estimated_volume", nullable = false, length = 20)
	private VolumeEstimado volumeEstimado;

	@Enumerated(EnumType.STRING)
	@Column(name = "collection_acesso", nullable = false, length = 30)
	private AcessoColeta acessoColeta;

	@Enumerated(EnumType.STRING)
	@Column(name = "drenagem_risk", nullable = false, length = 10)
	private RiscoDrenagem riscoDrenagem;

	@Column(name = "observacaos", length = 1000)
	private String observacoes;

	@Column(name = "latitude", precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "precisao_meters", precision = 8, scale = 2)
	private BigDecimal precisaoMetros;

	@Column(name = "address_text", length = 300)
	private String textoEndereco;

	@Column(name = "referencia_point", length = 300)
	private String pontoReferencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "localizacao_source", nullable = false, length = 30)
	private OrigemLocalizacao origemLocalizacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private OcorrenciaStatus situacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "prioridade", nullable = false, length = 10)
	private NivelPrioridade prioridade;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "prioridade_reasons", nullable = false, columnDefinition = "text[]")
	private List<String> motivosPrioridade = new ArrayList<>();

	@Column(name = "prioridade_set_manually", nullable = false)
	private boolean prioridadeDefinidaManualmente;

	@Column(name = "manual_prioridade_reason", length = 500)
	private String motivoPrioridadeManual;

	@Column(name = "in_vulneravel_area")
	private Boolean emAreaVulneravel;

	@Column(name = "client_request_id")
	private UUID idRequisicaoCliente;

	@Column(name = "resolved_at")
	private Instant resolvidaEm;

	@Column(name = "resolved_confirmed_by", length = 255)
	private String confirmadaPor;

	@Column(name = "resolution_observacao", length = 2000)
	private String observacaoResolucao;

	protected Ocorrencia() {
	}

	public static Ocorrencia create(UUID id, Instant now) {
		Ocorrencia ocorrencia = new Ocorrencia();
		ocorrencia.id = id;
		ocorrencia.criadoEm = now;
		ocorrencia.atualizadoEm = now;
		ocorrencia.situacao = OcorrenciaStatus.RECEBIDA;
		return ocorrencia;
	}

	public void applyPrioridade(PrioridadeResult result) {
		this.prioridade = result.level();
		this.motivosPrioridade = result.reasons().stream().map(PrioridadeReason::name).toList();
		this.prioridadeDefinidaManualmente = false;
	}

	public void applyManualPrioridade(NivelPrioridade level, String reason) {
		this.prioridade = level;
		this.prioridadeDefinidaManualmente = true;
		this.motivoPrioridadeManual = reason;
	}

	public void markResolved(Instant now, String confirmadaPor, String observacao) {
		this.situacao = OcorrenciaStatus.RESOLVIDA;
		this.resolvidaEm = now;
		this.confirmadaPor = confirmadaPor;
		this.observacaoResolucao = observacao;
	}

	public void clearResolution() {
		this.resolvidaEm = null;
		this.confirmadaPor = null;
		this.observacaoResolucao = null;
	}

	public void touch(Instant now) {
		this.atualizadoEm = now;
	}

	public UUID getId() {
		return id;
	}

	public Long getNumero() {
		return numero;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

	public OcorrenciaType getTipo() {
		return tipo;
	}

	public void setTipo(OcorrenciaType tipo) {
		this.tipo = tipo;
	}

	public VolumeEstimado getVolumeEstimado() {
		return volumeEstimado;
	}

	public void setVolumeEstimado(VolumeEstimado volumeEstimado) {
		this.volumeEstimado = volumeEstimado;
	}

	public AcessoColeta getAcessoColeta() {
		return acessoColeta;
	}

	public void setAcessoColeta(AcessoColeta acessoColeta) {
		this.acessoColeta = acessoColeta;
	}

	public RiscoDrenagem getRiscoDrenagem() {
		return riscoDrenagem;
	}

	public void setRiscoDrenagem(RiscoDrenagem riscoDrenagem) {
		this.riscoDrenagem = riscoDrenagem;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public BigDecimal getPrecisaoMetros() {
		return precisaoMetros;
	}

	public void setPrecisaoMetros(BigDecimal precisaoMetros) {
		this.precisaoMetros = precisaoMetros;
	}

	public String getTextoEndereco() {
		return textoEndereco;
	}

	public void setTextoEndereco(String textoEndereco) {
		this.textoEndereco = textoEndereco;
	}

	public String getPontoReferencia() {
		return pontoReferencia;
	}

	public void setPontoReferencia(String pontoReferencia) {
		this.pontoReferencia = pontoReferencia;
	}

	public OrigemLocalizacao getOrigemLocalizacao() {
		return origemLocalizacao;
	}

	public void setOrigemLocalizacao(OrigemLocalizacao origemLocalizacao) {
		this.origemLocalizacao = origemLocalizacao;
	}

	public OcorrenciaStatus getSituacao() {
		return situacao;
	}

	public void setSituacao(OcorrenciaStatus situacao) {
		this.situacao = situacao;
	}

	public NivelPrioridade getPrioridade() {
		return prioridade;
	}

	public List<String> getMotivosPrioridade() {
		return motivosPrioridade == null ? List.of() : List.copyOf(motivosPrioridade);
	}

	public boolean isPrioridadeDefinidaManualmente() {
		return prioridadeDefinidaManualmente;
	}

	public String getMotivoPrioridadeManual() {
		return motivoPrioridadeManual;
	}

	public Boolean getEmAreaVulneravel() {
		return emAreaVulneravel;
	}

	public void setEmAreaVulneravel(Boolean emAreaVulneravel) {
		this.emAreaVulneravel = emAreaVulneravel;
	}

	public UUID getIdRequisicaoCliente() {
		return idRequisicaoCliente;
	}

	public void setIdRequisicaoCliente(UUID idRequisicaoCliente) {
		this.idRequisicaoCliente = idRequisicaoCliente;
	}

	public Instant getResolvidaEm() {
		return resolvidaEm;
	}

	public String getConfirmadaPor() {
		return confirmadaPor;
	}

	public String getObservacaoResolucao() {
		return observacaoResolucao;
	}
}
