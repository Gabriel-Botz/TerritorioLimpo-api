package territorio_limpo_api.ocorrencia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocalizacaoConfirmRequest(
		@NotNull(message = "{validation.required}")
		@DecimalMin(value = "-90.0", message = "{validation.latitude.range}")
		@DecimalMax(value = "90.0", message = "{validation.latitude.range}")
		BigDecimal latitude,

		@NotNull(message = "{validation.required}")
		@DecimalMin(value = "-180.0", message = "{validation.longitude.range}")
		@DecimalMax(value = "180.0", message = "{validation.longitude.range}")
		BigDecimal longitude,

		@DecimalMin(value = "0.0", message = "{validation.precisao.min}")
		BigDecimal precisaoMeters) {
}
