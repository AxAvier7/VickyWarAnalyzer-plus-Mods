package ee.tkasekamp.vickywaranalyzer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the government of every country from the save game. Mods name their
 * countries after the government, as USA_hms_government, so the government is
 * needed to find the name the game itself shows. */
public class GovernmentReader {
	/* A country block starts in the first column, its own keys are one tab deep */
	private static final Pattern COUNTRY = Pattern.compile("^([A-Z0-9]{2,4})=");
	private static final String GOVERNMENT = "\tgovernment=";
	private static final String END = "}";
	private static final String QUOTE = "\"";

	private GovernmentReader() {
		super();
	}

	/** Government of every country of the save game, empty when the save game
	 * cannot be read */
	public static Map<String, String> read(String saveGamePath) {
		Map<String, String> governments = new HashMap<String, String>();
		File saveGame = new File(saveGamePath == null ? "" : saveGamePath);
		if (!saveGame.isFile()) {
			return governments;
		}
		BufferedReader scanner = null;
		try {
			scanner = new BufferedReader(
					new InputStreamReader(new FileInputStream(saveGame), "ISO8859_1"), 65536);
			String tag = null;
			String line;
			while ((line = scanner.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				if (line.charAt(0) == '\t') {
					if (tag != null && line.startsWith(GOVERNMENT)) {
						governments.put(tag, value(line.substring(GOVERNMENT.length())));
					}
					continue;
				}
				/* The brace that opens the block is on its own line and is no
				 * country of its own, only the end of one closes it */
				if (line.equals(END)) {
					tag = null;
					continue;
				}
				Matcher matcher = COUNTRY.matcher(line);
				tag = matcher.find() ? matcher.group(1) : tag;
			}
		} catch (IOException e) {
			/* A save game that cannot be read is reported by the parser */
		} finally {
			close(scanner);
		}
		return governments;
	}

	private static String value(String text) {
		String value = text.trim();
		if (value.length() > 1 && value.startsWith(QUOTE) && value.endsWith(QUOTE)) {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static void close(BufferedReader scanner) {
		if (scanner == null) {
			return;
		}
		try {
			scanner.close();
		} catch (IOException e) {
			/* Nothing can be done about a file that will not close */
		}
	}
}