package territorio_limpo_api.ocorrencia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OcorrenciaType;

public record PublicOcorrenciaRequest(
		@NotNull(message = "{validation.required}")
		OcorrenciaType type,

		@NotNull(message = "{validation.required}")
		VolumeEstimado estimatedVolume,

		@NotNull(message = "{validation.required}")
		AcessoColeta collectionAcesso,

		@NotNull(message = "{validation.required}")
		RiscoDrenagem drenagemRisk,

		@Size(max = 1000, message = "{validation.size}")
		String observacaos,

		@DecimalMin(value = "-90.0", message = "{validation.latitude.range}")
		@DecimalMax(value = "90.0", message = "{validation.latitude.range}")
		BigDecimal latitude,

		@DecimalMin(value = "-180.0", message = "{validation.longitude.range}")
		@DecimalMax(value = "180.0", message = "{validation.longitude.range}")
		BigDecimal longitude,

		@DecimalMin(value = "0.0", message = "{validation.precisao.min}")
		BigDecimal precisaoMeters,

		@Size(max = 300, message = "{validation.size}")
		String addressText,

		@Size(max = 300, message = "{validation.size}")
		String referenciaPoint) {

	@AssertTrue(message = "{validation.coordinates.pair}")
	public boolean isCoordinatesPairValid() {
		return (latitude == null) == (longitude == null);
	}

	@AssertTrue(message = "{validation.localizacao.required}")
	public boolean isLocalizacaoPresent() {
		return (latitude != null && longitude != null)
				|| NormalizadorTexto.hasText(addressText)
				|| NormalizadorTexto.hasText(referenciaPoint);
	}

	public boolean hasCoordinates() {
		return latitude != null && longitude != null;
	}
}
