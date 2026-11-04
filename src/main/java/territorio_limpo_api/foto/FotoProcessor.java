package territorio_limpo_api.foto;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * Valida e re-encoda fotos no servidor. O re-encode remove EXIF e metadados
 * (inclusive GPS do aparelho) e limita o tamanho da imagem armazenada.
 */
@Component
public class FotoProcessor {

	public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
	public static final int MAX_PHOTOS = 3;
	public static final int MAX_SIDE_PIXELS = 1600;
	public static final float JPEG_QUALITY = 0.82f;

	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	public FotoProcessada process(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ExcecaoFotoInvalida("A foto enviada está vazia. Envie outra imagem.");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new ExcecaoFotoInvalida("Cada foto deve ter no máximo 5 MB. Envie uma imagem menor.");
		}

		byte[] original = readBytes(file);
		if (!isJpeg(original) && !isPng(original)) {
			throw new ExcecaoFotoInvalida("Envie apenas fotos em JPEG ou PNG.");
		}

		BufferedImage image = decode(original);
		image = applyOrientation(image, readOrientation(original));
		image = resize(image);
		byte[] jpeg = encodeJpeg(image);

		return new FotoProcessada(jpeg, "image/jpeg");
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException exception) {
			throw new ExcecaoFotoInvalida("Não foi possível ler a foto enviada. Tente novamente.", exception);
		}
	}

	private boolean isJpeg(byte[] content) {
		return content.length >= JPEG_MAGIC.length
				&& content[0] == JPEG_MAGIC[0] && content[1] == JPEG_MAGIC[1] && content[2] == JPEG_MAGIC[2];
	}

	private boolean isPng(byte[] content) {
		if (content.length < PNG_MAGIC.length) {
			return false;
		}
		for (int index = 0; index < PNG_MAGIC.length; index++) {
			if (content[index] != PNG_MAGIC[index]) {
				return false;
			}
		}
		return true;
	}

	private BufferedImage decode(byte[] content) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
			if (image == null) {
				throw new ExcecaoFotoInvalida("Não foi possível abrir a foto enviada. Envie outra imagem.");
			}
			return image;
		}
		catch (IOException exception) {
			throw new ExcecaoFotoInvalida("Não foi possível abrir a foto enviada. Envie outra imagem.", exception);
		}
	}

	private int readOrientation(byte[] content) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(content));
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
				return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
		}
		catch (Exception ignored) {
			// Sem metadados legíveis: mantém a orientação original.
		}
		return 1;
	}

	private BufferedImage applyOrientation(BufferedImage image, int orientation) {
		return switch (orientation) {
			case 2 -> transform(image, -1, 0, 0, 1, image.getWidth(), 0);
			case 3 -> transform(image, -1, 0, 0, -1, image.getWidth(), image.getHeight());
			case 4 -> transform(image, 1, 0, 0, -1, 0, image.getHeight());
			case 5 -> transform(image, 0, 1, 1, 0, 0, 0);
			case 6 -> transform(image, 0, 1, -1, 0, image.getHeight(), 0);
			case 7 -> transform(image, 0, -1, -1, 0, image.getHeight(), image.getWidth());
			case 8 -> transform(image, 0, -1, 1, 0, 0, image.getWidth());
			default -> image;
		};
	}

	private BufferedImage transform(BufferedImage image, int m00, int m01, int m10, int m11,
			int translateX, int translateY) {
		int width = Math.abs(m00) * image.getWidth() + Math.abs(m01) * image.getHeight();
		int height = Math.abs(m10) * image.getWidth() + Math.abs(m11) * image.getHeight();
		BufferedImage transformed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = transformed.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.drawImage(image, new AffineTransform(m00, m10, m01, m11, translateX, translateY), null);
		}
		finally {
			graphics.dispose();
		}
		return transformed;
	}

	private BufferedImage resize(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width <= MAX_SIDE_PIXELS && height <= MAX_SIDE_PIXELS) {
			return toRgb(image);
		}

		double scale = Math.min((double) MAX_SIDE_PIXELS / width, (double) MAX_SIDE_PIXELS / height);
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));

		BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = resized.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, targetWidth, targetHeight);
			graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
		}
		finally {
			graphics.dispose();
		}
		return resized;
	}

	private BufferedImage toRgb(BufferedImage image) {
		if (image.getType() == BufferedImage.TYPE_INT_RGB) {
			return image;
		}
		BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgb.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.drawImage(image, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return rgb;
	}

	private byte[] encodeJpeg(BufferedImage image) {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new ExcecaoFotoInvalida("Não foi possível processar a foto enviada. Tente novamente.");
		}
		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parameters.setCompressionQuality(JPEG_QUALITY);

			writer.setOutput(imageOutput);
			writer.write(null, new IIOImage(image, null, null), parameters);
			imageOutput.flush();
			return output.toByteArray();
		}
		catch (IOException exception) {
			throw new ExcecaoFotoInvalida("Não foi possível processar a foto enviada. Tente novamente.", exception);
		}
		finally {
			writer.dispose();
		}
	}
}
