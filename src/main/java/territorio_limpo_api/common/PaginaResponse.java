package territorio_limpo_api.common;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <T> PaginaResponse<T> from(Page<T> page) {
		return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}
