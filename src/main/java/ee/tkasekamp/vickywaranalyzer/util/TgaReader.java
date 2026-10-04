package ee.tkasekamp.vickywaranalyzer.util;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** Reads Targa images. JavaFX is not able to read this format, but Victoria II
 * stores most of its flags as TGA, so they have to be decoded by hand. */
public class TgaReader {

	private TgaReader() {
		super();
	}

	/** Decodes a Targa file into a BufferedImage.
	 * Supports the colour mapped, true colour and greyscale image types, both
	 * uncompressed and run length encoded. */
	public static BufferedImage read(byte[] data) throws IOException {
		if (data == null || data.length < 18) {
			throw new IOException("Too short to be a TGA file");
		}
		int idLength = data[0] & 0xFF;
		int colorMapType = data[1] & 0xFF;
		int imageType = data[2] & 0xFF;
		int colorMapLength = readShort(data, 5);
		int colorMapDepth = data[7] & 0xFF;
		int width = readShort(data, 12);
		int height = readShort(data, 14);
		int pixelDepth = data[16] & 0xFF;
		int descriptor = data[17] & 0xFF;

		if (width <= 0 || height <= 0) {
			throw new IOException("TGA file without usable dimensions");
		}
		if (colorMapType != 0 && colorMapType != 1) {
			throw new IOException("Unknown TGA colour map type " + colorMapType);
		}

		int position = 18 + idLength;
		int[] palette = null;
		if (colorMapType == 1) {
			int entrySize = Math.max(1, colorMapDepth / 8);
			palette = new int[colorMapLength];
			for (int i = 0; i < colorMapLength; i++) {
				palette[i] = readPixel(data, position + i * entrySize, colorMapDepth);
			}
			position += colorMapLength * entrySize;
		}

		int pixels = width * height;
		int[] argb = new int[pixels];
		boolean colorMapped = imageType == 1 || imageType == 9;
		int bytesPerPixel = (pixelDepth + 7) / 8;
		int index = 0;

		if (imageType == 1 || imageType == 2 || imageType == 3) {
			for (index = 0; index < pixels; index++) {
				if (colorMapped) {
					argb[index] = palette[data[position++] & 0xFF];
				} else {
					argb[index] = readPixel(data, position, pixelDepth);
					position += bytesPerPixel;
				}
			}
		} else if (imageType == 9 || imageType == 10 || imageType == 11) {
			while (index < pixels && position < data.length) {
				int packet = data[position++] & 0xFF;
				int count = (packet & 0x7F) + 1;
				if ((packet & 0x80) != 0) {
					int value;
					if (colorMapped) {
						value = palette[data[position++] & 0xFF];
					} else {
						value = readPixel(data, position, pixelDepth);
						position += bytesPerPixel;
					}
					for (int i = 0; i < count && index < pixels; i++) {
						argb[index++] = value;
					}
				} else {
					for (int i = 0; i < count && index < pixels; i++) {
						if (colorMapped) {
							argb[index++] = palette[data[position++] & 0xFF];
						} else {
							argb[index] = readPixel(data, position, pixelDepth);
							position += bytesPerPixel;
							index++;
						}
					}
				}
			}
		} else {
			throw new IOException("Unknown TGA image type " + imageType);
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		boolean topLeftFirst = (descriptor & 0x20) != 0;
		for (int y = 0; y < height; y++) {
			int row = topLeftFirst ? y : height - 1 - y;
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, argb[row * width + x]);
			}
		}
		return image;
	}

	/** Reads one pixel at the given position and returns it as ARGB */
	private static int readPixel(byte[] data, int position, int depth) throws IOException {
		int needed = (depth <= 8) ? 1 : (depth + 7) / 8;
		if (position < 0 || position + needed > data.length) {
			throw new IOException("TGA file ends in the middle of a pixel");
		}
		int first = data[position] & 0xFF;
		int second = data[position + 1] & 0xFF;
		int third = data[position + 2] & 0xFF;

		if (depth <= 8) {
			return 0xFF000000 | (first << 16) | (first << 8) | first;
		}
		if (depth == 15 || depth == 16) {
			int value = first | (second << 8);
			return 0xFF000000 | (to8Bit((value >> 10) & 0x1F) << 16)
					| (to8Bit((value >> 5) & 0x1F) << 8) | to8Bit(value & 0x1F);
		}
		if (depth == 24) {
			return 0xFF000000 | (third << 16) | (second << 8) | first;
		}
		if (depth == 32) {
			return (data[position + 3] << 24) | (third << 16) | (second << 8) | first;
		}
		throw new IOException("Unknown TGA pixel depth " + depth);
	}

	/** Turns a 5 bit colour channel into an 8 bit one */
	private static int to8Bit(int value) {
		return (value << 3) | (value >> 2);
	}

	private static int readShort(byte[] data, int position) {
		return (data[position] & 0xFF) | ((data[position + 1] & 0xFF) << 8);
	}
}