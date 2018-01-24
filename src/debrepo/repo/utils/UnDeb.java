package debrepo.repo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class UnDeb {

    private static boolean DEBUG = false;

    public static List<File> unDeb(final File inputDeb, final File outputDir, boolean verbose) throws IOException, ArchiveException {
        final List<File> unpackedFiles = new ArrayList<File>();
        DEBUG = verbose;
        System.out.println(String.format("Unzipping deb file %s.", inputDeb.getAbsoluteFile()));

        if (!outputDir.exists()) {
            System.out.println(String.format("Creating output directory %s.", outputDir.getAbsoluteFile()));
            outputDir.mkdirs();
        }
        final InputStream is = new FileInputStream(inputDeb);
        final ArArchiveInputStream debInputStream = (ArArchiveInputStream) new ArchiveStreamFactory()
                .createArchiveInputStream("ar", is);
        ArArchiveEntry entry = null;
        while ((entry = (ArArchiveEntry) debInputStream.getNextEntry()) != null) {
            if(DEBUG)System.out.println("Read entry: " + entry.getName() + " isDirectory: " + entry.isDirectory());
            File outputFile = new File(outputDir, entry.getName());
            if(DEBUG)System.out.println("Dest:" + outputFile.getAbsolutePath());

            if (entry.isDirectory()) {
                outputFile = new File(outputDir, entry.getName());
                if(DEBUG)System.out.println("Making directory:" + outputFile.getAbsolutePath());
                if (!outputFile.mkdirs()) {
                    throw new RuntimeException("Could not create directory " + outputFile.getAbsolutePath());
                }
                unpackedFiles.add(outputFile);
            } else if (entry.getName().toLowerCase().endsWith(".gz")) {
                // RECURSIVE CALL
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, outputDir);
                outputFile.delete();
            } else if (entry.getName().toLowerCase().endsWith(".tar")) {
                // RECURSIVE CALL
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, new File(outputFile.getPath().replace(".tar", "")));
            } else {
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                unpackedFiles.add(outputFile);
            }
        }
        debInputStream.close();
        return unpackedFiles;
    }

    public static List<File> untar(final File inputtar, final File outputDir) throws IOException, ArchiveException {
        final List<File> unpackedFiles = new ArrayList<File>();
        System.out.println(String.format("Unzipping tar file %s to %s", inputtar.getAbsoluteFile(),
                outputDir.getAbsolutePath()));

        final InputStream is = new FileInputStream(inputtar);
        final TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new TarArchiveInputStream(is);
        TarArchiveEntry entry = null;
        while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
            if(DEBUG)System.out.println("Read entry: " + entry.getName() + " isDirectory: " + entry.isDirectory());
            String filename = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);

            if (entry.isDirectory()) {
                if (entry.getName() != "./" && entry.getName() != ".") {
                    File outputFile = new File(outputDir, entry.getName());
                    if(DEBUG)System.out.println("Making directory:" + outputFile.getAbsolutePath());
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        tarInputStream.close();
                        throw new RuntimeException("Could not create directory");
                    }
                    unpackedFiles.add(outputFile);
                }

            } else if (filename.toLowerCase().endsWith(".gz")) {
                // RECURSIVE CALL
                final File outputFile = new File(outputDir, filename);
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, outputDir);
                outputFile.delete();
            } else if (filename.toLowerCase().endsWith(".tar")) {
                // RECURSIVE CALL
                final File outputFile = new File(outputDir, filename);
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                untar(outputFile, outputDir);
                outputFile.delete();
            } else {
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                unpackedFiles.add(outputFile);
            }
        }
        tarInputStream.close();
        return unpackedFiles;
    }

    public static boolean gunzip(File src, File tgtPath) {
        System.out.println(String.format("UnGzipping gz file %s path %s.", src.getAbsoluteFile(),
                tgtPath.getAbsolutePath()));
        try {
            GzipCompressorInputStream oSRC = new GzipCompressorInputStream(new FileInputStream(src));
            File tgt = new File(tgtPath.getAbsolutePath() + "/"
                    + src.getName().substring(0, src.getName().lastIndexOf(".")));
            FileOutputStream oTGT = new FileOutputStream(tgt);
            byte[] buffer = new byte[8192];
            int iBytesRead = 0;
            while ((iBytesRead = oSRC.read(buffer)) != -1) {
                oTGT.write(buffer, 0, iBytesRead);
            }
            oTGT.close();
            oSRC.close();

            if (tgt.getName().endsWith(".tar")) {
                File destDir = new File(tgt.getPath().replace(".tar", ""));
                if(!destDir.exists()) {
                    destDir.mkdirs();
                }
                untar(tgt, destDir);
                tgt.delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
