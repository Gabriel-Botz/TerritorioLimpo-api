package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import territorio_limpo_api.historico.OcorrenciaStatusHistoricoRepository;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.foto.OcorrenciaFotoRepository;
import territorio_limpo_api.storage.ClienteStorage;
import territorio_limpo_api.storage.ExcecaoStorage;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PublicOcorrenciaIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OcorrenciaRepository ocorrenciaRepository;

	@Autowired
	private OcorrenciaFotoRepository fotoRepository;

	@Autowired
	private OcorrenciaStatusHistoricoRepository historicoRepository;

	@MockitoBean
	private ClienteStorage storageClient;

	private MockMultipartFile dataPart(String json) {
		return new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
				json.getBytes(StandardCharsets.UTF_8));
	}

	private MockMultipartFile fotoPart(String name, byte[] content) {
		return new MockMultipartFile("fotos", name, "image/jpeg", content);
	}

	private byte[] jpeg() throws Exception {
		BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", output);
		return output.toByteArray();
	}

	private String addressOnlyJson() {
		return """
				{
				  "type": "LIXO_COMUM",
				  "estimatedVolume": "PEQUENO",
				  "collectionAcesso": "NORMAL",
				  "drenagemRisk": "NAO",
				  "addressText": "Rua Exemplo, 100",
				  "referenciaPoint": "perto da escola"
				}
				""";
	}

	@Test
	@DisplayName("registra ocorrência apenas com endereço e devolve protocolo sem UUID")
	void createsWithAddressOnly() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias").file(dataPart(addressOnlyJson())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.number").isNumber())
				.andExpect(jsonPath("$.status").value("RECEBIDA"))
				.andExpect(jsonPath("$.fotos.received").value(0))
				.andExpect(jsonPath("$.fotos.saved").value(0))
				.andExpect(jsonPath("$.localizacaoSummary").value("Rua Exemplo, 100 (perto da escola)"))
				.andExpect(jsonPath("$.createdAt").isString())
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.message").value(
						"Seu registro foi recebido e será analisado conforme a disponibilidade da equipe."));

		Ocorrencia ocorrencia = ocorrenciaRepository.findAll().getFirst();
		assertThat(ocorrencia.getOrigemLocalizacao()).isEqualTo(OrigemLocalizacao.MANUAL);
		assertThat(ocorrencia.getSituacao().name()).isEqualTo("RECEBIDA");
		assertThat(ocorrencia.getIdRequisicaoCliente()).isNull();
		assertThat(historicoRepository.findByOcorrenciaIdOrderByAlteradoEmAsc(ocorrencia.getId())).hasSize(1);
	}

	@Test
	@DisplayName("registra ocorrência com GPS e define origem pelo servidor")
	void createsWithGps() throws Exception {
		String json = """
				{
				  "type": "ENTULHO",
				  "estimatedVolume": "MEDIO",
				  "collectionAcesso": "DIFICIL",
				  "drenagemRisk": "NAO_SEI",
				  "latitude": -22.505000,
				  "longitude": -43.178000,
				  "precisaoMeters": 12.5
				}
				""";

		mockMvc.perform(multipart("/api/v1/public/ocorrencias").file(dataPart(json)))
				.andExpect(status().isCreated());

		Ocorrencia ocorrencia = ocorrenciaRepository.findAll().getFirst();
		assertThat(ocorrencia.getOrigemLocalizacao()).isEqualTo(OrigemLocalizacao.GPS);
		assertThat(ocorrencia.getLatitude()).isEqualByComparingTo("-22.505000");
		assertThat(ocorrencia.getLongitude()).isEqualByComparingTo("-43.178000");
		assertThat(ocorrencia.getPrecisaoMetros()).isEqualByComparingTo("12.50");
	}

	@Test
	@DisplayName("rejeita registro sem nenhuma localização")
	void rejectsWithoutLocalizacao() throws Exception {
		String json = """
				{
				  "type": "LIXO_COMUM",
				  "estimatedVolume": "PEQUENO",
				  "collectionAcesso": "NORMAL",
				  "drenagemRisk": "NAO"
				}
				""";

		mockMvc.perform(multipart("/api/v1/public/ocorrencias").file(dataPart(json)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[0].message").value(
						"Informe o endereço, um ponto de referência ou use sua localização."));

		assertThat(ocorrenciaRepository.count()).isZero();
	}

	@Test
	@DisplayName("rejeita apenas uma das coordenadas")
	void rejectsIncompleteCoordinates() throws Exception {
		String json = """
				{
				  "type": "LIXO_COMUM",
				  "estimatedVolume": "PEQUENO",
				  "collectionAcesso": "NORMAL",
				  "drenagemRisk": "NAO",
				  "latitude": -22.5
				}
				""";

		mockMvc.perform(multipart("/api/v1/public/ocorrencias").file(dataPart(json)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	@DisplayName("mesma Idempotency-Key não duplica e responde 200 no reenvio")
	void idempotentReplay() throws Exception {
		UUID key = UUID.randomUUID();

		String first = mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.header("Idempotency-Key", key.toString()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.header("Idempotency-Key", key.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.number").value(
						com.jayway.jsonpath.JsonPath.read(first, "$.number")));

		assertThat(ocorrenciaRepository.count()).isOne();
	}

	@Test
	@DisplayName("salva ocorrência e foto quando o armazenamento funciona")
	void savesFoto() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.file(fotoPart("foto.jpg", jpeg())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fotos.received").value(1))
				.andExpect(jsonPath("$.fotos.saved").value(1))
				.andExpect(jsonPath("$.fotos.failed").value(0));

		assertThat(fotoRepository.count()).isOne();
		verify(storageClient).upload(anyString(), any(byte[].class));
	}

	@Test
	@DisplayName("falha do armazenamento não impede o registro")
	void storageFailureStillCreates() throws Exception {
		doThrow(new ExcecaoStorage("indisponível"))
				.when(storageClient).upload(anyString(), any(byte[].class));

		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.file(fotoPart("foto.jpg", jpeg())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fotos.saved").value(0))
				.andExpect(jsonPath("$.fotos.failed").value(1))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Não foi possível salvar 1 foto")));

		assertThat(ocorrenciaRepository.count()).isOne();
		assertThat(fotoRepository.count()).isZero();
	}

	@Test
	@DisplayName("rejeita mais de três fotos")
	void rejectsTooManyFotos() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.file(fotoPart("1.jpg", jpeg()))
						.file(fotoPart("2.jpg", jpeg()))
						.file(fotoPart("3.jpg", jpeg()))
						.file(fotoPart("4.jpg", jpeg())))
				.andExpect(status().isBadRequest());

		assertThat(ocorrenciaRepository.count()).isZero();
	}

	@Test
	@DisplayName("rejeita foto inválida antes de gravar no banco")
	void rejectsInvalidFoto() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.file(fotoPart("falsa.jpg", "não é imagem".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("fotos"));

		assertThat(ocorrenciaRepository.count()).isZero();
	}

	@Test
	@DisplayName("rejeita Idempotency-Key que não é UUID")
	void rejectsInvalidIdempotencyKey() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart(addressOnlyJson()))
						.header("Idempotency-Key", "123"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
	}
}
