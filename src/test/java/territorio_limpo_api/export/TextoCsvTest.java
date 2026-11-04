package territorio_limpo_api.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextoCsvTest {

	@Test
	@DisplayName("prefixa apóstrofo em células que começam com caractere de fórmula")
	void neutralizesFormulaCharacters() {
		assertThat(TextoCsv.neutralize("=SUM(1,1)")).isEqualTo("'=SUM(1,1)");
		assertThat(TextoCsv.neutralize("+55 24 99999-0000")).isEqualTo("'+55 24 99999-0000");
		assertThat(TextoCsv.neutralize("-22.5")).isEqualTo("'-22.5");
		assertThat(TextoCsv.neutralize("@usuario")).isEqualTo("'@usuario");
		assertThat(TextoCsv.neutralize("\tabc")).isEqualTo("'\tabc");
		assertThat(TextoCsv.neutralize("\rabc")).isEqualTo("'\rabc");
	}

	@Test
	@DisplayName("mantém texto comum e converte nulo em vazio")
	void keepsRegularText() {
		assertThat(TextoCsv.neutralize("Rua Exemplo, 100")).isEqualTo("Rua Exemplo, 100");
		assertThat(TextoCsv.neutralize("")).isEmpty();
		assertThat(TextoCsv.neutralize(null)).isEmpty();
	}
}
