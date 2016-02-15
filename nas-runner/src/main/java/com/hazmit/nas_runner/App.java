package com.hazmit.nas_runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.WordUtils;

/**
 * Hello world!
 *
 */
public class App {

	public static boolean looksLikeDVD(File file) {
		if (file == null || file.isFile()) {
			return false;
		} else if (file.isDirectory()) {
			for (String subFileName : file.list()) {
				if (subFileName.endsWith("VOB") || subFileName.endsWith("vob")) {
					return true;
				}
			}
		}
		return false;
	}

	public static String guessName(File file) {
		if ("VIDEO_TS".equalsIgnoreCase(file.getName())) {
			return guessName(file.getParentFile());
		} else {
			String name=file.getName().toLowerCase().replaceAll("\\.dvdmedia", "").replaceAll("[^A-Za-z0-9]", " ").trim();
			name = name.replaceAll("dis[ck]\\s?\\d+$", "");
			name = name.replaceAll("[s]\\d+\\s?[d]\\d+$", "");
			name = name.replaceAll("[d]\\d+$", "");
			return WordUtils.capitalizeFully(name);
		}
	}
	
	public static String guessComponentName(File file) {
		return file.getAbsolutePath();
	}

	public static List<File> findDVDS(File file) {
		List<File> files = new ArrayList<File>();
		if (file == null || file.isFile()) {
			return Collections.EMPTY_LIST;
		} else if (file.isDirectory() && looksLikeDVD(file)) {
			files.add(file);
		} else if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				files.addAll(findDVDS(subFile));
			}
		}
		return files;
	}

	public static void main(String[] args) {

		try {
			System.out.println("Welcome to the NAS Runner");
			// Sun's ProcessBuilder and Process example
			ProcessBuilder pb = new ProcessBuilder(args[0], "--version");
			Map<String, String> env = pb.environment();
			// env.put("VAR1", "myValue");
			// env.remove("OTHERVAR");
			// env.put("VAR2", env.get("VAR1") + "suffix");
			pb.directory(new File(args[1]));
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("\t" + line);
			}

			Map<String, ShowOrMovie> data = new HashMap<String, ShowOrMovie>();

			List<File> files = findDVDS(new File(args[1]));
			for (File file : files) {
				String name = guessName(file);
				String componentName = guessComponentName(file);
				if (!data.containsKey(name)) {
					data.put(name, new ShowOrMovie(name));
				}
				data.get(name).getComponents().put(componentName, file);
			}
			Iterator<Entry<String, ShowOrMovie>> it = data.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<String, ShowOrMovie> e = it.next();
				System.out.println(e.getValue().getName());
				Iterator<String> it2 = e.getValue().getComponents().keySet().iterator();
				while (it2.hasNext()) {
					System.out.println("\t" + it2.next());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
