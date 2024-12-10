package org.palladiosimulator.analyzer.slingshot.injection.utility.plotter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

/**
 *
 * Create CSV files in the tmp directory and also write (x,y) value pairs to the files.
 *
 * @author Sophie Stieß
 *
 */
public final class CSVCreator {

    private final static Logger LOGGER = Logger.getLogger(CSVCreator.class);

    /**
     * A dataPoint to be written to the CSV.
     */
    public record DataPoint(double x, double y) {
    }

    private final File file;

    /**
     * Create a creator for writing to a file with the specified name in the tmp directory.
     *
     * @param filename
     *            name of file to be created. must include file ending.
     */
    public CSVCreator(final String filename) {
        this(System.getProperty("java.io.tmpdir"), Optional.empty(), filename);
    }

    /**
     * Create a creator for writing to a file with the specified name in a directory with the
     * specified name in the tmp directory.
     *
     * @param directory
     *            name of the directory, only created if it does not yet exist.
     * @param filename
     *            name of file to be created. must include file ending.
     */
    public CSVCreator(final String directory, final String filename) {
        this(System.getProperty("java.io.tmpdir"), Optional.of(directory), filename);
    }

    /**
     *
     * Create a creator for writing to a file with the specified name in a directory with the
     * specified name in the specified path.
     *
     * @param path
     *            base path to create directory and file at.
     * @param directory
     *            name of the directory, only created if it does not yet exist.
     * @param filename
     *            name of file to be created. must include file ending.
     */
    public CSVCreator(final String path, final Optional<String> directory, final String filename) {
        super();
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(filename);
        Preconditions.checkNotNull(directory);
        Preconditions.checkArgument(!filename.isBlank(), "TODO");
        Preconditions.checkArgument(!path.isBlank(), "TODO");

        final String cleanPath = path.endsWith(File.separator) ? path : path + File.separator;

        if (directory.isEmpty()) {
            this.file = new File(cleanPath + filename);
        } else {

            final File directoryFile = new File(cleanPath + directory.get());
            directoryFile.mkdirs();

            this.file = new File(directoryFile.getAbsolutePath() + File.separator + filename);
        }
        LOGGER.error(String.format("Save data to %s", this.file.getAbsolutePath()));

    }

    public void testwrite() {
        try (Writer writer = new BufferedWriter(new FileWriter(this.file))) {

            writer.append("hello")
                .append(" ")
                .append("world");

            writer.flush();

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void write(final List<DataPoint> data, final String header1, final String header2) {
        try (Writer writer = new BufferedWriter(new FileWriter(this.file))) {

            writer.append(header1)
                .append(";")
                .append(header2)
                .append("\n");

            for (final DataPoint datapoint : data) {
                writer.append(Double.toString(datapoint.x()))
                    .append(";")
                    .append(Double.toString(datapoint.y()))
                    .append("\n");
            }

            writer.flush();

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(final String[] args) {
//
//        final String filename = "foo.csv";
//
//        final CSVCreator creator = new CSVCreator("bar", filename);
//        creator.testwrite();
//
//    }
}
