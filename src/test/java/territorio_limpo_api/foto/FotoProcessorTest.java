package territorio_limpo_api.foto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FotoProcessorTest {

	private final FotoProcessor processor = new FotoProcessor();

	private byte[] jpeg(int width, int height) throws Exception {
		return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
	}

	private byte[] png(int width, int height) throws Exception {
		return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png");
	}

	private byte[] encode(BufferedImage image, String format) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);
		return output.toByteArray();
	}

	private MockMultipartFile file(String name, String contentType, byte[] content) {
		return new MockMultipartFile("fotos", name, contentType, content);
	}

	@Test
	@DisplayName("re-encoda JPEG e limita o maior lado a 1600 px")
	void reencodesAndResizesJpeg() throws Exception {
		FotoProcessada result = processor.process(file("foto.jpg", "image/jpeg", jpeg(2000, 1000)));

		assertThat(result.contentType()).isEqualTo("image/jpeg");
		assertThat(result.content()).startsWith((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.content()));
		assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isLessThanOrEqualTo(1600);
	}

	@Test
	@DisplayName("aceita PNG e devolve JPEG")
	void acceptsPng() throws Exception {
		FotoProcessada result = processor.process(file("foto.png", "image/png", png(200, 100)));

		assertThat(result.contentType()).isEqualTo("image/jpeg");
		assertThat(result.content()).startsWith((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
	}

	@Test
	@DisplayName("remove metadados EXIF no re-encode")
	void stripsExif() throws Exception {
		byte[] original = jpeg(300, 200);
		byte[] withExif = injectExif(original);

		FotoProcessada result = processor.process(file("foto.jpg", "image/jpeg", withExif));

		assertThat(new String(result.content(), StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
		assertThat(new String(result.content(), StandardCharsets.ISO_8859_1)).doesNotContain("GPS");
	}

	@Test
	@DisplayName("rejeita arquivo com extensão de imagem mas conteúdo inválido")
	void rejectsFakeImage() {
		MockMultipartFile fake = file("foto.jpg", "image/jpeg",
				"isto nao e uma imagem".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> processor.process(fake)).isInstanceOf(ExcecaoFotoInvalida.class);
	}

	@Test
	@DisplayName("rejeita foto acima de 5 MB")
	void rejectsOversizedFoto() {
		byte[] oversized = new byte[(int) FotoProcessor.MAX_FILE_SIZE_BYTES + 1];
		oversized[0] = (byte) 0xFF;
		oversized[1] = (byte) 0xD8;
		oversized[2] = (byte) 0xFF;

		assertThatThrownBy(() -> processor.process(file("foto.jpg", "image/jpeg", oversized)))
				.isInstanceOf(ExcecaoFotoInvalida.class)
				.hasMessageContaining("5 MB");
	}

	@Test
	@DisplayName("rejeita foto vazia")
	void rejectsEmptyFoto() {
		assertThatThrownBy(() -> processor.process(file("foto.jpg", "image/jpeg", new byte[0])))
				.isInstanceOf(ExcecaoFotoInvalida.class);
	}

	private byte[] injectExif(byte[] jpeg) {
		byte[] exifPayload = "Exif\0\0GPS latitude -22.5".getBytes(StandardCharsets.ISO_8859_1);
		int segmentLength = exifPayload.length + 2;
		byte[] segment = new byte[4 + exifPayload.length];
		segment[0] = (byte) 0xFF;
		segment[1] = (byte) 0xE1;
		segment[2] = (byte) ((segmentLength >> 8) & 0xFF);
		segment[3] = (byte) (segmentLength & 0xFF);
		System.arraycopy(exifPayload, 0, segment, 4, exifPayload.length);

		byte[] result = new byte[jpeg.length + segment.length];
		System.arraycopy(jpeg, 0, result, 0, 2);
		System.arraycopy(segment, 0, result, 2, segment.length);
		System.arraycopy(jpeg, 2, result, 2 + segment.length, jpeg.length - 2);
		return Arrays.copyOf(result, result.length);
	}
}
