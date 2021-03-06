package com.github.lunatrius.core.version;

import com.github.lunatrius.core.LunatriusCore;
import com.github.lunatrius.core.lib.Reference;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class VersionChecker {
	private static final List<ModMetadata> REGISTERED_MODS = new ArrayList<ModMetadata>();
	private static final Map<String, String> OUTDATED_MODS = new HashMap<String, String>();
	private static boolean done = false;

	public static void registerMod(ModMetadata modMetadata) {
		REGISTERED_MODS.add(modMetadata);

		if (modMetadata.description != null) {
			modMetadata.description += String.format("\n---\nRecommended Forge: %s", Reference.FORGE);
		}
	}

	public static Set<Map.Entry<String, String>> getOutdatedMods() {
		return OUTDATED_MODS.entrySet();
	}

	public static void setDone(boolean isDone) {
		done = isDone;
	}

	public static boolean isDone() {
		return done;
	}

	public static void startVersionCheck() {
		new Thread("LunatriusCore Version Check") {
			@Override
			public void run() {
				try {
					URL url = new URL(String.format("http://mc.lunatri.us/json?latest=1&mc=%s", Reference.MINECRAFT));
					InputStream con = url.openStream();
					String data = new String(ByteStreams.toByteArray(con));
					con.close();

					Map<String, Object> json = new Gson().fromJson(data, Map.class);

					if (json.get("version").equals(1.0)) {
						Map<String, Map<String, Map<String, String>>> mods = (Map<String, Map<String, Map<String, String>>>) json.get("mods");

						for (ModMetadata modMetadata : REGISTERED_MODS) {
							String modid = modMetadata.modId;
							ArtifactVersion versionLocal = new DefaultArtifactVersion(modMetadata.version);

							try {
								DefaultArtifactVersion versionRemote = new DefaultArtifactVersion(mods.get(modid).get("latest").get("version"));
								int diff = versionRemote.compareTo(versionLocal);

								if (diff > 0) {
									OUTDATED_MODS.put(modMetadata.name, String.format("%s -> %s", versionLocal, versionRemote));
									modMetadata.description += String.format("\nUpdate is available (%s -> %s)!", versionLocal, versionRemote);
									LunatriusCore.logger.info(String.format("Update is available for %s (%s -> %s)!", modid, versionLocal, versionRemote));
								} else if (diff == 0) {
									modMetadata.description += "\nUp to date!";
									LunatriusCore.logger.info(String.format("%s is up to date!", modid));
								} else {
									LunatriusCore.logger.info(String.format("Is %s from the future?", modid));
								}
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				done = true;
			}
		}.start();
	}
}
