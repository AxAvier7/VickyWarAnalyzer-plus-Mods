package ee.tkasekamp.vickywaranalyzer.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;

/** Searches the Victoria II mod folders for flags. The flags bundled in the jar
 * only cover the vanilla countries, so mods need their own gfx/flags folder to
 * be searched. Mods mostly use the TGA format, which is converted on the fly. */
public class ModFlags {
	private static final String[] EXTENSIONS = { ".tga", ".png", ".gif", ".jpg", ".jpeg", ".bmp" };
	/* Which government variant is used when a mod has no flag named after the tag alone */
	private static final String[] VARIANTS = { "default", "generic", "common", "monarchy", "republic",
			"democracy", "neutral", "fascist", "communist", "empire", "despotism", "theocracy" };

	private static List<Map<String, File>> indexes;
	private static final Map<String, Image> loaded = new HashMap<String, Image>();
	/* Country tags are short, upper case and may contain digits, as KH1 in some mods */
	private static final java.util.regex.Pattern TAG = java.util.regex.Pattern.compile("[A-Z0-9]{2,4}");

	private ModFlags() {
		super();
	}

	/** True when the given text can be a country tag */
	public static boolean isCountryTag(String tag) {
		return tag != null && TAG.matcher(tag).matches();
	}

	/** Returns the flag of the given tag, taken from the mods, the install
	 * directory or the flags inside the jar. Null when nothing was found. */
	public static Image getFlag(String tag) {
		if (!isCountryTag(tag)) {
			return null;
		}
		for (File file : findFlagFiles(tag)) {
			Image image = load(file);
			if (image != null) {
				return image;
			}
		}
		return loadBundled(tag);
	}

	/** The file the given tag is taken from, the first of the found ones */
	static File findFlagFile(String tag) {
		List<File> files = findFlagFiles(tag);
		return files.isEmpty() ? null : files.get(0);
	}

	/** Throws away everything that was searched and read before. Needed when
	 * another save game or another set of mod folders is used. */
	public static void reset() {
		indexes = null;
		loaded.clear();
		ModFinder.reset();
	}

	/** All files that could hold the flag of the tag, best one first */
	private static List<File> findFlagFiles(String tag) {
		List<File> found = new ArrayList<File>();
		for (Map<String, File> index : getIndexes()) {
			File file = findInIndex(index, tag);
			if (file != null && !found.contains(file)) {
				found.add(file);
			}
		}
		return found;
	}

	/** Finds the file of the tag. Prefers a plain TAG flag over a government
	 * variant like TAG_monarchy. */
	private static File findInIndex(Map<String, File> index, String tag) {
		String key = tag.toUpperCase();
		for (String extension : EXTENSIONS) {
			File file = index.get(key + extension.toUpperCase());
			if (file != null) {
				return file;
			}
		}
		for (String variant : VARIANTS) {
			for (String separator : new String[] { "_", " ", "-" }) {
				String variantKey = key + separator + variant.toUpperCase();
				for (String extension : EXTENSIONS) {
					File file = index.get(variantKey + extension.toUpperCase());
					if (file != null) {
						return file;
					}
				}
			}
		}
		List<String> names = new ArrayList<String>(index.keySet());
		Collections.sort(names);
		for (String name : names) {
			if (name.startsWith(key + "_") || name.startsWith(key + " ") || name.startsWith(key + "-")) {
				return index.get(name);
			}
		}
		return null;
	}

	/** Reads the image of a file, remembering it for the next country with the
	 * same flag */
	private static Image load(File file) {
		String key;
		try {
			key = file.getCanonicalPath();
		} catch (IOException e) {
			key = file.getAbsolutePath();
		}
		if (loaded.containsKey(key)) {
			return loaded.get(key);
		}
		Image image = null;
		try {
			if (file.getName().toLowerCase().endsWith(".tga")) {
				image = convert(readTga(file));
			} else {
				image = read(new FileInputStream(file));
			}
		} catch (Exception e) {
			image = null;
		}
		if (image != null && !image.isError()) {
			loaded.put(key, image);
		}
		return image;
	}

	private static BufferedImage readTga(File file) throws IOException {
		return TgaReader.read(Files.readAllBytes(file.toPath()));
	}

	/** JavaFX has no TGA support, so the decoded image is written as a PNG
	 * first, which JavaFX does understand */
	private static Image convert(BufferedImage image) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return read(new ByteArrayInputStream(out.toByteArray()));
	}

	/** JavaFX only reads images that are already completely in memory, so the
	 * stream is used instead of an URL */
	private static Image read(InputStream stream) throws IOException {
		try {
			Image image = new Image(stream);
			if (image.isError()) {
				return null;
			}
			return image;
		} finally {
			stream.close();
		}
	}

	/** The flag that comes with the analyzer itself */
	private static Image loadBundled(String tag) {
		String key = "bundled " + tag.toUpperCase();
		if (loaded.containsKey(key)) {
			return loaded.get(key);
		}
		URL url = ModFlags.class.getResource(Reference.FLAGPATH + tag + ".png");
		if (url == null) {
			return null;
		}
		Image image;
		try {
			image = read(url.openStream());
		} catch (Exception e) {
			return null;
		}
		if (image == null) {
			return null;
		}
		loaded.put(key, image);
		return image;
	}

	/** One index per gfx/flags folder, the one of the used mod first */
	private static synchronized List<Map<String, File>> getIndexes() {
		if (indexes != null) {
			return indexes;
		}
		List<Map<String, File>> result = new ArrayList<Map<String, File>>();
		for (File folder : ModFinder.flagFolders()) {
			result.add(indexFolder(folder));
		}
		if (!Reference.INSTALLPATH.isEmpty()) {
			result.add(indexFolder(new File(Reference.INSTALLPATH + "/gfx/flags")));
		}
		indexes = result;
		return indexes;
	}

	/** All flag files of one folder, name without path and in upper case */
	private static Map<String, File> indexFolder(File folder) {
		Map<String, File> index = new HashMap<String, File>();
		File[] files = folder.listFiles();
		if (files == null) {
			return index;
		}
		for (File file : files) {
			if (file.isFile()) {
				index.put(file.getName().toUpperCase(), file);
			}
		}
		return index;
	}
}