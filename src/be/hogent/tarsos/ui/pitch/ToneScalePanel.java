/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

/**
 * @author Joren Six
 */
public final class ToneScalePanel extends JPanel implements AudioFileChangedListener, ScaleChangedListener,
		AnnotationListener {

	public static final int X_BORDER = 5; // pixels
	public static final int Y_BORDER = 5; // pixels

	/**
     */
	private static final long serialVersionUID = 5493280409705136547L;
	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.ambitus_stop);
	//private static final int AMBITUS_START = Configuration.getInt(ConfKey.ambitus_start);

	private final HashMap<PitchDetectionMode, Histogram> histos;
	private final List<Layer> layers;
	private final ScalaLayer scalaLayer;
	private final ScaleChangedListener scaleChangedPublisher;
	private final double stop;
	private AudioFile audioFile;
	/**
	 * Hehe, feces.
	 */
	private final JTabbedPane layerUserInterfeces;

	public ToneScalePanel(final Histogram histogram, final ScaleChangedListener scaleChangedPublisher) {
		super(new BorderLayout());
		stop = histogram.getStop();
		setSize(640, 480);
		histos = new HashMap<PitchDetectionMode, Histogram>();
		this.scaleChangedPublisher = scaleChangedPublisher;
		layers = new ArrayList<Layer>();
		scalaLayer = new ScalaLayer(this, ScalaFile.westernTuning().getPitches(), histogram.getStop()
				- histogram.getStart(), scaleChangedPublisher);
		layers.add(scalaLayer);

		layerUserInterfeces = new JTabbedPane();
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		audioFile = newAudioFile;
		for (Layer layer : layers) {
			if (layer instanceof HistogramLayer) {
				((HistogramLayer) layer).audioFileChanged(newAudioFile);
			}
		}

		for (Histogram histogram : histos.values()) {
			histogram.clear();
		}
	}

	@Override
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		for (final Layer layer : layers) {
			layer.draw(graphics);
		}
	}

	public List<Layer> getLayers() {
		return layers;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		this.scalaLayer.scaleChanged(newScale, isChanging);
		for (Layer layer : layers) {
			if (layer instanceof HistogramLayer) {
				HistogramLayer histoLayer = (HistogramLayer) layer;
				histoLayer.scaleChanged(newScale, isChanging);
				this.scalaLayer.setXOffset(histoLayer.getXOffset());
			}
		}
	}

	double[] histoValues;

	public Component controls() {
		return layerUserInterfeces;
	}

	public void addAnnotation(Annotation annotation) {
		double pitchInAbsCents = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > 0 && pitchInAbsCents <= AMBITUS_STOP) {
			final Histogram histo;
			if (!histos.containsKey(annotation.getSource())) {
				if (stop > 1200) {
					histo = new AmbitusHistogram();
				} else {
					histo = new ToneScaleHistogram();
				}
				histos.put(annotation.getSource(), histo);
				Color color = Tarsos.COLORS[annotation.getSource().ordinal() % Tarsos.COLORS.length];
				HistogramLayer layer = new HistogramLayer(this, histo, scaleChangedPublisher, color);
				// KDELayer kdeLayer = new KDELayer(this, delta);
				// histoValues = kdeLayer.getValues();
				layer.audioFileChanged(audioFile);
				layers.add(layer);
				// layers.add(kdeLayer);
				layerUserInterfeces.addTab(annotation.getSource().name(), layer.ui());
			} else {
				histo = histos.get(annotation.getSource());
			}

			histo.add(pitchInAbsCents);

			// ToneScaleHistogram.addAnnotationTo(histoValues, annotation, 5,
			// stop > 1200 ? PitchUnit.ABSOLUTE_CENTS :
			// PitchUnit.RELATIVE_CENTS);
		}
	}

	public void clearAnnotations() {
		for (Histogram histogram : histos.values()) {
			histogram.clear();
		}

		if (histoValues != null) {
			for (int i = 0; i < histoValues.length; i++) {
				histoValues[i] = 0;
			}
		}
	}

	public void extractionStarted() {
		// NO OP
	}

	public void extractionFinished() {
		// NO OP
	}

	public void annotationsAdded() {
		repaint();
	}
}
