package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JComponent;

public class UniversalVirtualKeyboard extends JComponent implements
		VirtualKeyboard {

	private static final long serialVersionUID = -3017076399911747736L;

	private final int numberOfKeysPerOctave;
	private Receiver recveiver = null;
	// only one midi note can be pressed using the mouse
	private int currentlyPressedMidiNote;
	
	private int lowestAssignedKey;
	
	private final int numberOfKeys;

	private final boolean[] keyDown;
	
	public UniversalVirtualKeyboard(int numberOfKeysPerOctave) {
		this(numberOfKeysPerOctave,numberOfKeysPerOctave*7 > 128 ? 128 : numberOfKeysPerOctave*7 );
	}

	public UniversalVirtualKeyboard(int numberOfKeysPerOctave,int numberOfKeys) {
		super();
		setFocusable(true);
		
		this.numberOfKeys = numberOfKeys;
		this.numberOfKeysPerOctave = numberOfKeysPerOctave;
		this.currentlyPressedMidiNote = -1;
		lowestAssignedKey = 3 * numberOfKeysPerOctave;
		
		keyDown = new boolean[numberOfKeys];

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				grabFocus();
				Point p = e.getPoint();
				currentlyPressedMidiNote = getMidiNote(p.x, p.y);
				sendNoteMessage(currentlyPressedMidiNote,true);
			}

			public void mouseReleased(MouseEvent e) {
				sendNoteMessage(currentlyPressedMidiNote,false);
				currentlyPressedMidiNote = -1;
			}
		});

		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				repaint();
			}
			
			public void focusLost(FocusEvent e) {
				repaint();
			}
		});
		
		addKeyListener(new KeyListener()
        {
            public void keyPressed(KeyEvent e) {
                int pressedKeyChar = e.getKeyChar();
                for (int i = 0; i < VirtualKeyboard.mappedKeys.length(); i++) {
                    if(VirtualKeyboard.mappedKeys.charAt(i) == pressedKeyChar){
                    	int midiKey = i+lowestAssignedKey;
                    	if(midiKey < UniversalVirtualKeyboard.this.numberOfKeys)
                        if(!keyDown[midiKey]){
                        	sendNoteMessage(midiKey,true);
                        }                        
                        return;
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
            	char pressedKeyChar = e.getKeyChar();
                for (int i = 0; i < VirtualKeyboard.mappedKeys.length(); i++) {
                	if(VirtualKeyboard.mappedKeys.charAt(i) == pressedKeyChar){
                		int midiKey = i+lowestAssignedKey;
                        if(keyDown[midiKey])
                        	sendNoteMessage(midiKey,false);
                        return;
                    }
                }                
            }

            public void keyTyped(KeyEvent e) {                
                if(e.getKeyChar() == '-'){     
                	lowestAssignedKey -= UniversalVirtualKeyboard.this.numberOfKeysPerOctave;
                    if(lowestAssignedKey < 0) lowestAssignedKey = 0;
                    repaint();
                }
                if(e.getKeyChar() == '+'){
                    lowestAssignedKey += UniversalVirtualKeyboard.this.numberOfKeysPerOctave;
                    if(lowestAssignedKey > 127) 
                    	lowestAssignedKey -= UniversalVirtualKeyboard.this.numberOfKeysPerOctave;
                    repaint();
                }
            }            
        });
	}
	
	private void sendNoteMessage(int midiKey, boolean sendOnMessage){
		if(midiKey > numberOfKeys)
			throw new Error("Requested invalid midi key: " + midiKey);
		
		//do not send note on messages to pressed keys
		if(sendOnMessage && keyDown[midiKey])
			return;
		//do not send note off messages to keys that are not down
		if(!sendOnMessage && !keyDown[midiKey])
			return;
		
        try {
        	ShortMessage sm = new ShortMessage();
        	int command = sendOnMessage ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF;
        	int velocity = sendOnMessage ? VirtualKeyboard.VELOCITY : 0;
            sm.setMessage(command, VirtualKeyboard.CHANNEL,midiKey, velocity);
            if (recveiver != null)
				recveiver.send(sm, -1);
            send(sm, -1);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }                               
        //mark key correctly
        keyDown[midiKey] = sendOnMessage;
	}

	public int getMidiNote(int x, int y) {
		int w = getWidth();
		float nw = w / (float) numberOfKeys;
		int wn = (int) (x / nw);
		int oct = wn / numberOfKeysPerOctave;
		int n = oct * numberOfKeysPerOctave + wn % numberOfKeysPerOctave;
		if (n < 0)
			n = 0;
		if (n > numberOfKeys - 1)
			n = numberOfKeys - 1;
		return n;
	}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		int w = getWidth();
		int h = getHeight();

		float nw = w / (float) numberOfKeys;
		float cx = 0;
		Rectangle2D rect = new Rectangle2D.Double();
		for (int i = 0; i < numberOfKeys; i++) {

			rect.setRect(cx, 0, nw, h);
			if (keyDown[i])
				g2.setColor(new Color(0.8f, 0.8f, 0.95f));
			else
				g2.setColor(Color.WHITE);
			g2.fill(rect);
			g2.setColor(Color.BLACK);
			g2.draw(rect);

			if (i % this.numberOfKeysPerOctave == 0)
				g2.drawString("_", cx + 2, 12);
			
			if(i >= lowestAssignedKey)
            {
                if(i - lowestAssignedKey < VirtualKeyboard.mappedKeys.length())
                {
                    g2.setColor(Color.GRAY);
                    char keyChar = VirtualKeyboard.mappedKeys.charAt(i - lowestAssignedKey);
                    g2.drawString("" + keyChar, cx + 2, h - 4);
                }
            }

			cx += nw;
		}
	}

	@Override
	public void setReceiver(Receiver receiver) {
		this.recveiver = receiver;
	}

	@Override
	public void close() {

	}

	@Override
	public Receiver getReceiver() {
		return this.recveiver;
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {

		if (message instanceof ShortMessage) {
			ShortMessage sm = (ShortMessage) message;
			boolean correctChannel = sm.getChannel() == VirtualKeyboard.CHANNEL;
			boolean noteOnOrOff = sm.getCommand() == ShortMessage.NOTE_ON
					|| sm.getCommand() == ShortMessage.NOTE_OFF;
			if (correctChannel && noteOnOrOff) {
				keyDown[sm.getData1()] = (sm.getCommand() == ShortMessage.NOTE_ON)
						&& (sm.getData2() != 0);
				repaint();
			}
		}
	}

}
