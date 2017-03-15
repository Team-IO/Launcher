/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.builder;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skcraft.launcher.model.modpack.FileInstall;
import com.skcraft.launcher.model.modpack.Manifest;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Walks a path and adds hashed path versions to the given
 * {@link com.skcraft.launcher.model.modpack.Manifest}.
 */
@Log
public class ClientFileCollector extends DirectoryWalker {

    public static final String URL_FILE_SUFFIX = ".url.txt";

    private final Manifest manifest;
    private final PropertiesApplicator applicator;
    private final File destDir;
    private HashFunction hf = Hashing.sha1();
    private Gson gson = new GsonBuilder().create();

    /**
     * Create a new collector.
     *
     * @param manifest the manifest
     * @param applicator applies properties to manifest entries
     * @param destDir the destination directory to copy the hashed objects
     */
    public ClientFileCollector(@NonNull Manifest manifest, @NonNull PropertiesApplicator applicator,
                               @NonNull File destDir) {
        this.manifest = manifest;
        this.applicator = applicator;
        this.destDir = destDir;
    }

    @Override
    protected DirectoryBehavior getBehavior(@NonNull String name) {
        return getDirectoryBehavior(name);
    }

    @Override
    protected void onFile(File file, String relPath) throws IOException {
        if (file.getName().endsWith(FileInfoScanner.FILE_SUFFIX) || file.getName().endsWith(URL_FILE_SUFFIX)) {
            return;
        }

        String hash;
        String to;
        boolean copy = false;
        String location = "";
        FileInstall entry = new FileInstall();

        if (file.getName().endsWith(".url")){
            String name = FilenameUtils.getBaseName(file.getName());
            URL url = new URL(FileUtils.readLines(file, "UTF-8").get(1));
            File filetmp = File.createTempFile("temp",".jar");
            try {
                FileUtils.copyURLToFile(url, filetmp);
            }catch (IOException e){
                filetmp.delete();
                File failedFile = new File(file.getAbsolutePath() + File.pathSeparator + "FAILED" + file.getName());
                FileUtils.moveFileToDirectory(file, failedFile, true);
                return;
            }
            HashObject h = new HashObject();
            hash = Files.hash(filetmp, hf).toString();
            h.setHash(hash);
            h.setLocation(url.toString());
            h.setTo(FilenameUtils.separatorsToUnix(FilenameUtils.normalize(relPath)));
            h.setSize(file.length());
            file = new File(name + ".json");
            filetmp.delete();
            FileUtils.writeStringToFile(file , gson.toJson(h), "UTF-8");
        }

        if (file.getName().endsWith(".jar")) {

            hash = Files.hash(file, hf).toString();
            to = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(relPath));

            // url.txt override file
            File urlFile = new File(file.getAbsoluteFile().getParentFile(), file.getName() + URL_FILE_SUFFIX);
            copy = true;
            if (urlFile.exists() && !System.getProperty("ignoreURLOverridescom.skcraft.builder.", "false").equalsIgnoreCase("true")) {
                location = Files.readFirstLine(urlFile, Charset.defaultCharset());
                copy = false;
            } else {
                location = hash.substring(0, 2) + "/" + hash.substring(2, 4) + "/" + hash;
            }
            entry.setHash(hash);
            entry.setLocation(location);
            entry.setTo(to);
            entry.setSize(file.length());
        }

        if (file.getName().endsWith(".json")){
            HashObject h = gson.fromJson(FileUtils.readFileToString(file,"UTF-8"), HashObject.class);
            entry.setHash(h.getHash());
            entry.setLocation(h.getLocation());
            entry.setTo(h.getTo());
            entry.setSize(h.getSize());
        }
        File destPath = new File(destDir, location);

        applicator.apply(entry);
        destPath.getParentFile().mkdirs();
        ClientFileCollector.log.info(String.format("Adding %s from %s...", relPath, file.getAbsolutePath()));
        if (copy) {
            Files.copy(file, destPath);
        }
        manifest.getTasks().add(entry);
    }

    public static DirectoryBehavior getDirectoryBehavior(@NonNull String name) {
        if (name.startsWith(".")) {
            return DirectoryBehavior.SKIP;
        } else if (name.equals("_OPTIONAL")) {
            return DirectoryBehavior.IGNORE;
        } else if (name.equals("_SERVER")) {
            return DirectoryBehavior.SKIP;
        } else if (name.equals("_CLIENT")) {
            return DirectoryBehavior.IGNORE;
        } else {
            return DirectoryBehavior.CONTINUE;
        }
    }

}
