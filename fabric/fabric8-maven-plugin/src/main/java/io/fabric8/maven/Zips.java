/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 */
public class Zips {
    /**
     * Creates a zip fie from the given source directory and output zip file name
     */
    public static void createZipFile(Log log, File sourceDir, File outputZipFile) throws IOException {
        outputZipFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(outputZipFile);
        ZipOutputStream zos = new ZipOutputStream(os);
        try {
            //zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            //zos.setLevel(Deflater.NO_COMPRESSION);
            String path = "";
            FileFilter filter = null;
            zipDirectory(log, sourceDir, zos, path, filter);

        } finally {
            try {
                zos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Zips the directory recursively into the ZIP stream given the starting path and optional filter
     */
    public static void zipDirectory(Log log, File directory, ZipOutputStream zos, String path, FileFilter filter) throws IOException {
        // get a listing of the directory content
        File[] dirList = directory.listFiles();
        byte[] readBuffer = new byte[8192];
        int bytesIn = 0;
        // loop through dirList, and zip the files
        if (dirList != null) {
            for (File f : dirList) {
                if (f.isDirectory()) {
                    String prefix = path + f.getName() + "/";
                    zos.putNextEntry(new ZipEntry(prefix));
                    zipDirectory(log, f, zos, prefix, filter);
                } else {
                    String entry = path + f.getName();
                    if (filter == null || filter.accept(f)) {
                        FileInputStream fis = new FileInputStream(f);
                        try {
                            ZipEntry anEntry = new ZipEntry(entry);
                            zos.putNextEntry(anEntry);
                            bytesIn = fis.read(readBuffer);
                            while (bytesIn != -1) {
                                zos.write(readBuffer, 0, bytesIn);
                                bytesIn = fis.read(readBuffer);
                            }
                        } finally {
                            fis.close();
                        }
                        log.info("zipping file " + entry);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public static List<String> notNullList(List<String> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }
}
