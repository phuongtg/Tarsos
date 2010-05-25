package be.hogent.tarsos.apps;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * @author Joren Six
 */
public final class AudioToScala extends AbstractTarsosApp {

    @Override
    public String description() {
        return "Detects a tone scale from an audio file and exports it as a scala file.";
    }

    @Override
    public String name() {
        return "audio_to_scala";
    }

    @Override
    public void run(final String... args) {
        final OptionParser parser = new OptionParser();
        final OptionSpec<File> fileSpec = parser.accepts("in", "The file to annotate.").withRequiredArg()
        .ofType(
                File.class)
                .withValuesSeparatedBy(' ');
        final OptionSpec<File> scalaSpec = parser.accepts("scala",
        "The output scala file. (default: 'name of the input file'.scl)")
        .withRequiredArg().ofType(
                File.class)
                .withValuesSeparatedBy(' ');

        final OptionSpec<PitchDetectionMode> detectionModeSpec = parser.accepts("detector",
        "The detector to use.")
        .withRequiredArg().ofType(PitchDetectionMode.class)
        .defaultsTo(PitchDetectionMode.TARSOS_YIN);

        final OptionSet options = parse(args, parser, this);

        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
            final File inputFile = options.valueOf(fileSpec);
            final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);
            File scalaFile;
            if (options.valueOf(scalaSpec) == null) {
                scalaFile = new File(FileUtils.basename(inputFile.getAbsolutePath()) + ".scl");
            } else {
                scalaFile = options.valueOf(scalaSpec);
            }
            exportScalaFile(scalaFile, inputFile, detectionMode);
        }
    }

    /**
     * Export a Scala file using a detector and an input file.
     * @param scalaFile
     *            The location to export to.
     * @param inputFile
     *            The audio input file.
     * @param detectionMode
     *            The detection mode.
     */
    private void exportScalaFile(final File scalaFile, final File inputFile,
            final PitchDetectionMode detectionMode) {
        final AudioFile audioFile = new AudioFile(inputFile.getAbsolutePath());
        final PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);
        pitchDetector.executePitchDetection();
        final List<Sample> samples = pitchDetector.getSamples();
        final AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
        final ToneScaleHistogram scaleHistogram = ambitusHistogram.toneScaleHistogram();
        scaleHistogram.gaussianSmooth(1.0);
        final List<Peak> peaks = PeakDetector.detect(scaleHistogram, 20, 0.5);
        ToneScaleHistogram.exportPeaksToScalaFileFormat(scalaFile.getAbsolutePath(), FileUtils
                .basename(inputFile.getAbsolutePath()), peaks);
    }
}