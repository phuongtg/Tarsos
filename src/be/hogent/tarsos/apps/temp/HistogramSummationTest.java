package be.hogent.tarsos.apps.temp;

import java.util.List;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.pure.TarsosPitchDetection;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public final class HistogramSummationTest {

    /**
     * Tests if summing histograms constructed with different pitch extractors
     * is useful.
     */
    public static void main(final String[] args) {
        final AudioFile audioFile = new AudioFile("audio\\maghreb\\4_ABERDAG___LA_DANSE.wav");
        PitchDetector pitchDetector = new TarsosPitchDetection(audioFile, PitchDetectionMode.TARSOS_YIN);
        pitchDetector.executePitchDetection();
        List<Sample> samples = pitchDetector.getSamples();
        AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
        final ToneScaleHistogram toneScaleHistogramTarsosYin = ambitusHistogram.toneScaleHistogram();

        pitchDetector = new AubioPitchDetection(audioFile, PitchDetectionMode.AUBIO_YIN);
        pitchDetector.executePitchDetection();
        samples = pitchDetector.getSamples();
        ambitusHistogram = Sample.ambitusHistogram(samples);
        final ToneScaleHistogram toneScaleHistogramAbioYin = ambitusHistogram.toneScaleHistogram();

        pitchDetector = new IPEMPitchDetection(audioFile, PitchDetectionMode.IPEM_SIX);
        pitchDetector.executePitchDetection();
        samples = pitchDetector.getSamples();
        ambitusHistogram = Sample.ambitusHistogram(samples);
        final ToneScaleHistogram toneScaleHistogramIPEM = ambitusHistogram.toneScaleHistogram();

        toneScaleHistogramTarsosYin.normalize();
        toneScaleHistogramAbioYin.normalize();
        toneScaleHistogramIPEM.normalize();

        toneScaleHistogramTarsosYin.plot("data/tests/tarsos_yin.png", "tarsos_yin");
        toneScaleHistogramAbioYin.plot("data/tests/audbio_yin.png", "aubio_yin");
        toneScaleHistogramIPEM.plot("data/tests/ipem.png", "ipem");

        toneScaleHistogramTarsosYin.add(toneScaleHistogramAbioYin).add(toneScaleHistogramIPEM).normalize()
        .gaussianSmooth(1.0).plot("data/tests/added.png", "Aubio + Tarsos + Ipem");
    }
}
