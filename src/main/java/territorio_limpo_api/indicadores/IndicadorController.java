package territorio_limpo_api.indicadores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/indicadores")
public class IndicadorController {

	private final IndicadorRepository repository;

	public IndicadorController(IndicadorRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	public IndicadoresResponse indicadores() {
		return repository.load();
	}
}
