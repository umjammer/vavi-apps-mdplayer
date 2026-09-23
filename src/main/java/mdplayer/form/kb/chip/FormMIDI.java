package mdplayer.form.kb.chip;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.prefs.Preferences;

import mdplayer.Common;
import mdplayer.form.FrameBuffer;
import mdplayer.MIDIParam;
import mdplayer.form.ScreenPanel;
import mdplayer.Tables;
import mdplayer.chips.MidiPlugin;
import mdplayer.form.FormBase;
import mdplayer.form.SettingTab;
import mdplayer.form.kb.ViewProvider;
import mdplayer.form.sys.FormMain;
import mdplayer.form.View;
import mdplayer.form.sys.setting.SettingMIDIExpPanel;
import mdplayer.form.sys.setting.SettingMIDIOut2Panel;
import mdplayer.form.sys.setting.SettingMIDIOutPanel;


public class FormMIDI extends FormBase implements View {

    private boolean isClosed = false;
    private int x = -1;
    private int y = -1;

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void setDefaultLocation(int x, int y) {
        this.x = x;
        this.y = y;
        // there at once, not only once opened: a window closed before its windowOpened ran
        // would otherwise record the platform's default spot as where it was
        setLocation(x, y);
    }
    private int frameSizeW = 0;
    private int frameSizeH = 0;
    private final int chipId;
    private final int zoom;

    private final MIDIParam newParam = new MIDIParam();
    private final MIDIParam oldParam = new MIDIParam();
    private final FrameBuffer frameBuffer = new FrameBuffer();
    private String notes = "";

    private static final Preferences prefs = Preferences.userNodeForPackage(FormMIDI.class);

    public FormMIDI(FormMain frm, int chipId, int zoom) {
        super(frm);
        this.chipId = chipId;
        this.zoom = zoom;

        initializeComponent();

        frameBuffer.add(pbScreen, Common.getImage("planeMIDI_GM"), null, zoom);
        screenInitMIDI(frameBuffer);
        update();
    }

    @Override
    public void update() {
        frameBuffer.refresh(null);
    }

    protected boolean getShowWithoutActivation() {
        return true;
    }

