package ee.tkasekamp.vickywaranalyzer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Finds the mods that belong to the save game. The mods the user pointed at
 * are used first, after that the ones that are found on their own: the mod
 * folder of the install directory and the ones in the Paradox user folder. The
 * mod the save game was played with comes before those, since a save game
 * usually only works with that one.
 *
 * Both the flags and the country names are searched in these mods, in this
 * order, so that a mod always wins from the game itself. */
public class ModFinder {
	private static final int MAX_DEPTH = 3;
	/* A folder with one of these is a mod itself and is not searched any deeper */
	private static final String[] MOD_FOLDERS = { "localisation", "common", "gfx" };

	private static List<File> mods;

	private ModFinder() {
		super();
	}

	/** Throws away everything that was searched before, needed when another
	 * save game or another set of mod folders is used */
	public static synchronized void reset() {
		mods = null;
	}

	/** The mod folders that are used, the one of the save game first */
	public static synchronized List<File> modFolders() {
		if (mods == null) {
			List<File> result = new ArrayList<File>();
			/* The mods the user pointed at are always used */
			for (File root : userRoots()) {
				collectMods(root, MAX_DEPTH, result);
			}
			if (!isVanillaSave()) {
				List<File> found = new ArrayList<File>();
				for (File root : automaticRoots()) {
					collectMods(root, MAX_DEPTH, found);
				}
				String modName = saveGameModName();
				File saveGameMod = find(found, modName);
				if (saveGameMod == null) {
					/* The save game belongs to a mod that is not installed
					 * here, so every mod that is found is better than none */
					for (File mod : found) {
						addIfNew(result, mod);
					}
				} else {
					addIfNew(result, saveGameMod);
				}
			}
			mods = result;
		}
		return mods;
	}

	/** The gfx/flags folders of those mods, in the same order */
	public static synchronized List<File> flagFolders() {
		List<File> result = new ArrayList<File>();
		for (File mod : modFolders()) {
			File flagFolder = new File(mod, "gfx/flags");
			if (flagFolder.isDirectory()) {
				result.add(flagFolder);
			}
		}
		return result;
	}

	/** True when the save game is a game without a mod, in which case the names
	 * and flags of the mods do not belong to it */
	private static boolean isVanillaSave() {
		File folder = saveGameFolder();
		if (folder == null) {
			return true;
		}
		String name = folder.getName();
		return name.equalsIgnoreCase("save games") || name.equalsIgnoreCase("Victoria II")
				|| samePath(folder, new File(Reference.INSTALLPATH));
	}

	/** The folder a mod keeps its save games in, null when the save game is not
	 * in one */
	private static File saveGameFolder() {
		if (Reference.saveGameFile == null || Reference.saveGameFile.trim().isEmpty()) {
			return null;
		}
		File saveGames = new File(Reference.saveGameFile).getParentFile();
		if (saveGames == null) {
			return null;
		}
		File folder = saveGames.getParentFile();
		return folder == null ? null : folder;
	}

	/** Name of the mod the save game belongs to, null when unknown */
	private static String saveGameModName() {
		File folder = saveGameFolder();
		return folder == null ? null : folder.getName();
	}

	private static File find(List<File> folders, String name) {
		if (name == null) {
			return null;
		}
		for (File folder : folders) {
			if (folder.getName().equalsIgnoreCase(name)) {
				return folder;
			}
		}
		/* Saves of a mod are kept in a folder named after it, but that folder
		 * does not always have the same name as the mod itself */
		for (File folder : folders) {
			if (initials(folder.getName()).equalsIgnoreCase(name)) {
				return folder;
			}
		}
		return null;
	}

	/** The first letter of every word, as DoD of Divergences of Darkness */
	private static String initials(String name) {
		StringBuilder result = new StringBuilder();
		boolean start = true;
		for (int i = 0; i < name.length(); i++) {
			char letter = name.charAt(i);
			if (letter == ' ' || letter == '_' || letter == '-') {
				start = true;
				continue;
			}
			if (start) {
				result.append(letter);
				start = false;
			}
		}
		return result.toString();
	}

	/** The folders the user pointed at */
	private static List<File> userRoots() {
		List<File> result = new ArrayList<File>();
		if (Reference.MODPATH != null) {
			for (String path : Reference.MODPATH.split(";")) {
				addPath(result, path);
			}
		}
		return result;
	}

	/** The folders that may hold mods without the user saying so */
	private static List<File> automaticRoots() {
		List<File> result = new ArrayList<File>();
		addPath(result, Reference.INSTALLPATH + "/mod");
		String user = System.getProperty("user.home");
		addPath(result, user + "/Documents/Paradox Interactive/Victoria II/mod");
		addPath(result, user + "/OneDrive/Documents/Paradox Interactive/Victoria II/mod");
		return result;
	}

	private static void addIfNew(List<File> folders, File folder) {
		if (!contains(folders, folder)) {
			folders.add(folder);
		}
	}

	private static void addPath(List<File> roots, String path) {
		if (path == null || path.trim().isEmpty()) {
			return;
		}
		File folder = new File(path.trim());
		if (folder.isDirectory() && !contains(roots, folder)) {
			roots.add(folder);
		}
	}

	private static boolean contains(List<File> folders, File folder) {
		for (File each : folders) {
			if (samePath(each, folder)) {
				return true;
			}
		}
		return false;
	}

	private static boolean samePath(File one, File other) {
		if (other == null) {
			return false;
		}
		return one.getAbsolutePath().equalsIgnoreCase(other.getAbsolutePath());
	}

	private static void collectMods(File folder, int depth, List<File> result) {
		if (depth < 0) {
			return;
		}
		if (isMod(folder)) {
			if (!contains(result, folder)) {
				result.add(folder);
			}
			return;
		}
		for (File child : childrenOf(folder)) {
			collectMods(child, depth - 1, result);
		}
	}

	private static boolean isMod(File folder) {
		for (String name : MOD_FOLDERS) {
			if (new File(folder, name).isDirectory()) {
				return true;
			}
		}
		return false;
	}

	private static List<File> childrenOf(File folder) {
		File[] children = folder.listFiles();
		if (children == null) {
			return Collections.emptyList();
		}
		List<File> result = new ArrayList<File>();
		for (File child : children) {
			if (child.isDirectory() && !child.getName().startsWith(".")) {
				result.add(child);
			}
		}
		Collections.sort(result, new Comparator<File>() {
			@Override
			public int compare(File one, File other) {
				return one.getName().compareTo(other.getName());
			}
		});
		return result;
	}
}