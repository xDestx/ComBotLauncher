package com.mdc.combot.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ComBotLauncher {

	public static void main(String[] args) {
		Map<String,String> config = getConfig();
		boolean checkForUpdate;
		boolean autoDownload;
		
		System.out.println("\n====================\nMOTD: " + getMOTD() + "\n====================");
		
		if(config.get("check-update") != null && Boolean.parseBoolean(config.get("check-update"))) {
			checkForUpdate = true;
			if(config.get("auto-download-update") != null && Boolean.parseBoolean(config.get("auto-download-update"))) {
				autoDownload = true;
			} else {
				autoDownload = false;
			}
		} else {
			checkForUpdate = true;
			autoDownload = true;
			if(config.get("check-update") != null) {
				System.out.println("You have opted out of checking for an update.");
			} else {
				System.out.println("You seem to be missing\ncheck-update and auto-download-update\nin your conifg file. I'll set those to true for now, but be sure to add them to your file later.");
			}
		}
		System.out.println("Check for update: " + checkForUpdate + "\nAuto-download: " + autoDownload + "\n");
		String jarToLoad = "";
		if(checkForUpdate) {
			String versionToLoad;
			String version = getMostRecentVersion();
			String fullName = "ComBot.v" + version + ".jar";
			String currentVersion = checkCurrentVersion();
			if(currentVersion != null && version.compareToIgnoreCase(currentVersion) > 0) {
				//Version is newer
				System.out.println("Found that the most recommended version " + version + " is higher than your current version: " + currentVersion);
				versionToLoad = version;
			} else if (currentVersion != null){
				versionToLoad = currentVersion;
				System.out.println("Using current version");
			} else {
				versionToLoad = version;
				//launch missing
				File launchFile = new File((System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + ".combot-launch"));
				if(!launchFile.exists()) {
					try {
						launchFile.createNewFile();
						BufferedWriter bw = new BufferedWriter(new FileWriter(launchFile));
						bw.write("combot-version:" + versionToLoad);
						bw.flush();
						bw.close();
						System.out.println("Created launch file");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if((autoDownload && currentVersion == null) || (autoDownload && !currentVersion.equals(version))) {
				//Download
				System.out.println("Downloading recommended version");
				File destinationFile = new File((System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + "versions" + File.separatorChar + fullName));
				if(downloadComBotVersion(fullName, destinationFile)) {
					System.out.println("Download success");
					versionToLoad = version;
					try {
						File launchFile = new File((System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + ".combot-launch"));
						BufferedWriter bw = new BufferedWriter(new FileWriter(launchFile));
						bw.write("combot-version:" + versionToLoad);
						bw.flush();
						bw.close();
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Failed to update launch settings");
					}
				} else {
					System.out.println("Download failure");
					versionToLoad = currentVersion;
				}
			}
			jarToLoad = "ComBot.v" + versionToLoad + ".jar";
		}
		if(!jarToLoad.equals("")) {
			System.out.println("Starting bot..." + jarToLoad);
			loadComBot(new File((System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + "versions" + File.separatorChar + jarToLoad)));
		} else {
			System.out.println("Couldn't find jar to load. Trying running the bot directly.\nType the following into your command line: java -jar path/to/bot.jar");
		}
	}
	
	private static String getMOTD() {
		String motd = "Message of the Day: ";
		try {
			URL motdURL = new URL("https://plugin-combot.firebaseio.com/MOTD.json");
			String msg = "";
			Scanner s = new Scanner(motdURL.openStream());
			while(s.hasNextLine()) {
				msg+=s.nextLine();
			}
			s.close();
			motd+=msg.replace("\"", "");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			motd+=" -- Sorry, unable to check MOTD";
		} catch (IOException e) {
			e.printStackTrace();
			motd+=" -- Sorry, unable to check MOTD";
		}
		return motd;
	}
	
	protected static void loadComBot(File f) {
		if (!f.exists()) {
			System.out.println("Jar doesn't exist. " + f.getPath());
			return;
		}
		try {
			JarFile jar = new JarFile(f);
			URL loc = f.toURI().toURL();
			URLClassLoader loader = new URLClassLoader(new URL[] { loc });
			Class<?> launcherClass = loader.loadClass("com.mdc.combot.BotLauncher");
			
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class")) {
					loader.loadClass(entry.getName().replace(".class", "").replace("/","."));
				}
			}
			loader.close();
			jar.close();
			launcherClass.getMethod("main", String[].class).invoke(null, (Object)new String[0]);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
			//try {
		//		Runtime.getRuntime().exec("java -jar " + f.getAbsolutePath() + " &");
		//	} catch (IOException e) {
			//	e.printStackTrace();
			//}
	}
	
	private static String checkCurrentVersion() {
		File f = new File((System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + ".combot-launch"));
		Map<String,String> launchMap;
		if(f.exists()) {
			launchMap = mapFromFile(f);
			return launchMap.get("combot-version");
		} else {
			return null;
		}
	}
	

	private static Map<String,String> mapFromFile(File f) {
		try {
			Map<String,String> configMap = new HashMap<String,String>();
			
			Scanner s = new Scanner(new FileInputStream(f));
			while(s.hasNextLine()) {
				String ln = s.nextLine();
				if(ln.trim().startsWith("#") || ln.trim().equals("")) {
					continue;
				}
				String key = ln.substring(0, ln.indexOf(':')).replace(" ", "");
				String value = ln.substring(ln.indexOf(':')+1).trim();
				configMap.put(key, value);
			}
			s.close();
			return configMap;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error loading " + f.getName() + ".");
			return null;
		}
	}
	
	
	private static boolean downloadComBotVersion(String fullVersionName, File destinationFile) {
		try {
			if(!destinationFile.exists()) {
				if(destinationFile.getParent() != null && !destinationFile.getParentFile().exists()) {
					destinationFile.getParentFile().mkdirs();
				}
				destinationFile.createNewFile();
			}
			URL downloadURL = new URL("https://memedistributionco.github.io/jars/combot/" + fullVersionName);
			ReadableByteChannel rbc = Channels.newChannel(downloadURL.openStream());
			FileOutputStream fos = new FileOutputStream(destinationFile);
			System.out.println("Transfer start");
			long transferred = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			rbc.close();
			System.out.println("Transfer complete\n" + transferred + " bytes transferred.");
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	private static Map<String,String> getConfig() {
		File f = new File(System.getProperty("user.home") + File.separatorChar + "ComBot" + File.separatorChar + "settings" + File.separatorChar + "config.txt");
		if(!f.exists()) {
			Map<String,String> configMap = new HashMap<String,String>();
			configMap.put("check-update", "true");
			configMap.put("auto-download-update", "true");
			System.out.println("Couldn't find config, using default map");
			return configMap;
		} else {
			System.out.println("Found config file, loading");
			return mapFromFile(f);
		}
	}
	
	private static String getMostRecentVersion() {
		String toReturn = "";
		
		try {
			URL versionURL = new URL("https://plugin-combot.firebaseio.com/recommendedVersion.json");
			Scanner s = new Scanner(versionURL.openConnection().getInputStream());
			while(s.hasNext()) {
				toReturn+=s.next();
			}
			s.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return toReturn.replace("\"", "");
	}

	
	
}