    private final WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            parent.setting.getLocation().setPos("MIDI", chipId, getLocation());
            isClosed = true;
        }

        @Override
        public void windowOpened(WindowEvent e) {
            setLocation(new Point(x, y));

            frameSizeW = getWidth() - getSize().width;
            frameSizeH = getHeight() - getSize().height;

            changeZoom();
        }
    };

    private void changeZoom() {
        this.setMaximumSize(new Dimension(frameSizeW + Common.getImage("planeMIDI_GM").getWidth() * zoom, frameSizeH + Common.getImage("planeMIDI_GM").getHeight() * zoom));
        this.setMinimumSize(new Dimension(frameSizeW + Common.getImage("planeMIDI_GM").getWidth() * zoom, frameSizeH + Common.getImage("planeMIDI_GM").getHeight() * zoom));
        this.setPreferredSize(new Dimension(frameSizeW + Common.getImage("planeMIDI_GM").getWidth() * zoom, frameSizeH + Common.getImage("planeMIDI_GM").getHeight() * zoom));
        componentListener.componentResized(null);
    }

    private final ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            prefs.putInt("x", e.getComponent().getX());
            prefs.putInt("y", e.getComponent().getY());
        }

        @Override
        public void componentResized(ComponentEvent e) {
        }
    };

    @Override
    public void changeScreenParams() {
        MIDIParam prm = audio.plugin.chipRegister.plugin(MidiPlugin.class).get(chipId);

        for (int ch = 0; ch < 16; ch++) {
            System.arraycopy(prm.cc[ch], 0, newParam.cc[ch], 0, 256);
            newParam.bend[ch] = prm.bend[ch];

            System.arraycopy(prm.note[ch], 0, newParam.note[ch], 0, 128);

            newParam.level[ch][0] = prm.level[ch][0];
            newParam.level[ch][1] = prm.level[ch][1];
            newParam.level[ch][2] = prm.level[ch][2];
            newParam.level[ch][3] = prm.level[ch][3];
            newParam.level[ch][4] = prm.level[ch][4];
            if (prm.level[ch][0] > 0) {
                prm.level[ch][0] -= 3;
                if (prm.level[ch][0] < 0) prm.level[ch][0] = 0;
            }
            if (prm.level[ch][1] > 0) {
                prm.level[ch][1] -= 3;
                if (prm.level[ch][1] < 0) prm.level[ch][1] = 0;
            }
            if (prm.level[ch][2] > 0) {
                prm.level[ch][2] -= 3;
                if (prm.level[ch][2] < 0) prm.level[ch][2] = 0;
            }
            if (prm.level[ch][3] > 0) {
                prm.level[ch][4] -= 3;
                if (prm.level[ch][4] < 0) {
                    prm.level[ch][4] = 0;
                    prm.level[ch][3] -= 3;
                    if (prm.level[ch][3] < 0) prm.level[ch][3] = 0;
                }
            }

            newParam.pc[ch] = prm.pc[ch];

            newParam.nrpnVibRate[ch] = prm.nrpnVibRate[ch];
            newParam.nrpnVibDepth[ch] = prm.nrpnVibDepth[ch];
            newParam.nrpnVibDelay[ch] = prm.nrpnVibDelay[ch];

            newParam.nrpnLPF[ch] = prm.nrpnLPF[ch];
            newParam.nrpnLPFRsn[ch] = prm.nrpnLPFRsn[ch];
            newParam.nrpnHPF[ch] = prm.nrpnHPF[ch];

            newParam.nrpnEQBaseFrq[ch] = prm.nrpnEQBaseFrq[ch];
            newParam.nrpnEQBaseGain[ch] = prm.nrpnEQBaseGain[ch];
            newParam.nrpnEQTrebleFrq[ch] = prm.nrpnEQTrebleFrq[ch];
            newParam.nrpnEQTrebleGain[ch] = prm.nrpnEQTrebleGain[ch];

            newParam.nrpnEGAttack[ch] = prm.nrpnEGAttack[ch];
            newParam.nrpnEGDecay[ch] = prm.nrpnEGDecay[ch];
            newParam.nrpnEGRls[ch] = prm.nrpnEGRls[ch];
        }

        newParam.midiModule = prm.midiModule;

        // Display data
        System.arraycopy(prm.LCDDisplay, 0, newParam.LCDDisplay, 0, 64);
        newParam.lcdDisplayTime = prm.lcdDisplayTime;
        prm.lcdDisplayTime -= 3;
        if (prm.lcdDisplayTime < 0) prm.lcdDisplayTime = 0;

        System.arraycopy(prm.lcd8850Display, 0, newParam.lcd8850Display, 0, prm.lcd8850Display.length);
        newParam.lcd8850DisplayTime = prm.lcd8850DisplayTime;
        if (newParam.lcd8850DisplayTime != 400) prm.lcd8850DisplayTime -= 3;
        if (prm.lcd8850DisplayTime < 0) prm.lcd8850DisplayTime = 0;

        newParam.lcdDisplayTimeXG = prm.lcdDisplayTimeXG;
        prm.lcdDisplayTimeXG -= 3;
        if (prm.lcdDisplayTimeXG < 0) prm.lcdDisplayTimeXG = 0;

        // Display Letter data
        System.arraycopy(prm.lcdDisplayLetter, 0, newParam.lcdDisplayLetter, 0, 32);
        newParam.lcdDisplayLetterLen = prm.lcdDisplayLetterLen;
        newParam.lcdDisplayLetterTime = prm.lcdDisplayLetterTime;
        prm.lcdDisplayLetterTime -= 3;
        if (prm.lcdDisplayLetterTime < 0) {
            if (prm.lcdDisplayLetterLen > 0) {
                for (int i = 1; i < 32; i++) {
                    prm.lcdDisplayLetter[i - 1] = (byte) (i < prm.lcdDisplayLetterLen ? prm.lcdDisplayLetter[i] : 0x20);
                }
                prm.lcdDisplayLetterTime = 40;
                prm.lcdDisplayLetterLen--;
            } else {
                prm.lcdDisplayLetterTime = 0;
            }
        }
        newParam.lcdDisplayLetterTimeXG = prm.lcdDisplayLetterTimeXG;
        prm.lcdDisplayLetterTimeXG -= 3;
        if (prm.lcdDisplayLetterTimeXG < 0) prm.lcdDisplayLetterTimeXG = 0;

        newParam.reverbGS = prm.reverbGS;
        newParam.chorusGS = prm.chorusGS;
        newParam.delayGS = prm.delayGS;
        newParam.efxGS = prm.efxGS;

        newParam.reverbXG = prm.reverbXG;
        newParam.chorusXG = prm.chorusXG;
        newParam.variationXG = prm.variationXG;
        newParam.insertion1XG = prm.insertion1XG;
        newParam.insertion2XG = prm.insertion2XG;
        newParam.insertion3XG = prm.insertion3XG;
        newParam.insertion4XG = prm.insertion4XG;

        newParam.masterVolume = prm.masterVolume;

        newParam.lyric = prm.lyric;

    }

    @Override
    public void drawScreenParams() {
        int module = newParam.midiModule;

        if (oldParam.midiModule != newParam.midiModule) {
            frameBuffer.drawByteArray(0, 0, FrameBuffer.rPlane_MIDI[newParam.midiModule], 440, 0, 0, 440, 352);
            oldParam.midiModule = newParam.midiModule;
        }

        if (module == 1) {
            drawMIDI_MacroXG(frameBuffer, module, 0, 4 * 42, 16 + 33 * 8, oldParam.reverbXG, newParam.reverbXG);
            drawMIDI_MacroXG(frameBuffer, module, 1, 4 * 42, 32 + 33 * 8, oldParam.chorusXG, newParam.chorusXG);
            drawMIDI_MacroXG(frameBuffer, module, 2, 4 * 42, 48 + 33 * 8, oldParam.variationXG, newParam.variationXG);
            drawMIDI_MacroXG(frameBuffer, module, 2, 4 * 42, 64 + 33 * 8, oldParam.insertion1XG, newParam.insertion1XG);
            drawMIDI_MacroXG(frameBuffer, module, 2, 4 * 42, 80 + 33 * 8, oldParam.insertion2XG, newParam.insertion2XG);
            drawMIDI_MacroXG(frameBuffer, module, 2, 4 * 60, 32 + 33 * 8, oldParam.insertion3XG, newParam.insertion3XG);
            drawMIDI_MacroXG(frameBuffer, module, 2, 4 * 60, 48 + 33 * 8, oldParam.insertion4XG, newParam.insertion4XG);
        } else {
            drawMIDI_MacroGS(frameBuffer, module, 0, 4 * 42, 16 + 33 * 8, oldParam.reverbGS, newParam.reverbGS);
            drawMIDI_MacroGS(frameBuffer, module, 1, 4 * 42, 32 + 33 * 8, oldParam.chorusGS, newParam.chorusGS);
            drawMIDI_MacroGS(frameBuffer, module, 2, 4 * 42, 48 + 33 * 8, oldParam.delayGS, newParam.delayGS);
            drawMIDI_MacroGS(frameBuffer, module, 3, 4 * 42, 64 + 33 * 8, oldParam.efxGS, newParam.efxGS);
        }

        if (newParam.lcdDisplayLetterTime == 0 && newParam.lcdDisplayLetterTimeXG == 0) {
            drawMIDILCD_Letter(frameBuffer, module, 4, 277, oldParam.lcdDisplayLetter, 16);
        } else {
            drawMIDILCD_Letter(frameBuffer, module, 4, 277, oldParam.lcdDisplayLetter, newParam.lcdDisplayLetter, newParam.lcdDisplayLetterLen);
        }

        drawFont4IntMIDI(frameBuffer, 60 * 4, 17 * 16 + 8, 2 + module, oldParam.masterVolume, newParam.masterVolume);

        drawMIDI_Lyric(frameBuffer, chipId, 60 * 4, 41 * 8, oldParam.lyric, newParam.lyric);

        for (int ch = 0; ch < 16; ch++) {
            byte b = (byte) (128 - newParam.cc[ch][10] & 0xff);
            b = b < 0 ? 127 : b;
            oldParam.cc[ch][10] = drawMIDILCD_Fader(frameBuffer, module, 0, 64, ch * 16 + 16, oldParam.cc[ch][10], b); // Panpot
            oldParam.cc[ch][7] = drawMIDILCD_Fader(frameBuffer, module, 1, 68, ch * 16 + 16, oldParam.cc[ch][7], newParam.cc[ch][7]); // Volume
            oldParam.cc[ch][11] = drawMIDILCD_Fader(frameBuffer, module, 1, 72, ch * 16 + 16, oldParam.cc[ch][11], newParam.cc[ch][11]); // Expression
            oldParam.bend[ch] = drawMIDILCD_Fader(frameBuffer, module, 0, 76, ch * 16 + 16, oldParam.bend[ch], newParam.bend[ch]); // Pitch Bend
            oldParam.cc[ch][1] = drawMIDILCD_Fader(frameBuffer, module, 1, 80, ch * 16 + 16, oldParam.cc[ch][1], newParam.cc[ch][1]); // Modulation
            oldParam.cc[ch][91] = drawMIDILCD_Fader(frameBuffer, module, 1, 84, ch * 16 + 16, oldParam.cc[ch][91], newParam.cc[ch][91]); // Reverb
            oldParam.cc[ch][93] = drawMIDILCD_Fader(frameBuffer, module, 1, 88, ch * 16 + 16, oldParam.cc[ch][93], newParam.cc[ch][93]); // Chorus
            oldParam.cc[ch][94] = drawMIDILCD_Fader(frameBuffer, module, 1, 92, ch * 16 + 16, oldParam.cc[ch][94], newParam.cc[ch][94]); // Variation(Delay)
            oldParam.cc[ch][64] = drawMIDILCD_Fader(frameBuffer, module, 1, 96, ch * 16 + 16, oldParam.cc[ch][64], newParam.cc[ch][64]); // Hold(DumperPedal)
            oldParam.cc[ch][67] = drawMIDILCD_Fader(frameBuffer, module, 1, 100, ch * 16 + 16, oldParam.cc[ch][67], newParam.cc[ch][67]); // Soft
            oldParam.cc[ch][66] = drawMIDILCD_Fader(frameBuffer, module, 1, 104, ch * 16 + 16, oldParam.cc[ch][66], newParam.cc[ch][66]); // Sostenuto

            notes = "";
            for (int n = 0; n < 120; n++) {
                oldParam.note[ch][n] = drawMIDILCD_Kbd(frameBuffer, 108,
                        ch * 16 + 16, n, oldParam.note[ch][n], newParam.note[ch][n]);

                if (newParam.note[ch][n] > 0) {
                    notes = notes + "%s%d ".formatted(Tables.kbns[n % 12], n / 12);
                }
            }
            notes += "                           ";
            notes = notes.substring(0, 26);
            oldParam.notes[ch] = drawFont4MIDINotes(frameBuffer, 71 * 4, ch * 16 + 24, module + 2, oldParam.notes[ch], notes);

            oldParam.level[ch][0] = VolumeToMIDILCD(frameBuffer, module, 388, ch * 16 + 16, oldParam.level[ch][0], newParam.level[ch][0]);
            oldParam.level[ch][2] = VolumeToMIDILCD(frameBuffer, module, 388, ch * 16 + 24, oldParam.level[ch][2], newParam.level[ch][2]);

            // L1:
            if (newParam.lcdDisplayTime == 0 && newParam.lcd8850DisplayTime == 0 && newParam.lcdDisplayTimeXG == 0) {
                VolumeLCDToMIDILCD(frameBuffer,
                        module,
                        5 + ch * 10 + (ch > 3 ? 1 : 0) + (ch > 11 ? 1 : 0),
                        341,
                        oldParam.level[ch][1],
                        newParam.level[ch][1],
                        oldParam.level[ch][3],
                        newParam.level[ch][3]);
            } else {
                int s;
                for (int n = 0; n < 16; n++) {
                    oldParam.level[ch][1] = 256;
                    oldParam.level[ch][3] = 256;
                    oldParam.level[ch][4] = 256;
                }
                if (newParam.lcdDisplayTime > 0) {
                    // GS
                    for (int n = 0; n < 64; n++) {
                        s = newParam.LCDDisplay[n];
                        int x = n / 16;
                        int y = n % 16;
                        frameBuffer.drawByteArray(x * 50 + 5 + 0 + (x > 0 ? 1 : 0) + (x > 2 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x10) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        if (n > 47) continue;
                        frameBuffer.drawByteArray(x * 50 + 5 + 10 + (x > 0 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x08) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 50 + 5 + 20 + (x > 0 ? 1 : 0) + (x > 1 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x04) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 50 + 5 + 30 + (x > 0 ? 1 : 0) + (x > 1 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x02) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 50 + 5 + 41 + (x > 1 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x01) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                    }
                } else if (newParam.lcd8850DisplayTime == 400) {
                    for (int y = 0; y < 64; y++) {
                        for (int n = 0; n < 27; n++) {
                            s = newParam.lcd8850Display[n + y * 27];
                            //if (oldParam.lcd8850Display[n + y * 27] != s)
                            //{
                            oldParam.lcd8850Display[n + y * 27] = (byte) s;
                            for (int d = 0; d < 6; d++) {
                                frameBuffer.drawByteArray(n * 6 + d + 4 + 0, 293 + y - 6, FrameBuffer.rMIDILCD[module], 136, 8 * 16 + 3, ((s & (0x20 >> d)) != 0) ? 0 : 8, 1, 1);
                            }
                            //}
                        }
                    }
                    newParam.lcd8850DisplayTime--;
                    if (newParam.lcd8850DisplayTime == 0) {
                        frameBuffer.drawByteArray(0, 272, FrameBuffer.rPlane_MIDI[module], 440, 0, 272, 168, 80);
                    }
                } else if (newParam.lcdDisplayTimeXG > 0) {
                    // XG
                    for (int n = 0; n < 48; n++) {
                        s = newParam.LCDDisplay[n];
                        int x = n / 16;
                        int y = n % 16;
                        frameBuffer.drawByteArray(x * 70 + 5 + 0 + (x > 0 ? 1 : 0) + (x > 1 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x40) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 70 + 5 + 10 + (x > 0 ? 1 : 0) + (x > 1 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x20) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);

                        if (n > 31) continue;

                        frameBuffer.drawByteArray(x * 70 + 5 + 20 + (x > 0 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x10) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 70 + 5 + 30 + (x > 0 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x08) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 70 + 5 + 41,
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x04) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 70 + 5 + 51 + (x > 0 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x02) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                        frameBuffer.drawByteArray(x * 70 + 5 + 61 + (x > 0 ? 1 : 0),
                                288 + y * 3 + (y + 1) / 2,
                                FrameBuffer.rMIDILCD[module], 136, 8 * 16, ((s & 0x01) != 0) ? 0 : 8, 8, (y % 2 == 0 || y == 15) ? 3 : 2);
                    }
                }
            }

            // Prg Bank Map
            drawFont4IntMIDIInstrument(frameBuffer, 4 * 7, ch * 16 + 16, 2 + module, oldParam.pc[ch], newParam.pc[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 10, ch * 16 + 16, 2 + module, oldParam.cc[ch][0], newParam.cc[ch][0]);
            drawFont4IntMIDI(frameBuffer, 4 * 13, ch * 16 + 16, 2 + module, oldParam.cc[ch][32], newParam.cc[ch][32]);

            // Vib Rate Depth Delay
            drawFont4IntMIDI(frameBuffer, 4 * 28, ch * 16 + 24, 2 + module, oldParam.nrpnVibRate[ch], newParam.nrpnVibRate[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 31, ch * 16 + 24, 2 + module, oldParam.nrpnVibDepth[ch], newParam.nrpnVibDepth[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 34, ch * 16 + 24, 2 + module, oldParam.nrpnVibDelay[ch], newParam.nrpnVibDelay[ch]);

            // Filter LPF LPFRsn HPF
            drawFont4IntMIDI(frameBuffer, 4 * 38, ch * 16 + 24, 2 + module, oldParam.nrpnLPF[ch], newParam.nrpnLPF[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 41, ch * 16 + 24, 2 + module, oldParam.nrpnLPFRsn[ch], newParam.nrpnLPFRsn[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 44, ch * 16 + 24, 2 + module, oldParam.nrpnHPF[ch], newParam.nrpnHPF[ch]);

            // EG Atk Dcy Rsn
            drawFont4IntMIDI(frameBuffer, 4 * 48, ch * 16 + 24, 2 + module, oldParam.nrpnEGAttack[ch], newParam.nrpnEGAttack[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 51, ch * 16 + 24, 2 + module, oldParam.nrpnEGDecay[ch], newParam.nrpnEGDecay[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 54, ch * 16 + 24, 2 + module, oldParam.nrpnEGRls[ch], newParam.nrpnEGRls[ch]);

            // EQ Base Gain Frq Treble Gain Frq
            drawFont4IntMIDI(frameBuffer, 4 * 58, ch * 16 + 24, 2 + module, oldParam.nrpnEQBaseGain[ch], newParam.nrpnEQBaseGain[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 61, ch * 16 + 24, 2 + module, oldParam.nrpnEQBaseFrq[ch], newParam.nrpnEQBaseFrq[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 64, ch * 16 + 24, 2 + module, oldParam.nrpnEQTrebleGain[ch], newParam.nrpnEQTrebleGain[ch]);
            drawFont4IntMIDI(frameBuffer, 4 * 67, ch * 16 + 24, 2 + module, oldParam.nrpnEQTrebleFrq[ch], newParam.nrpnEQTrebleFrq[ch]);
        }
    }

    private final MouseListener pbScreen_MouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            int py = ev.getY() / zoom;
        }
    };

    @Override
    public void initScreen() {
        int module = newParam.midiModule;
        newParam.lcd8850DisplayTime = 0;
        frameBuffer.drawByteArray(0, 272, FrameBuffer.rPlane_MIDI[module], 440, 0, 272, 168, 80);
    }

    private void initializeComponent() {
        this.pbScreen = new ScreenPanel();

        //
        // pbScreen
        //
        this.image = Common.getImage("planeMIDI_GM");
        this.pbScreen.setLocation(new Point(0, 0));
        this.pbScreen.setName("pbScreen");
        this.pbScreen.setPreferredSize(new Dimension(440, 352));
        this.pbScreen.addMouseListener(this.pbScreen_MouseClick);
        //
        // frmMIDI
        //
        this.setPreferredSize(new Dimension(440, 352));
        this.getContentPane().add(this.pbScreen);
        this.setIconImage(Common.getImage("Feli128"));
        this.setName("frmMIDI");
        this.setTitle("MIDI");
        this.addWindowListener(this.windowListener);
        this.addComponentListener(this.componentListener);
    }

    private BufferedImage image;
    private ScreenPanel pbScreen;

//#region draw buffer

    private static void screenInitMIDI(FrameBuffer screen) {
    }

    private static void VolumeLCDToMIDILCD(FrameBuffer screen,
                                           int MIDImodule,
                                           int x,
                                           int y,
                                           int oldValue1,
                                           int value1,
                                           int oldValue2,
                                           int value2) {
        if (oldValue1 == value1 && oldValue2 == value2)
            return;

        int s;
        int vy = y;
        // for (int n = (Math.min(oldValue1, value1) / 8); n < 16; n++)
        for (int n = 0; n < 16; n++) {
            s = (value1 / 8) < n ? 8 : 0;
            screen.drawByteArray(x, vy, FrameBuffer.rMIDILCD[MIDImodule], 136, 8 * 16, s, 8, (n % 2 == 0 ? 2 : 3));
            vy -= (n % 2 == 0 ? 4 : 3);
        }

        s = value2 / 8;
        screen.drawByteArray(x, y - s * 3 - (s + 1) / 2, FrameBuffer.rMIDILCD[MIDImodule], 136, 8 * 16, 0, 8, (s % 2 == 0 ? 2 : 3));

        oldValue1 = value1;
        oldValue2 = value2;
    }

    private static int VolumeToMIDILCD(FrameBuffer screen, int MIDImodule, int x, int y, /* ref */ int oldValue, int value) {
        if (oldValue == value)
            return oldValue;

        int s;
        for (int n = (Math.min(oldValue, value) / 5); n < (Math.max(oldValue, value) / 5) + 1; n++) {
            s = (value / 5) < n ? 2 : 0;
            screen.drawByteArray(n * 2 + x, y, FrameBuffer.rMIDILCD_Vol[MIDImodule], 32, 0 + (n > 23 ? 4 : 0) + s, 0, 2, 8);
        }

        oldValue = value;
        return oldValue;
    }

    private static byte drawMIDILCD_Fader(FrameBuffer screen,
                                          int MIDImodule,
                                          int faderType,
                                          int x,
                                          int y,
            /* ref */ byte oldValue,
                                          byte value) {
        if (oldValue == value)
            return oldValue;
        oldValue = value;

        int v;
        switch (faderType) {
            case 0:
                v = Math.max(value - 8, 0) / 8;
                drawMIDILCD_FaderP(screen, MIDImodule, 0, x, y, v);
                break;
            case 1:
                v = value / 8;
                drawMIDILCD_FaderP(screen, MIDImodule, 1, x, y, v);
                break;
        }
        return oldValue;
    }

    private static short drawMIDILCD_Fader(FrameBuffer screen,
                                           int MIDImodule,
                                           int faderType,
                                           int x,
                                           int y,
            /* ref */ short oldValue,
                                           short value) {
        if (oldValue == value)
            return oldValue;
        oldValue = value;

        int v;
        switch (faderType) {
            case 0:
                v = Math.max(value - 0x1ff, 0) / 0x3ff;
                drawMIDILCD_FaderP(screen, MIDImodule, 0, x, y, v);
                break;
            case 1:
                break;
        }
        return oldValue;
    }

    private static byte drawMIDILCD_Kbd(FrameBuffer screen, int x, int y, int note, /* ref */ byte oldVel, byte vel) {
        if (oldVel == vel)
            return oldVel;
        oldVel = vel;

        drawMIDILCD_KbdP(screen, x, y, note, vel);
        return oldVel;
    }

    private static String drawFont4MIDINotes(FrameBuffer screen, int x, int y, int t, /* ref */ String oldnotes, String notes) {
        if (oldnotes.equals(notes))
            return oldnotes;
        oldnotes = notes;

        if (screen == null)
            return oldnotes;

        screen.drawFont4(x, y, t, notes);
        return oldnotes;
    }

    private static void drawMIDI_Lyric(FrameBuffer screen, int chipId, int x, int y, String oldValue1, String value1) {
        // if (oldValue1 == value1) return;

        //        gMIDILyric[chipId].clear(Color.black);
        //        JTextRenderer
        //                .DrawText(gMIDILyric[chipId], value1, fntMIDILyric[chipId], new Point(0, 0), Color.white);
        byte[] bit = FrameBuffer.getByteArray(FrameBuffer.bitmapMIDILyric[chipId]);
        screen.drawByteArray(x, y, bit, 200, 0, 0, 200, 24);

        oldValue1 = value1;
    }

    private static void drawMIDI_MacroXG(FrameBuffer screen,
                                         int MIDImodule,
                                         int macroType,
                                         int x,
                                         int y,
                                         int oldValue1,
                                         int value1) {
        // if (oldValue1 == value1) return;

        screen.drawFont4(x, y, 2 + MIDImodule, Tables.tblMIDIEffectXG[macroType][value1]);

        oldValue1 = value1;
    }

    private static void drawMIDI_MacroGS(FrameBuffer screen,
                                         int MIDImodule,
                                         int macroType,
                                         int x,
                                         int y,
                                         int oldValue1,
                                         int value1) {
        // if (oldValue1 == value1) return;

        screen.drawFont4(x, y, 2 + MIDImodule, Tables.tblMIDIEffectGS[macroType][value1]);

        oldValue1 = value1;
    }

    private static void drawMIDILCD_Letter(FrameBuffer screen, int MIDImodule, int x, int y, byte[] oldValue, int len) {
        for (int i = 0; i < 16; i++) {
            if (oldValue[i] == Tables.spc[i])
                continue;
            oldValue[i] = Tables.spc[i];

            if (screen == null)
                return;

            int cd;
            // if (i < len)
            cd = Tables.spc[i] - ' ';

            screen.drawByteArray(x + i * 8, y, FrameBuffer.rMIDILCD_Font[MIDImodule], 128, (cd % 16) * 8, (cd / 16) * 8, 8, 8);
        }

    }

    private static void drawMIDILCD_Letter(FrameBuffer screen,
                                           int MIDImodule,
                                           int x,
                                           int y,
                                           byte[] oldValue,
                                           byte[] value,
                                           int len) {
        for (int i = 0; i < 20; i++) {
            if (oldValue[i] == value[i])
                continue;
            oldValue[i] = value[i];

            if (screen == null)
                return;

            int cd;
            // if (i < len)
            cd = value[i] - ' ';

            screen.drawByteArray(x + i * 8, y, FrameBuffer.rMIDILCD_Font[MIDImodule], 128, (cd % 16) * 8, (cd / 16) * 8, 8, 8);
        }

    }

    private static void drawFont4IntMIDI(FrameBuffer screen, int x, int y, int t, byte oldnum, byte num) {
        if (oldnum == num)
            return;
        oldnum = num;

        if (screen == null)
            return;

        int n;

        n = num / 100;
        num -= (byte) (n * 100);
        // n = (n > 9) ? 0 : n;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

        n = num / 10;
        num -= (byte) (n * 10);
        x += 4;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

        n = num / 1;
        x += 4;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

    }

    private static void drawFont4IntMIDIInstrument(FrameBuffer screen, int x, int y, int t, byte oldnum, byte num) {
        if (oldnum == num)
            return;
        oldnum = num;

        if (screen == null)
            return;

        screen.drawFont4(x, y + 8, t, Tables.tblMIDIInstrumentGM[num]);

        int n;

        n = num / 100;
        num -= (byte) (n * 100);
        // n = (n > 9) ? 0 : n;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

        n = num / 10;
        num -= (byte) (n * 10);
        x += 4;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

        n = num / 1;
        x += 4;
        screen.drawByteArray(x, y, FrameBuffer.rFont2[t], 128, n * 4 + 64, 0, 4, 8);

    }

    private static void drawMIDILCD_FaderP(FrameBuffer screen, int MIDImodule, int faderType, int x, int y, int value) {
        screen.drawByteArray(x, y, FrameBuffer.rMIDILCD_Fader[MIDImodule], 64, value * 4, faderType * 16, 4, 16);
    }

    private static void drawMIDILCD_KbdP(FrameBuffer screen, int x, int y, int note, int vel) {
        screen.drawByteArrayTransp(x + Tables.kbdl[note % 12] + note / 12 * 28,
                y,
                FrameBuffer.rMIDILCD_KBD,
                16,
                Tables.kbl2[note % 12],
                vel / 16 * 8,
                4,
                8);
    }

//#endregion

    /** what this panel contributes to the GUI; see {@link ViewProvider} */
    public static class Provider implements ViewProvider {

        @Override public String id() { return "MIDI"; }
        @Override public View create(FormMain frm, int chipId, int zoom) { return new FormMIDI(frm, chipId, zoom); }
        @Override public List<SettingTab> settingTabs() {
            return List.of(new SettingMIDIOutPanel(),
                    new SettingMIDIOut2Panel(),
                    new SettingMIDIExpPanel());
        }
    }
}
