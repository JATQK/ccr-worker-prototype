package de.leipzig.htwk.gitrdf.worker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.*;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZipUtils {

    public static File writeInputStreamZipToTempFile(InputStream inputStream, int bufferLengthInBytes) throws IOException {

        File tempZipDirectory = Files.createTempFile("tempZipFile", "zip").toFile();

        try (OutputStream tempZipDirectoryOutputStream = new BufferedOutputStream(new FileOutputStream(tempZipDirectory))) {

            byte[] buffer = new byte[bufferLengthInBytes];

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                tempZipDirectoryOutputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempZipDirectory;

    }

    public static File extractZip(Blob blob) throws SQLException, IOException {
        return extractZip(blob.getBinaryStream());
    }

    public static File extractZip(InputStream inputStream) throws  IOException {
        return extractZip(inputStream, 1024);
    }

    public static File extractZip(InputStream inputStream, int byteBufferSize) throws  IOException {

        File gitRepositoryTempDirectory = Files.createTempDirectory("GitRepositoryTempDirectory").toFile();

        byte[] buffer = new byte[byteBufferSize];

        try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {

            ZipEntry zipEntry = zipStream.getNextEntry();

            while (zipEntry != null) {

                File newFile = newFile(gitRepositoryTempDirectory, zipEntry);

                if (zipEntry.isDirectory()) {

                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }

                } else {

                    File parent = newFile.getParentFile();

                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent);
                    }

                    // write file content
                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {

                        int len;

                        while ((len = zipStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, len);
                        }
                    }

                }

                zipEntry = zipStream.getNextEntry();

            }

            zipStream.closeEntry();
        }

        return gitRepositoryTempDirectory;

    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {

        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        // Protect against a ZIP-Slip
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target directory: " + zipEntry.getName());
        }

        return destFile;
    }

}
