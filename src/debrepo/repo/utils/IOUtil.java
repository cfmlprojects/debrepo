package debrepo.repo.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public final class IOUtil {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private IOUtil() {
    }

    public static void copy(final InputStream input, final OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final InputStream input, final OutputStream output, final int bufferSize)
            throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    public static void copy(final Reader input, final Writer output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final Reader input, final Writer output, final int bufferSize) throws IOException {
        final char[] buffer = new char[bufferSize];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    public static void copy(final InputStream input, final Writer output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final InputStream input, final Writer output, final int bufferSize) throws IOException {
        final InputStreamReader in = new InputStreamReader(input);
        copy(in, output, bufferSize);
    }

    public static void copy(final InputStream input, final Writer output, final String encoding) throws IOException {
        final InputStreamReader in = new InputStreamReader(input, encoding);
        copy(in, output);
    }

    public static void copy(final InputStream input, final Writer output, final String encoding, final int bufferSize)
            throws IOException {
        final InputStreamReader in = new InputStreamReader(input, encoding);
        copy(in, output, bufferSize);
    }

    public static String toString(final InputStream input) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final InputStream input, final int bufferSize) throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw, bufferSize);
        return sw.toString();
    }

    public static String toString(final InputStream input, final String encoding) throws IOException {
        return toString(input, encoding, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final InputStream input, final String encoding, final int bufferSize)
            throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw, encoding, bufferSize);
        return sw.toString();
    }

    public static byte[] toByteArray(final InputStream input) throws IOException {
        return toByteArray(input, DEFAULT_BUFFER_SIZE);
    }

    public static byte[] toByteArray(final InputStream input, final int bufferSize) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output, bufferSize);
        return output.toByteArray();
    }

    public static void copy(final Reader input, final OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final Reader input, final OutputStream output, final int bufferSize) throws IOException {
        final OutputStreamWriter out = new OutputStreamWriter(output);
        copy(input, out, bufferSize);
        out.flush();
    }

    public static String toString(final Reader input) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final Reader input, final int bufferSize) throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw, bufferSize);
        return sw.toString();
    }

    public static byte[] toByteArray(final Reader input) throws IOException {
        return toByteArray(input, DEFAULT_BUFFER_SIZE);
    }

    public static byte[] toByteArray(final Reader input, final int bufferSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output, bufferSize);
        return output.toByteArray();
    }

    public static void copy(final String input, final OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final String input, final OutputStream output, final int bufferSize) throws IOException {
        final StringReader in = new StringReader(input);
        final OutputStreamWriter out = new OutputStreamWriter(output);
        copy(in, out, bufferSize);
        out.flush();
    }

    public static void copy(final String input, final Writer output) throws IOException {
        output.write(input);
    }

    public static void bufferedCopy(final InputStream input, final OutputStream output) throws IOException {
        final BufferedInputStream in = new BufferedInputStream(input);
        final BufferedOutputStream out = new BufferedOutputStream(output);
        copy(in, out);
        out.flush();
    }

    public static byte[] toByteArray(final String input) throws IOException {
        return toByteArray(input, DEFAULT_BUFFER_SIZE);
    }

    public static byte[] toByteArray(final String input, final int bufferSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output, bufferSize);
        return output.toByteArray();
    }

    public static void copy(final byte[] input, final Writer output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final byte[] input, final Writer output, final int bufferSize) throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(input);
        copy(in, output, bufferSize);
    }

    public static void copy(final byte[] input, final Writer output, final String encoding) throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(input);
        copy(in, output, encoding);
    }

    public static void copy(final byte[] input, final Writer output, final String encoding, final int bufferSize)
            throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(input);
        copy(in, output, encoding, bufferSize);
    }

    public static String toString(final byte[] input) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final byte[] input, final int bufferSize) throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw, bufferSize);
        return sw.toString();
    }

    public static String toString(final byte[] input, final String encoding) throws IOException {
        return toString(input, encoding, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final byte[] input, final String encoding, final int bufferSize) throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw, encoding, bufferSize);
        return sw.toString();
    }

    public static void copy(final byte[] input, final OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static void copy(final byte[] input, final OutputStream output, final int bufferSize) throws IOException {
        output.write(input);
    }

    public static void fileWrite(String fileName, String data) throws IOException {
        fileWrite(fileName, null, data);
    }

    public static void fileWrite(String fileName, String encoding, String data) throws IOException {
        File file = (fileName == null) ? null : new File(fileName);
        fileWrite(file, encoding, data);
    }

    public static void fileWrite(File file, String data) throws IOException {
        fileWrite(file, null, data);
    }

    public static void fileWrite(File file, String encoding, String data) throws IOException {
        Writer writer = null;
        try {
            OutputStream out = new FileOutputStream(file);
            if (encoding != null) {
                writer = new OutputStreamWriter(out, encoding);
            } else {
                writer = new OutputStreamWriter(out);
            }
            writer.write(data);
        } finally {
            writer.close();
        }
    }

    public static void copyFileToDirectory(final File source, final File destinationDirectory) throws IOException {
        if (destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
            throw new IllegalArgumentException("Destination is not a directory");
        }

        copyFile(source, new File(destinationDirectory, source.getName()));
    }

    public static void copyFile(final File source, final File destination) throws IOException {
        // check source exists
        if (!source.exists()) {
            final String message = "File " + source + " does not exist";
            throw new IOException(message);
        }

        // check source != destination, see PLXUTILS-10
        if (source.getCanonicalPath().equals(destination.getCanonicalPath())) {
            // if they are equal, we can exit the method without doing any work
            return;
        }

        copyStreamToFile(new FileInputStream(source), destination);

        if (source.length() != destination.length()) {
            final String message = "Failed to copy full contents from " + source + " to " + destination;
            throw new IOException(message);
        }
    }

    public static void copyStreamToFile(final InputStream source, final File destination) throws IOException {
        // does destination directory exist ?
        if (destination.getParentFile() != null && !destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }

        // make sure we can write to destination
        if (destination.exists() && !destination.canWrite()) {
            final String message = "Unable to open file " + destination + " for writing.";
            throw new IOException(message);
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destination);
            IOUtil.copy(source, output);
        } finally {
            IOUtil.close(source);
            IOUtil.close(output);
        }
    }

    public static class ExtFilter implements FilenameFilter {
        private String ext;

        public ExtFilter(String extension) {
            ext = extension;
        }

        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(ext);
        }
    }

    public static boolean contentEquals(final InputStream input1, final InputStream input2) throws IOException {
        final InputStream bufferedInput1 = new BufferedInputStream(input1);
        final InputStream bufferedInput2 = new BufferedInputStream(input2);

        int ch = bufferedInput1.read();
        while (-1 != ch) {
            final int ch2 = bufferedInput2.read();
            if (ch != ch2) {
                return false;
            }
            ch = bufferedInput1.read();
        }

        final int ch2 = bufferedInput2.read();
        if (-1 != ch2) {
            return false;
        } else {
            return true;
        }
    }

    public static void close(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    public static void close(OutputStream outputStream) {
        if (outputStream == null) {
            return;
        }

        try {
            outputStream.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    public static void close(Reader reader) {
        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    public static void close(Writer writer) {
        if (writer == null) {
            return;
        }

        try {
            writer.close();
        } catch (IOException ex) {
            // ignore
        }
    }
}