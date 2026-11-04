package territorio_limpo_api.ocorrencia;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import territorio_limpo_api.common.NormalizadorTexto;

public final class OcorrenciaSpecifications {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private OcorrenciaSpecifications() {
	}

	public static Specification<Ocorrencia> from(AdminOcorrenciaFilter filter) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (filter.status() != null) {
				predicates.add(builder.equal(root.get("situacao"), filter.status()));
			}
			if (filter.type() != null) {
				predicates.add(builder.equal(root.get("tipo"), filter.type()));
			}
			if (filter.prioridade() != null) {
				predicates.add(builder.equal(root.get("prioridade"), filter.prioridade()));
			}
			if (filter.hasCoordinates() != null) {
				if (filter.hasCoordinates()) {
					predicates.add(builder.isNotNull(root.get("latitude")));
				}
				else {
					predicates.add(builder.isNull(root.get("latitude")));
				}
			}
			if (filter.from() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("criadoEm"),
						filter.from().atStartOfDay(SAO_PAULO).toInstant()));
			}
			if (filter.to() != null) {
				predicates.add(builder.lessThan(root.get("criadoEm"),
						filter.to().plusDays(1).atStartOfDay(SAO_PAULO).toInstant()));
			}
			if (NormalizadorTexto.hasText(filter.query())) {
				String pattern = "%" + escapeLike(filter.query().strip().toLowerCase()) + "%";
				Predicate address = builder.like(builder.lower(root.get("textoEndereco")), pattern, '\\');
				Predicate referencia = builder.like(builder.lower(root.get("pontoReferencia")), pattern, '\\');
				Predicate observacaos = builder.like(builder.lower(root.get("observacoes")), pattern, '\\');
				predicates.add(builder.or(address, referencia, observacaos));
			}

			return builder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
