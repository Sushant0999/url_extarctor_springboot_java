package com.url.extractor.utils;

import com.url.extractor.model.UrlData;
import com.url.extractor.service.UrlDataService;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDirectory {

    public static void zip() throws IOException {
        //Getting directory information
        String filePath = System.getProperty("user.dir");
        File directoryPath = new File(System.getProperty("os.name").contains("Windows") ? filePath + "\\data\\temp\\" : filePath + "//data//temp//");
        String sourceFile = UrlDataService.pageName;
        //Getting links to a List
        List<String> links = UrlDataService.data.stream()
                .map(UrlData::getAnchorTags)
                .flatMap(List::stream)
                .distinct()
                .toList();
        //Adding this links to a text file
        writeLinks(links, sourceFile);

        FileOutputStream fos = new FileOutputStream(directoryPath + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        File fileToZip = new File(sourceFile);
        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static void writeLinks(List<String> list, String dir) {
        //Replacing '\' with '\\'

        dir = System.getProperty("os.name").contains("Windows") ? dir.replace("\\", "\\\\") : dir.replace("/", "//");
        //Extracting folder name
        System.out.println(Arrays.toString(dir.split("//")));
        String dirName = "";
        if(System.getProperty("os.name").contains("Windows")){
            String[] arr = dir.split("\\\\");
            dirName = arr[arr.length - 1];
        }else{
            String[] arr = dir.split("//");
            dirName = arr[arr.length - 1];
        }

        try {
            //Getting directory info
            String filePath = System.getProperty("user.dir");
            File directoryPath = new File(System.getProperty("os.name").contains("Windows") ? filePath + "\\data\\temp\\" + dirName + "\\temp.txt" : filePath + "//data//temp//" + dirName + "/temp.txt");
            PrintWriter pw = new PrintWriter(directoryPath);
            for (String link : list) {
                //Writing link on text file
                pw.write(link + "\n");
            }
            //PrintWriter Closed
            pw.close();
            MyLogger.info("LINKS ADDED TO FILE");
        } catch (IOException e) {
            MyLogger.err(e.getMessage());
        }
    }
}