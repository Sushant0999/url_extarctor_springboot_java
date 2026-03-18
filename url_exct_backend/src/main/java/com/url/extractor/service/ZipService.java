package com.url.extractor.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    public Path zipDirectory(String sourceDirPath) throws IOException {
        Path sourcePath = Paths.get(sourceDirPath);
        String zipFileName = sourcePath.getFileName().toString() + ".zip";
        Path zipPath = sourcePath.getParent().resolve(zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            File dirToZip = sourcePath.toFile();
            addDirToZip(zos, dirToZip, "");
        }

        return zipPath;
    }

    private void addDirToZip(ZipOutputStream zos, File fileToZip, String parentDirectoryName) throws IOException {
        if (fileToZip == null || !fileToZip.exists()) {
            return;
        }

        String zipEntryName = parentDirectoryName + fileToZip.getName();
        if (fileToZip.isDirectory()) {
            zipEntryName += "/";
            zos.putNextEntry(new ZipEntry(zipEntryName));
            zos.closeEntry();

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    addDirToZip(zos, childFile, zipEntryName);
                }
            }
        } else {
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zos.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                zos.closeEntry();
            }
        }
    }
}
