package com.hazmit.nas_runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			String name = file.getName().toLowerCase()
					.replaceAll("\\.dvdmedia", "")
					.replaceAll("[^A-Za-z0-9]", " ").trim();
			name = name.replaceAll("dis[ck]\\s?\\d+$", "");
			name = name.replaceAll("[s]\\d+\\s?[d]\\d+$", "");
			name = name.replaceAll("[d]\\d+$", "");
			return WordUtils.capitalizeFully(name);
		}
	}

	public static String guessComponentName(File file) {
		Pattern p = Pattern.compile("[0-9]+$");
		if ("VIDEO_TS".equalsIgnoreCase(file.getName())) {
			return guessComponentName(file.getParentFile());
		} else {
			String name = file.getName().toLowerCase()
					.replaceAll("\\.dvdmedia", "")
					.replaceAll("[^A-Za-z0-9]", " ").trim();
			if (name.matches(".+dis[ck]\\s?\\d+$")) {
				Matcher m = p.matcher(name);
				if (m.find()) {
					return "d" + m.group();
				}
			}
			if (name.matches(".+[s]\\d+\\s?[d]\\d+$")) {
				return name.substring(name.indexOf("s", name.length() - 6),
						name.length()).replace(" ", "");
			}
			if (name.matches(".+[d]\\d+$")) {
				Matcher m = p.matcher(name);
				if (m.find()) {
					return "d" + m.group();
				}
			}
			return "-";
		}
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

	public static String getHandbrakeVersion(String handbrakeLocation, String workingDir) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(handbrakeLocation, "--version");
		Map<String, String> env = pb.environment();
		pb.directory(new File(workingDir));
		Process p = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line,result="";
		while ((line = reader.readLine()) != null) {
			result += line;
		}
		return result;
	}

	public static Set<Integer> getTitleDurations(String handbrakeLocation, String workingDir) throws IOException,ParseException {
		Pattern pa = Pattern.compile("[0-9]+");
		ProcessBuilder pb = new ProcessBuilder(handbrakeLocation, "-i", workingDir, "-t", "0");
		Map<String, String> env = pb.environment();
		pb.directory(new File(workingDir));
		Process p = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String line;
		Integer currentTitle=0;
		Set<Integer> results=new HashSet<Integer>();
		while ((line = reader.readLine()) != null) {
			if (line.toLowerCase().indexOf("title") > -1 && line.toLowerCase().indexOf('+') == 0) {
				Matcher m = pa.matcher(line);
				if (m.find()) {
					currentTitle = Integer.parseInt(m.group());
				}
			}
		    if (line.toLowerCase().indexOf("duration") > -1 && line.toLowerCase().indexOf('+') == 2) {
		    	Date d = new SimpleDateFormat("HH:mm:ss").parse(line.replaceAll("  \\+ duration: ", ""));
				if (((d.getHours() * 60) + d.getMinutes()) >= 20) {
					results.add(currentTitle);
				}
			}
		}
		return results;
	}
	
	
	public static void main(String[] args) {

		try {
			System.out.println("Welcome to the NAS Runner");

			String version=getHandbrakeVersion(args[0], args[1]);
			System.out.println("\t" + version);
			

			Map<String, ShowOrMovie> data = findDvdsAndShows(args[1]);
			
			Iterator<Entry<String, ShowOrMovie>> it = data.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<String, ShowOrMovie> e = it.next();
				System.out.println(e.getValue().getName());
				Iterator<String> it2 = e.getValue().getComponents().keySet()
						.iterator();
				while (it2.hasNext()) {
					String discTitle = it2.next();
					String directoryName = e.getValue().getComponents().get(discTitle).getAbsolutePath();
					System.out.println("\t" + discTitle);
					Set<Integer> tracks=getTitleDurations(args[0], directoryName);
					System.out.println("\t\t" + tracks);
					for (Integer trackNumber : tracks) {
						encode(args[0],args[1],directoryName,e.getValue().getName() + "_" + discTitle + "_T" + trackNumber,trackNumber);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Map<String, ShowOrMovie> findDvdsAndShows(String workingDir) {
		Map<String, ShowOrMovie> data = new HashMap<String, ShowOrMovie>();
		List<File> files = findDVDS(new File(workingDir));
		for (File file : files) {
			String name = guessName(file);
			String componentName = guessComponentName(file);
			if (!data.containsKey(name)) {
				data.put(name, new ShowOrMovie(name));
			}
			data.get(name).getComponents().put(componentName, file);
		}
		return data;
	}
	
	private static void encode(String handbrakeLocation, String outputDir, String encodeDir, String title, Integer track) throws IOException {
		System.out.println(outputDir + "/" + title +".mp4");
		if (!new File(outputDir + "/" + title +".mp4").exists()) {
			ProcessBuilder pb = new ProcessBuilder(handbrakeLocation, "-i", encodeDir, "-t", "" + track,"-Z","AppleTV 3", "-E", "ffaac", "-o", outputDir + "/" + title +".mp4");
			Map<String, String> env = pb.environment();
			pb.directory(new File(encodeDir));
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(
                                        p.getErrorStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
			while ((line = reader2.readLine()) != null) {
                                System.out.println(line);
                        }
		}
	}
	
}
