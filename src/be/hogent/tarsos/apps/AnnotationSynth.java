package be.hogent.tarsos.apps;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import be.hogent.tarsos.midi.ToneSequenceBuilder;
import be.hogent.tarsos.midi.ToneSequenceBuilder.CSVFileHandler;
import be.hogent.tarsos.util.FileUtils;




/**
 * @author jsix666
 * Generates audio from a set of annotations.
 * The
 *
 */
public class AnnotationSynth {

	public static void main(String[] args) throws IOException
	{

		LongOpt[] longopts = new LongOpt[6];
		longopts[0] = new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'o');
		longopts[1] = new LongOpt("filter", LongOpt.REQUIRED_ARGUMENT, null, 'f');
		longopts[2] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[3] = new LongOpt("format", LongOpt.REQUIRED_ARGUMENT, null, 'r');
		longopts[4] = new LongOpt("listen", LongOpt.NO_ARGUMENT, null, 'l');
		longopts[5] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		Getopt g = new Getopt("AnnotationSynth", args, "-o:f:i:h", longopts);

		String outputFile = "out.wav";
		String inputFile = null;
		int medianFilterWindowSize = 0;
		String format = "IPEM";
		int c;
		while ((c = g.getopt()) != -1)
		{
			String arg = g.getOptarg();
			switch(c)
			{
			case 'i':
				inputFile = arg;
				break;
			case 'o':
				outputFile = arg;
				break;
			case 'r':
				format = arg.toUpperCase();
				break;
			case 'l':
				outputFile = null;
				break;
			case 'm':
				medianFilterWindowSize = Integer.parseInt(arg);
				break;
			case 'h':
				System.out.println("");
				System.out.println("AnnotationSynth is used to sonificate pitch annotation files. For the moment it" +
						" uderstands the pitch files used by BOZKURT, AUBIO and IPEM. It reads the data from " +
						"a file or from STDIN.");
				System.out.println("");
				System.out.println("-----------------------");
				System.out.println("");
				System.out.println("java -jar annotationsynth.jar [--out out.wav] [--filter 5] [--in bla.csv] [--format AUBIO|IPEM|BOZKURT] [--listen]");
				System.out.println("");
				System.out.println("-----------------------");
				System.out.println("");
				System.out.println("--out file.wav\t\tDefine the output file, default is out.wav");
				System.out.println("--filter integer\tDefines the number of samples are\n\t\t\tused in a median filter. 0 is the default. With samples\n\t\t\tevery 10ms and a median filter of 5 there can be\n\t\t\ta 50/2ms delay.");
				System.out.println("--in file.txt\t\tInput file. By default it reads standard input.");
				System.out.println("--format FORMAT\t\tThe format of the input file: is it a \n\t\t\tfile generated by AUBIO, BOZKURT or IPEM?");
				System.out.println("--listen\t\tDo not write a wav file but listen to the generated tones.");
				System.out.println("");
				System.out.println("-----------------------");
				System.out.println("");
				System.out.println("Example: listen to an ipem file.");
				System.out.println("");
				System.out.println("java -jar annotationsynth.jar --listen --format IPEM --in song.txt");
				System.out.println("");
				System.out.println("-----------------------");
				System.out.println("");
				System.out.println("Bugs: currently gain information is only used while listening, not while writing a file.");
				System.out.println("");
				System.exit(0);
				return;
			}
		}

		CSVFileHandler handler = null;
		if(format.equals("IPEM"))
			handler = ToneSequenceBuilder.IPEM_CSVFILEHANDLER;
		else
			handler = format.equals("BOZKURT") ? ToneSequenceBuilder.BOZKURT_CSVFILEHANDLER : ToneSequenceBuilder.AUBIO_CSVFILEHANDLER;

		ToneSequenceBuilder builder = new ToneSequenceBuilder();
		String separator = handler.getSeparator();
		if(inputFile == null){
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String s;
			while ((s = in.readLine()) != null && s.length() != 0){
				String[] row = s.split(separator);
				handler.handleRow(builder, row);
			}
		}else{
			List<String[]> rows = FileUtils.readCSVFile(inputFile,separator,-1);
			for(String[] row : rows){
				handler.handleRow(builder, row);
			}
		}

		try {
			builder.writeFile(outputFile,medianFilterWindowSize);
		} catch (Exception e) {
			System.out.println("Could not write: " + outputFile + "\n");
			e.printStackTrace();
		}
		System.exit(0);
	}
}