package ee.tkasekamp.vickywaranalyzer.util;

import ee.tkasekamp.vickywaranalyzer.core.Country;
import ee.tkasekamp.vickywaranalyzer.gui.GuiController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ee.tkasekamp.vickywaranalyzer.util.Reference.countryList;

/** Gives every country of the save game the name the game shows for it. The
 * names are read from the localisation files of the localisation folder. Mods
 * rename their countries and are read before the game itself, so a mod always
 * wins. Mods name a country after its government as well, as USA_hms_government,
 * which is why the government is read from the save game first. */
public class Localisation {
	private GuiController controller;

	public Localisation(GuiController controller) {
		this.controller = controller;
	}

	/** Main method of this class. Manages the reading from csv */
	public void readLocalisation() {
		if (controller.getLocalisationCheck().isSelected()) {
			try {
				Names names = new Names(countryList);
				readGovernments(names);
				/* The mods of the save game come before the game itself */
				for (File mod : ModFinder.modFolders()) {
					names.read(new File(mod, "localisation"), true);
				}
				if (!Reference.INSTALLPATH.isEmpty()) {
					names.read(new File(Reference.INSTALLPATH, "localisation"), false);
				}
			} catch (RuntimeException e) {
				controller.getErrorLabel().setText(controller.getErrorLabel().getText()
						+ " Some or all of the localisation files could not be found. ");
			}
		}
		/* If some countries have not been found an official name, setting the tag as official name */
		for (Country country : countryList) {
			if (country.getOfficialName().equals("")) {
				country.setOfficialName(country.getTag());
			}
		}
	}

	/** Takes the government of every country from the save game, needed to find
	 * the name a mod gives to that kind of country */
	private void readGovernments(Names names) {
		Map<String, String> governments = GovernmentReader.read(Reference.saveGameFile);
		for (Country country : countryList) {
			String government = governments.get(country.getTag());
			if (government != null) {
				country.setGovernment(government);
				names.addGovernment(country, government);
			}
		}
	}

	/** Looks for the name of every country of the save game in the localisation
	 * files of one folder. The first name that is found is kept, so the files
	 * that are read later cannot change it. */
	private static class Names {
		/* Names a mod gives to a country, both TAG and TAG_government */
		private final Map<String, Country> wanted = new HashMap<String, Country>();
		private final Map<String, Country> byGovernment = new HashMap<String, Country>();

		Names(List<Country> countries) {
			for (Country country : countries) {
				wanted.put(country.getTag(), country);
			}
		}

		void addGovernment(Country country, String government) {
			if (!government.isEmpty()) {
				byGovernment.put(country.getTag() + "_" + government, country);
			}
		}

		/** Reads every csv of the folder. A mod reads its files backwards,
		 * since the game lets the last file win, while the files of the game
		 * itself are read in the order they come in. */
		void read(File folder, boolean newestFileFirst) {
			File[] files = folder.listFiles();
			if (files == null) {
				return;
			}
			List<File> localisation = new ArrayList<File>();
			for (File file : files) {
				if (file.isFile() && file.getName().toLowerCase().endsWith(".csv")) {
					localisation.add(file);
				}
			}
			if (newestFileFirst) {
				Collections.sort(localisation, new Comparator<File>() {
					@Override
					public int compare(File one, File other) {
						return other.getName().compareTo(one.getName());
					}
				});
			}
			for (File file : localisation) {
				read(file);
			}
		}

		/** Reads one file. Only the few keys the save game needs are looked up,
		 * instead of the name of every country for every line of the file. */
		private void read(File file) {
			BufferedReader scanner = null;
			try {
				scanner = new BufferedReader(
						new InputStreamReader(new FileInputStream(file), "ISO8859_1"), 65536);
				String line;
				while ((line = scanner.readLine()) != null) {
					int end = line.indexOf(';');
					if (end < 1) {
						continue;
					}
					String key = line.substring(0, end);
					/* The name of the government of the country wins from the
					 * plain name, no matter which of the two comes first */
					Country country = byGovernment.remove(key);
					if (country == null) {
						country = wanted.remove(key);
						if (country == null || !country.getOfficialName().isEmpty()) {
							continue;
						}
					}
					country.setOfficialName(name(line, end));
				}
			} catch (IOException e) {
				/* A file that cannot be read is skipped, the game would not
				 * show a name for those countries either */
			} finally {
				close(scanner);
			}
		}

		/** The name is the text of the line up to the second semicolon */
		private static String name(String line, int start) {
			int end = line.indexOf(';', start + 1);
			return (end < 0 ? line.substring(start + 1) : line.substring(start + 1, end)).trim();
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
}