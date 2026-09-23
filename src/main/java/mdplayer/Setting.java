package mdplayer;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.IntStream;

import mdplayer.Common.EnmInstFormat;
import mdplayer.chips.Pcm8Chip;
import mdplayer.driver.Plugin;
import mdplayer.driver.mndrv.MNDPlugin;
import mdplayer.driver.mxdrv.MDXPlugin;
import mdplayer.driver.rcp.RCSPlugin;
import mdplayer.vst.VstInfo;
import mdsound.MDSound;
import vavi.util.serdes.JacksonXMLBeanBinder;
import vavi.util.serdes.Serdes;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.dataformat.xml.ser.ToXmlGenerator;

import static java.lang.System.getLogger;

@Serdes(beanBinder = JacksonXMLBeanBinder.class)
public class Setting implements Serializable, Cloneable {

    private static final ResourceBundle rb = ResourceBundle.getBundle("mdplayer/properties/resources");

    private static final Logger logger = getLogger(Setting.class.getName());

    public static class ChipType2 implements Serializable, Cloneable {

        private boolean[] useEmu = null;

        public boolean[] getUseEmu() {
            return useEmu;
        }

        public void setUseEmu(boolean[] value) {
            useEmu = value;
        }

        private boolean[] useReal = null;

        public boolean[] getUseReal() {
            return useReal;
        }

        public void setUseReal(boolean[] value) {
            useReal = value;
        }

        private RealChipInfo[] realChipInfos = null;

        public RealChipInfo[] getRealChipInfo() {
            return realChipInfos;
        }

        public void setRealChipInfo(RealChipInfo[] value) {
            realChipInfos = value;
        }

        public static class RealChipInfo implements Cloneable {

            /** Chip common identification information */
            private int interfaceType = -1;

            public int getInterfaceType() {
                return interfaceType;
            }

            void setInterfaceType(int value) {
                interfaceType = value;
            }

            private int soundLocation = -1;

            public int getSoundLocation() {
                return soundLocation;
            }

            public void setSoundLocation(int value) {
                soundLocation = value;
            }

            private int busID = -1;

            public int getBusID() {
                return busID;
            }

            public void setBusID(int value) {
                busID = value;
            }

            private int soundChip = -1;

            public int getSoundChip() {
                return soundChip;
            }

            public void setSoundChip(int value) {
                soundChip = value;
            }

            private int chipType = 0;

            public int getChipType() {
                return chipType;
            }

            void setChipType(int value) {
                chipType = value;
            }

            private String interfaceName = "";

            public String getInterfaceName() {
                return interfaceName;
            }

            public void setInterfaceName(String value) {
                interfaceName = value;
            }

            private String chipName = "";

            public String getChipName() {
                return chipName;
            }

            void setChipName(String value) {
                chipName = value;
            }

            // Additional Chip-specific Settings

            /** Send wait command to SCCI */
            private boolean useWait = true;

            public boolean getUseWait() {
                return useWait;
            }

            public void setUseWait(boolean value) {
                useWait = value;
            }

            /** Do you want to double the wait command? */
            private boolean useWaitBoost = false;

            public boolean getUseWaitBoost() {
                return useWaitBoost;
            }

            public void setUseWaitBoost(boolean value) {
                useWaitBoost = value;
            }

            /** Do you want to emulate PCM only? */
            private boolean onlyPCMEmulation = false;

            public boolean getOnlyPCMEmulation() {
                return onlyPCMEmulation;
            }

            public void setOnlyPCMEmulation(boolean value) {
                onlyPCMEmulation = value;
            }

            @Override
            public RealChipInfo clone() {
                RealChipInfo ret = new RealChipInfo();

                ret.interfaceType = this.interfaceType;
                ret.soundLocation = this.soundLocation;
                ret.busID = this.busID;
                ret.soundChip = this.soundChip;
                ret.chipType = this.chipType;
                ret.interfaceName = this.interfaceName;
                ret.chipName = this.chipName;

                ret.useWait = this.useWait;
                ret.useWaitBoost = this.useWaitBoost;
                ret.onlyPCMEmulation = this.onlyPCMEmulation;

                return ret;
            }
        }

        /** Emulation delay time */
        private int latencyForEmulation = 0;

        public int getLatencyForEmulation() {
            return latencyForEmulation;
        }

        void setLatencyForEmulation(int value) {
            latencyForEmulation = value;
        }

        private int latencyForReal = 0;

        public int getLatencyForReal() {
            return latencyForReal;
        }

        void setLatencyForReal(int value) {
            latencyForReal = value;
        }

        private boolean[] _useRealChipFreqDiff = new boolean[2];
        public boolean[] getUseRealChipFreqDiff() { return _useRealChipFreqDiff; }
        public void setUseRealChipFreqDiff(boolean[] value) { _useRealChipFreqDiff = value; }

        // Should I switch to YM2149 mode?
        private boolean _YM2149mode = false;

        public boolean getYM2149mode() {
            return _YM2149mode;
        }

        public void setYM2149mode(boolean value) {
            _YM2149mode = value;
        }

        @Override
        public ChipType2 clone() {
            ChipType2 ct = new ChipType2();

            ct.useEmu = null;
            if (this.useEmu != null) {
                ct.useEmu = new boolean[this.useEmu.length];
                System.arraycopy(this.useEmu, 0, ct.useEmu, 0, this.useEmu.length);
            }

            ct.useReal = null;
            if (this.useReal != null) {
                ct.useReal = new boolean[this.useReal.length];
                System.arraycopy(this.useReal, 0, ct.useReal, 0, this.useReal.length);
            }

            ct.realChipInfos = null;
            if (this.realChipInfos != null) {
                ct.realChipInfos = new RealChipInfo[this.realChipInfos.length];
                for (int i = 0; i < this.realChipInfos.length; i++)
                    if (this.realChipInfos[i] != null)
                        ct.realChipInfos[i] = this.realChipInfos[i].clone();
            }

            ct.latencyForEmulation = this.latencyForEmulation;
            ct.latencyForReal = this.latencyForReal;

            return ct;
        }

        public int getEnabledId() {
            return IntStream.range(0, getUseEmu().length).filter(i -> getUseEmu()[i]).findFirst().orElseThrow();
        }
    }

    public static class SID implements Serializable, Cloneable {
        public String romKernalPath = "";
        public String romBasicPath = "";
        public String romCharacterPath = "";
        /**
         * The collection's songlength database, {@code DOCUMENTS/Songlengths.md5}. Where a tune is
         * listed in it, that is how long it plays for and none of the guessing below happens.
         * Empty for none.
         */
        public String songLengthPath = "";
        public int quality = 1;
        public int outputBufferSize = 5000;
        public int c64model = 0;
        public boolean c64modelForce = false;
        public int sidModel = 0;
        public boolean sidmodelForce = false;
        /**
         * Seconds to give a tune before ending it anyway. A Sid never says when it is over, so the
         * driver watches its register writes for the song coming around again - but a tune whose
         * writes never repeat exactly (a counter or a random source in the play routine) is not
         * detectable that way and would play forever. 0 to let such a tune do just that.
         */
        public int maxPlayTime = 300;

        @Override
        public SID clone() {
            SID sid = new SID();

            sid.romKernalPath = this.romKernalPath;
            sid.romBasicPath = this.romBasicPath;
            sid.romCharacterPath = this.romCharacterPath;
            sid.songLengthPath = this.songLengthPath;
            sid.quality = this.quality;
            sid.outputBufferSize = this.outputBufferSize;
            sid.c64model = this.c64model;
            sid.c64modelForce = this.c64modelForce;
            sid.sidModel = this.sidModel;
            sid.sidmodelForce = this.sidmodelForce;
            sid.maxPlayTime = this.maxPlayTime;

            return sid;
        }
    }

    public static class NukedOPN2 implements Serializable, Cloneable {
        public int emuType = 0;

        // Sorry, Gens options are here too...

        public boolean gensDACHPF = true;
        public boolean gensSSGEG = true;

        @Override
        public NukedOPN2 clone() {
            NukedOPN2 no = new NukedOPN2();
            no.emuType = this.emuType;
            no.gensDACHPF = this.gensDACHPF;
            no.gensSSGEG = this.gensSSGEG;

            return no;
        }
    }

    public static class AutoBalance implements Serializable, Cloneable {
        /** on by default: the bundled DefaultVolumeBalance_*.xml presets are calibrated, so a fresh
         *  install should play at the leveled volume without anyone having to switch this on */
        private boolean useThis = true;
        private boolean loadSongBalance = false;
        /** on by default, see {@link #useThis} */
        private boolean loadDriverBalance = true;
        private boolean saveSongBalance = false;
        private boolean samePositionAsSongData = false;

        public boolean getUseThis() {
            return useThis;
        }

        public void setUseThis(boolean value) {
            useThis = value;
        }

        public boolean getLoadSongBalance() {
            return loadSongBalance;
        }

        public void setLoadSongBalance(boolean value) {
            loadSongBalance = value;
        }

        public boolean getLoadDriverBalance() {
            return loadDriverBalance;
        }

        public void setLoadDriverBalance(boolean value) {
            loadDriverBalance = value;
        }

        public boolean getSaveSongBalance() {
            return saveSongBalance;
        }

        public void setSaveSongBalance(boolean value) {
            saveSongBalance = value;
        }

        public boolean getSamePositionAsSongData() {
            return samePositionAsSongData;
        }

        public void setSamePositionAsSongData(boolean value) {
            samePositionAsSongData = value;
        }

        @Override
        public AutoBalance clone() {
            AutoBalance AutoBalance = new AutoBalance();
            AutoBalance.useThis = this.useThis;
            AutoBalance.loadSongBalance = this.loadSongBalance;
            AutoBalance.loadDriverBalance = this.loadDriverBalance;
            AutoBalance.saveSongBalance = this.saveSongBalance;
            AutoBalance.samePositionAsSongData = this.samePositionAsSongData;

            return AutoBalance;
        }
    }

    public static class Pmd implements Serializable, Cloneable {
        public String compilerArguments = "/v /C";
        public boolean isAuto = true;
        public int soundBoard = 1;
        public boolean usePPSDRV = true;
        public boolean usePPZ8 = true;
        public String driverArguments = "";
        public boolean setManualVolume = false;
        public boolean usePPSDRVUseInterfaceDefaultFreq = true;
        public int ppsDrvManualFreq = 2000;
        public int ppsDrvManualWait = 1;
        public int volumeFM = 0;
        public int volumeSSG = 0;
        public int volumeRhythm = 0;
        public int volumeAdpcm = 0;
        public int volumeGIMICSSG = 31;

        @Override
        public Pmd clone() {
            Pmd p = new Pmd();
            p.compilerArguments = this.compilerArguments;
            p.isAuto = this.isAuto;
            p.soundBoard = this.soundBoard;
            p.usePPSDRV = this.usePPSDRV;
            p.usePPZ8 = this.usePPZ8;
            p.driverArguments = this.driverArguments;
            p.setManualVolume = this.setManualVolume;
            p.usePPSDRVUseInterfaceDefaultFreq = this.usePPSDRVUseInterfaceDefaultFreq;
            p.ppsDrvManualFreq = this.ppsDrvManualFreq;
            p.ppsDrvManualWait = this.ppsDrvManualWait;
            p.volumeFM = this.volumeFM;
            p.volumeSSG = this.volumeSSG;
            p.volumeRhythm = this.volumeRhythm;
            p.volumeAdpcm = this.volumeAdpcm;
            p.volumeGIMICSSG = this.volumeGIMICSSG;

            return p;
        }
    }

    public static class MuapJava implements Serializable, Cloneable {

        public int soundDeviceMode = 0;

        @Override
        public MuapJava clone() {
            MuapJava p = new MuapJava();
            p.soundDeviceMode = this.soundDeviceMode;

            return p;
        }
    }

    public static class ZMusic implements Serializable, Cloneable {
        public int compilePriority = 0;
        public int pcm8Type = Integer.getInteger("mdplayer.variant.pcm8", 1);
        public int mpcmType = Integer.getInteger("mdplayer.variant.mpcm", 1);
        public int waitNextPlay = 1000;
        public int pcm8ppsOption = -1;

        @Override
        public ZMusic clone() {
            ZMusic p = new ZMusic();
            p.compilePriority = this.compilePriority;
            p.pcm8Type = this.pcm8Type;
            p.mpcmType = this.mpcmType;
            p.waitNextPlay = this.waitNextPlay;
            p.pcm8ppsOption =this.pcm8ppsOption;

            return p;
        }
    }

    public static class MxDrv implements Serializable, Cloneable {

        /** the song decides by default: see {@link mdplayer.driver.mxdrv.Pcm8Detector} */
        public int pcm8Type = Integer.getInteger("mdplayer.variant.pcm8", Pcm8Chip.AUTO);
        public int pcm8ppsOption = -1;

        @Override
        public MxDrv clone() {
            MxDrv p = new MxDrv();
            p.pcm8Type = this.pcm8Type;
            p.pcm8ppsOption = this.pcm8ppsOption;

            return p;
        }
    }

    public static class Mndrv implements Serializable, Cloneable {
        public int mpcmType = Integer.getInteger("mdplayer.variant.mpcm", 1);

        @Override
        public Mndrv clone() {
            Mndrv p = new Mndrv();
            p.mpcmType = this.mpcmType;

            return p;
        }
    }

    /**
     * A psf says how long it is in its "length" tag, and most rips carry one. A rip that does not
     * - the whole PlayOnline Viewer set, for one - has nothing to end it, and the player would sit
     * on it for ever. These stand in when the file is silent about it, the way the other psf
     * players default to three minutes.
     */
    public static class Psf implements Serializable, Cloneable {
        /** seconds to play a psf whose tags do not say; 0 lets such a song play for ever */
        public int defaultLength = 180;

        /** seconds to fade one of those out over */
        public int defaultFade = 10;

        @Override
        public Psf clone() {
            Psf p = new Psf();
            p.defaultLength = this.defaultLength;
            p.defaultFade = this.defaultFade;

            return p;
        }
    }

    public static class Rcs implements Serializable, Cloneable {
        public int pcm8type = Integer.getInteger("mdplayer.variant.pcm8", 1); // PCM8PP is the default
        /** RCS used to take this from {@link ZMusic}, which left it unsettable per driver */
        public int pcm8ppsOption = -1;

        @Override
        public Rcs clone() {
            Rcs p = new Rcs();
            p.pcm8type = this.pcm8type;
            p.pcm8ppsOption = this.pcm8ppsOption;

            return p;
        }
    }

    public static class MidiExport implements Serializable, Cloneable {

        private boolean useMIDIExport = false;

        public boolean getUseMIDIExport() {
            return useMIDIExport;
        }

        public void setUseMIDIExport(boolean value) {
            useMIDIExport = value;
        }

        private boolean useYM2151Export = false;

        public boolean getUseYM2151Export() {
            return useYM2151Export;
        }

        public void setUseYM2151Export(boolean value) {
            useYM2151Export = value;
        }

        private boolean useYM2612Export = true;

        public boolean getUseYM2612Export() {
            return useYM2612Export;
        }

        public void setUseYM2612Export(boolean value) {
            useYM2612Export = value;
        }

        private String exportPath = "";

        public String getExportPath() {
            return exportPath;
        }

        public void setExportPath(String value) {
            exportPath = value;
        }

        private boolean useVOPMex = false;

        public boolean getUseVOPMex() {
            return useVOPMex;
        }

        public void setUseVOPMex(boolean value) {
            useVOPMex = value;
        }

        private boolean keyOnFnum = false;

        public boolean getKeyOnFnum() {
            return keyOnFnum;
        }

        public void setKeyOnFnum(boolean value) {
            keyOnFnum = value;
        }

        @Override
        public MidiExport clone() {
            MidiExport midiExport = new MidiExport();

            midiExport.useMIDIExport = this.useMIDIExport;
            midiExport.useYM2151Export = this.useYM2151Export;
            midiExport.useYM2612Export = this.useYM2612Export;
            midiExport.exportPath = this.exportPath;
            midiExport.useVOPMex = this.useVOPMex;
            midiExport.keyOnFnum = this.keyOnFnum;

            return midiExport;
        }
    }

    public static class MidiKbd implements Serializable, Cloneable {

        private boolean useMIDIKeyboard = false;
        public boolean getUseMIDIKeyboard() {
            return useMIDIKeyboard;
        }
        public void setUseMIDIKeyboard(boolean value) {
            useMIDIKeyboard = value;
        }
        private String midiInDeviceName = "";
        public String getMidiInDeviceName() {
            return midiInDeviceName;
        }
        public void setMidiInDeviceName(String value) {
            midiInDeviceName = value;
        }
        private boolean isMono = true;
        public boolean isMono() {
            return isMono;
        }
        public void setMono(boolean value) {
            isMono = value;
        }
        private int useFormat = 0;
        public int getUseFormat() {
            return useFormat;
        }
        public void setUseFormat(int value) {
            useFormat = value;
        }
        private int useMonoChannel = 0;
        public int getUseMonoChannel() {
            return useMonoChannel;
        }
        public void setUseMonoChannel(int value) {
            useMonoChannel = value;
        }
        private boolean[] useChannel = new boolean[9];
        public boolean[] getUseChannel() {
            return useChannel;
        }
        void setUseChannel(boolean[] value) {
            useChannel = value;
        }
        private Tone[] tones = new Tone[6];
        public Tone[] getTones() {
            return tones;
        }
        void setTones(Tone[] value) {
            tones = value;
        }
        private int midiCtrlCopyToneFromYM2612Ch1 = 97;
        public int getMidiCtrl_CopyToneFromYM2612Ch1() {
            return midiCtrlCopyToneFromYM2612Ch1;
        }
        public void setMidiCtrl_CopyToneFromYM2612Ch1(int value) {
            midiCtrlCopyToneFromYM2612Ch1 = value;
        }
        private int midiCtrlDelOneLog = 96;
        public int getMidiCtrl_DelOneLog() {
            return midiCtrlDelOneLog;
        }
        public void setMidiCtrl_DelOneLog(int value) {
            midiCtrlDelOneLog = value;
        }
        private int midiCtrlCopySelecttingLogToClipbrd = 66;
        public int getMidiCtrl_CopySelecttingLogToClipbrd() {
            return midiCtrlCopySelecttingLogToClipbrd;
        }
        public void setMidiCtrl_CopySelecttingLogToClipbrd(int value) {
            midiCtrlCopySelecttingLogToClipbrd = value;
        }
        private int midiCtrlStop = -1;
        public int getMidiCtrl_Stop() {
            return midiCtrlStop;
        }
        public void setMidiCtrl_Stop(int value) {
            midiCtrlStop = value;
        }
        private int midiCtrlPause = -1;
        public int getMidiCtrl_Pause() {
            return midiCtrlPause;
        }
        public void setMidiCtrl_Pause(int value) {
            midiCtrlPause = value;
        }
        private int midiCtrlFadeout = -1;
        public int getMidiCtrl_Fadeout() {
            return midiCtrlFadeout;
        }
        public void setMidiCtrl_Fadeout(int value) {
            midiCtrlFadeout = value;
        }
        private int midiCtrlPrevious = -1;
        public int getMidiCtrl_Previous() {
            return midiCtrlPrevious;
        }
        public void setMidiCtrl_Previous(int value) {
            midiCtrlPrevious = value;
        }
        private int midiCtrlSlow = -1;
        public int getMidiCtrlSlow() {
            return midiCtrlSlow;
        }
        public void setMidiCtrlSlow(int value) {
            midiCtrlSlow = value;
        }
        private int midiCtrlPlay = -1;
        public int getMidiCtrl_Play() {
            return midiCtrlPlay;
        }
        public void setMidiCtrl_Play(int value) {
            midiCtrlPlay = value;
        }
        private int midiCtrlFast = -1;
        public int getMidiCtrl_Fast() {
            return midiCtrlFast;
        }
        public void setMidiCtrl_Fast(int value) {
            midiCtrlFast = value;
        }
        private int midiCtrlNext = -1;
        public int getMidiCtrl_Next() {
            return midiCtrlNext;
        }
        public void setMidiCtrl_Next(int value) {
            midiCtrlNext = value;
        }

        @Override
        public MidiKbd clone() {
            MidiKbd midiKbd = new MidiKbd();

            midiKbd.midiInDeviceName = this.midiInDeviceName;
            midiKbd.useMIDIKeyboard = this.useMIDIKeyboard;
            System.arraycopy(this.useChannel, 0, midiKbd.useChannel, 0, midiKbd.useChannel.length);
            midiKbd.isMono = this.isMono;
            midiKbd.useMonoChannel = this.useMonoChannel;

            midiKbd.midiCtrlCopySelecttingLogToClipbrd = this.midiCtrlCopySelecttingLogToClipbrd;
            midiKbd.midiCtrlCopyToneFromYM2612Ch1 = this.midiCtrlCopyToneFromYM2612Ch1;
            midiKbd.midiCtrlDelOneLog = this.midiCtrlDelOneLog;
            midiKbd.midiCtrlFadeout = this.midiCtrlFadeout;
            midiKbd.midiCtrlFast = this.midiCtrlFast;
            midiKbd.midiCtrlNext = this.midiCtrlNext;
            midiKbd.midiCtrlPause = this.midiCtrlPause;
            midiKbd.midiCtrlPlay = this.midiCtrlPlay;
            midiKbd.midiCtrlPrevious = this.midiCtrlPrevious;
            midiKbd.midiCtrlSlow = this.midiCtrlSlow;
            midiKbd.midiCtrlStop = this.midiCtrlStop;

            return midiKbd;
        }
    }

    public static class KeyBoardHook implements Serializable, Cloneable {
        public static class HookKeyInfo implements Serializable {
            private boolean shift = false;
            private boolean ctrl = false;
            private boolean win = false;
            private boolean alt = false;
            private String key = "(None)";

            public boolean getShift() {
                return shift;
            }
            public void setShift(boolean value) {
                shift = value;
            }
            public boolean getCtrl() {
                return ctrl;
            }
            public void setCtrl(boolean value) {
                ctrl = value;
            }
            public boolean getWin() {
                return win;
            }
            public void setWin(boolean value) {
                win = value;
            }
            public boolean getAlt() {
                return alt;
            }
            public void setAlt(boolean value) {
                alt = value;
            }
            public String getKey() {
                return key;
            }
            public void setKey(String value) {
                key = value;
            }

            @Override
            public HookKeyInfo clone() {
                HookKeyInfo hookKeyInfo = new HookKeyInfo();
                hookKeyInfo.shift = this.shift;
                hookKeyInfo.ctrl = this.ctrl;
                hookKeyInfo.win = this.win;
                hookKeyInfo.alt = this.alt;
                hookKeyInfo.key = this.key;

                return hookKeyInfo;
            }
        }

        private boolean useKeyBoardHook = false;
        public boolean getUseKeyBoardHook() {
            return useKeyBoardHook;
        }
        public void setUseKeyBoardHook(boolean value) {
            useKeyBoardHook = value;
        }
        public HookKeyInfo getStop() {
            return stop;
        }
        void setStop(HookKeyInfo value) {
            stop = value;
        }
        public HookKeyInfo getPause() {
            return pause;
        }
        void setPause(HookKeyInfo value) {
            pause = value;
        }
        public HookKeyInfo getFadeout() {
            return fadeout;
        }
        void setFadeout(HookKeyInfo value) {
            fadeout = value;
        }
        public HookKeyInfo getPrev() {
            return prev;
        }
        void setPrev(HookKeyInfo value) {
            prev = value;
        }
        public HookKeyInfo getSlow() {
            return slow;
        }
        void setSlow(HookKeyInfo value) {
            slow = value;
        }
        public HookKeyInfo getPlay() {
            return play;
        }
        void setPlay(HookKeyInfo value) {
            play = value;
        }
        public HookKeyInfo getNext() {
            return next;
        }
        void setNext(HookKeyInfo value) {
            next = value;
        }
        public HookKeyInfo getFast() {
            return fast;
        }
        void setFast(HookKeyInfo value) {
            fast = value;
        }

        private HookKeyInfo stop = new HookKeyInfo();
        private HookKeyInfo pause = new HookKeyInfo();
        private HookKeyInfo fadeout = new HookKeyInfo();
        private HookKeyInfo prev = new HookKeyInfo();
        private HookKeyInfo slow = new HookKeyInfo();
        private HookKeyInfo play = new HookKeyInfo();
        private HookKeyInfo next = new HookKeyInfo();
        private HookKeyInfo fast = new HookKeyInfo();

        @Override
        public KeyBoardHook clone() {
            KeyBoardHook keyBoard = new KeyBoardHook();
            keyBoard.useKeyBoardHook = this.useKeyBoardHook;
            keyBoard.stop = this.stop.clone();
            keyBoard.pause = this.pause.clone();
            keyBoard.fadeout = this.fadeout.clone();
            keyBoard.prev = this.prev.clone();
            keyBoard.slow = this.slow.clone();
            keyBoard.play = this.play.clone();
            keyBoard.next = this.next.clone();
            keyBoard.fast = this.fast.clone();

            return keyBoard;
        }
    }

    public static class Vst implements Serializable, Cloneable {

        private String defaultPath = "";
        private String[] vstPluginPath = null;
        public String[] getVstPluginPath() {
            return vstPluginPath;
        }
        void setVstPluginPath(String[] value) {
            vstPluginPath = value;
        }
        private VstInfo[] vstInfos = null;
        public VstInfo[] getVSTInfo() {
            return vstInfos;
        }
        public void setVSTInfo(VstInfo[] value) {
            vstInfos = value;
        }
        public String getDefaultPath() {
            return defaultPath;
        }
        public void setDefaultPath(String value) {
            defaultPath = value;
        }

        @Override
        public Vst clone() {
            Vst vst = new Vst();

            vst.vstInfos = this.vstInfos;
            vst.defaultPath = this.defaultPath;

            return vst;
        }
    }
    static class MidiOutInfoArray implements Serializable {
        private MidiOutInfo[] infos = new MidiOutInfo[0];
        MidiOutInfo[] getInfos() { return infos; }
        public void setInfos(MidiOutInfo[] value) { infos = value; }
        public MidiOutInfoArray() {}
        MidiOutInfoArray(MidiOutInfo[] value) { infos = value; }
    }

    public static class MidiOut implements Serializable, Cloneable {

        private String gmReset = "30:F0,7E,7F,09,01,F7";
        public String getGMReset() {
            return gmReset;
        }
        public void setGMReset(String value) {
            gmReset = value;
        }
        private String xgReset = "30:F0,43,10,4C,00,00,7E,00,F7";
        public String getXGReset() {
            return xgReset;
        }
        public void setXGReset(String value) {
            xgReset = value;
        }
        private String gsReset = "30:F0,41,10,42,12,40,00,7F,00,41,F7";
        public String getGSReset() {
            return gsReset;
        }
        public void setGSReset(String value) {
            gsReset = value;
        }
        private String custom = "";
        public String getCustom() {
            return custom;
        }
        public void setCustom(String value) {
            custom = value;
        }
        private List<MidiOutInfo[]> midiOutInfos = null;
        @com.fasterxml.jackson.annotation.JsonIgnore
        public List<MidiOutInfo[]> getMidiOutInfos() {
            return midiOutInfos;
        }
        @com.fasterxml.jackson.annotation.JsonIgnore
        public void setMidiOutInfos(List<MidiOutInfo[]> value) {
            midiOutInfos = value;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("midiOutInfos")
        @tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper(useWrapping = true, localName = "midiOutInfos")
        @tools.jackson.dataformat.xml.annotation.JacksonXmlProperty(localName = "midiOutInfoArray")
        public List<MidiOutInfoArray> getMidiOutInfosForJackson() {
            if (midiOutInfos == null) return null;
            List<MidiOutInfoArray> result = new ArrayList<>();
            for (MidiOutInfo[] array : midiOutInfos) {
                result.add(new MidiOutInfoArray(array));
            }
            return result;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("midiOutInfos")
        public void setMidiOutInfosForJackson(List<MidiOutInfoArray> value) {
            if (value == null) {
                midiOutInfos = null;
                return;
            }
            midiOutInfos = new ArrayList<>();
            for (MidiOutInfoArray wrapper : value) {
                if (wrapper == null) {
                    midiOutInfos.add(null);
                } else {
                    midiOutInfos.add(wrapper.getInfos());
                }
            }
        }

        @Override
        public MidiOut clone() {
            MidiOut MidiOut = new MidiOut();

            MidiOut.gmReset = this.gmReset;
            MidiOut.xgReset = this.xgReset;
            MidiOut.gsReset = this.gsReset;
            MidiOut.custom = this.custom;
            MidiOut.midiOutInfos = this.midiOutInfos;

            return MidiOut;
        }
    }

    public static final Point EmptyPoint = new Point(0, 0);
    public static final Dimension EmptyDimension = new Dimension(0, 0);

    private static final Setting instance;

    static {
        instance = new Setting();
        instance.init();
    }

    private static int parseInt(String str, int def) {
        try {
            return Integer.parseInt(System.getProperty(str, String.valueOf(def)));
        } catch (Exception e) {
            logger.log(Level.TRACE, e.getMessage(), e);
            return def;
        }
    }

    public void init() {
        // ay8910 PSG
        if (this.getAY8910Type() == null || this.getAY8910Type().length < 2) {
            this.setAY8910Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getAY8910Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getAY8910Type()[i].setUseEmu(new boolean[3]); // {0: fmgen, 1: mame, 2: np}
                this.getAY8910Type()[i].getUseEmu()[parseInt("mdplayer.variant.ay8910", 0)] = true;
                this.getAY8910Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getK051649Type() == null || this.getK051649Type().length < 2) {
            this.setK051649Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getK051649Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getK051649Type()[i].setUseEmu(new boolean[1]);
                this.getK051649Type()[i].getUseEmu()[0] = true;
                this.getK051649Type()[i].setUseReal(new boolean[1]);
            }
        }
        // c140, c219
        if (this.getC140Type() == null || this.getC140Type().length < 2) {
            this.setC140Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getC140Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getC140Type()[i].setUseEmu(new boolean[2]); // {0: c140, 1: c219}
                this.getC140Type()[i].getUseEmu()[parseInt("mdplayer.variant.c140", 0)] = true;
                this.getC140Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getHuC6280Type() == null || this.getHuC6280Type().length < 2) {
            this.setHuC6280Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getHuC6280Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getHuC6280Type()[i].setUseEmu(new boolean[1]);
                this.getHuC6280Type()[i].getUseEmu()[0] = true;
                this.getHuC6280Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getSEGAPCMType() == null || this.getSEGAPCMType().length < 2) {
            this.setSEGAPCMType(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getSEGAPCMType()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getSEGAPCMType()[i].setUseEmu(new boolean[1]);
                this.getSEGAPCMType()[i].getUseEmu()[0] = true;
                this.getSEGAPCMType()[i].setUseReal(new boolean[1]);
            }
        }
        // sn76489
        if (this.getSN76489Type() == null || this.getSN76489Type().length < 2) {
            this.setSN76489Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getSN76489Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getSN76489Type()[i].setUseEmu(new boolean[2]); // {0: sn76489, 1: sn76496}
                this.getSN76489Type()[i].getUseEmu()[parseInt("mdplayer.variant.sn76489", 0)] = true;
                this.getSN76489Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getY8950Type() == null || this.getY8950Type().length < 2) {
            this.setY8950Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getY8950Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getY8950Type()[i].setUseEmu(new boolean[1]);
                this.getY8950Type()[i].getUseEmu()[0] = true;
                this.getY8950Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym2151 OPM
        if (this.getYM2151Type() == null || this.getYM2151Type().length < 2) {
            this.setYM2151Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM2151Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM2151Type()[i].setUseEmu(new boolean[4]); // {0: fmgen, 1: mame, 2: 68k, 3: ymfm}
                this.getYM2151Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2151", 0)] = true;
                this.getYM2151Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym2203 OPN
        if (this.getYM2203Type() == null || this.getYM2203Type().length < 2) {
            this.setYM2203Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM2203Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM2203Type()[i].setUseEmu(new boolean[2]); // {0: fmgen, 1: ymfm}
                this.getYM2203Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2203", 0)] = true;
                this.getYM2203Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym2413 OPLL
        if (this.getYM2413Type() == null || this.getYM2413Type().length < 2) {
            this.setYM2413Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM2413Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM2413Type()[i].setUseEmu(new boolean[4]); // {0: mame, 1: vrc7, 2: emu, 3: np}
                this.getYM2413Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2413", 0)] = true;
                this.getYM2413Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym2608 OPNA
        if (this.getYM2608Type() == null || this.getYM2608Type().length < 2) {
            this.setYM2608Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM2608Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM2608Type()[i].setUseEmu(new boolean[2]); // {0: fmgen, 1: ymfm}
                this.getYM2608Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2608", 0)] = true;
                this.getYM2608Type()[i].setUseReal(new boolean[1]);
            }
        }

        // ym2610 OPNB
        if (this.getYM2610Type() == null
                || this.getYM2610Type().length < 2
                || this.getYM2610Type()[0].getUseReal() == null
                || this.getYM2610Type()[0].getUseReal().length < 3
                || this.getYM2610Type()[1].getUseReal() == null
                || this.getYM2610Type()[1].getUseReal().length < 3
        ) {
            this.setYM2610Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM2610Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo(), new Setting.ChipType2.RealChipInfo(), new Setting.ChipType2.RealChipInfo()});
                this.getYM2610Type()[i].setUseEmu(new boolean[2]); // {0: fmgen, 1: ymfm}
                this.getYM2610Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2610", 0)] = true;
                this.getYM2610Type()[i].setUseReal(new boolean[3]);
            }
        }

        // ym2612 OPN2
        if (this.getYM2612Type() == null || this.getYM2612Type().length < 2) {
            this._YM2612Type = new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()};
            for (int i = 0; i < 2; i++) {
                this.getYM2612Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM2612Type()[i].setUseEmu(new boolean[5]); // {0: mame-A, 1: nuke-A, 2: mame-B, 3: nuke-B(simple), 4: nuke-A(vavi)}
                this.getYM2612Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym2612", 0)] = true;
                this.getYM2612Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym3526 OPL
        if (this.getYM3526Type() == null || this.getYM3526Type().length < 2) {
            this.setYM3526Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM3526Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM3526Type()[i].setUseEmu(new boolean[1]);
                this.getYM3526Type()[i].getUseEmu()[0] = true;
                this.getYM3526Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ym3812 OPL2
        if (this.getYM3812Type() == null || this.getYM3812Type().length < 2) {
            this.setYM3812Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYM3812Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYM3812Type()[i].setUseEmu(new boolean[2]); // {0: dosbox, 1: mame}
                this.getYM3812Type()[i].getUseEmu()[parseInt("mdplayer.variant.ym3812", 0)] = true;
                this.getYM3812Type()[i].setUseReal(new boolean[1]);
            }
        }
        // ymf262 OPL3
        if (this.getYMF262Type() == null || this.getYMF262Type().length < 2) {
            this.setYMF262Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYMF262Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYMF262Type()[i].setUseEmu(new boolean[5]); // {0: dosbox 1: mame, 2: nuked, 3: cozendey, 4: ymfm}
                this.getYMF262Type()[i].getUseEmu()[parseInt("mdplayer.variant.ymf262", 0)] = true;
                this.getYMF262Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getYMF271Type() == null || this.getYMF271Type().length < 2) {
            this.setYMF271Type(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYMF271Type()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYMF271Type()[i].setUseEmu(new boolean[1]);
                this.getYMF271Type()[i].getUseEmu()[0] = true;
                this.getYMF271Type()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getYMF278BType() == null || this.getYMF278BType().length < 2) {
            this.setYMF278BType(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYMF278BType()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYMF278BType()[i].setUseEmu(new boolean[1]);
                this.getYMF278BType()[i].getUseEmu()[0] = true;
                this.getYMF278BType()[i].setUseReal(new boolean[1]);
            }
        }
        if (this.getYMZ280BType() == null || this.getYMZ280BType().length < 2) {
            this.setYMZ280BType(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getYMZ280BType()[i].setRealChipInfo(new Setting.ChipType2.RealChipInfo[] {new Setting.ChipType2.RealChipInfo()});
                this.getYMZ280BType()[i].setUseEmu(new boolean[1]);
                this.getYMZ280BType()[i].getUseEmu()[0] = true;
                this.getYMZ280BType()[i].setUseReal(new boolean[1]);
            }
        }
        // qsound
        if (this.getQSoundType() == null || this.getQSoundType().length < 2) {
            this.setQSoundType(new Setting.ChipType2[] {new Setting.ChipType2(), new Setting.ChipType2()});
            for (int i = 0; i < 2; i++) {
                this.getQSoundType()[i].setUseEmu(new boolean[2]); // {0: qsound-ctr, 1: qsound}
                this.getQSoundType()[i].getUseEmu()[Integer.parseInt(System.getProperty("mdplayer.variant.qsound", "0"))] = true;
                this.getQSoundType()[i].setUseReal(new boolean[1]);
            }
        }
    }

    public static Setting getInstance() {
        return instance;
    }

    // Multiple audio sources
    private String fileSearchPathList;
    public String getFileSearchPathList() {
        return fileSearchPathList;
    }
    public void setFileSearchPathList(String value) {
        fileSearchPathList = value;
    }
    private OutputDevice outputDevice = new OutputDevice();
    public OutputDevice getOutputDevice() {
        return outputDevice;
    }
    void setOutputDevice(OutputDevice value) {
        outputDevice = value;
    }
    private ChipType2[] ay8910Type = null;
    public ChipType2[] getAY8910Type() {
        return ay8910Type;
    }
    public void setAY8910Type(ChipType2[] value) {
        ay8910Type = value;
    }

//    private ChipType2[] _AY8910SType = new ChipType2();
//    AY8910SType getAY8910SType() { return _AY8910SType; }
//    setAY8910SType(ChipType2[] value) { _AY8910SType = value; }

    private ChipType2[] ym2151Type = null;
    public ChipType2[] getYM2151Type() {
        return ym2151Type;
    }
    public void setYM2151Type(ChipType2[] value) {
        ym2151Type = value;
    }
    private ChipType2[] ym2203Type = null;
    public ChipType2[] getYM2203Type() {
        return ym2203Type;
    }
    public void setYM2203Type(ChipType2[] value) {
        ym2203Type = value;
    }
    private ChipType2[] ym2413Type = null;
    public ChipType2[] getYM2413Type() {
        return ym2413Type;
    }
    public void setYM2413Type(ChipType2[] value) {
        ym2413Type = value;
    }
    private ChipType2[] huC6280Type = null;
    public ChipType2[] getHuC6280Type() {
        return huC6280Type;
    }
    public void setHuC6280Type(ChipType2[] value) {
        huC6280Type = value;
    }
    private ChipType2[] k051649Type = null;
    public ChipType2[] getK051649Type() {
        return k051649Type;
    }
    public void setK051649Type(ChipType2[] value) {
        k051649Type = value;
    }

//    private ChipType2[] _YM2413SType = null;
//    public getYM2413SType() { return _YM2413SType; }
//    setYM2413SType(ChipType2[] value) { _YM2413SType = value; }

    private ChipType2[] ym2608Type = null;
    public ChipType2[] getYM2608Type() {
        return ym2608Type;
    }
    public void setYM2608Type(ChipType2[] value) {
        ym2608Type = value;
    }
    private ChipType2[] ym2610Type = null;
    public ChipType2[] getYM2610Type() {
        return ym2610Type;
    }
    public void setYM2610Type(ChipType2[] value) {
        ym2610Type = value;
    }
    private ChipType2[] ymf262Type = null;
    public ChipType2[] getYMF262Type() {
        return ymf262Type;
    }
    public void setYMF262Type(ChipType2[] value) {
        ymf262Type = value;
    }
    private ChipType2[] ymf271Type = null;
    public ChipType2[] getYMF271Type() {
        return ymf271Type;
    }
    public void setYMF271Type(ChipType2[] value) {
        ymf271Type = value;
    }
    private ChipType2[] _YMF278BType = null;
    public ChipType2[] getYMF278BType() {
        return _YMF278BType;
    }
    public void setYMF278BType(ChipType2[] value) {
        _YMF278BType = value;
    }
    private ChipType2[] _YMZ280BType = null;
    public ChipType2[] getYMZ280BType() {
        return _YMZ280BType;
    }
    public void setYMZ280BType(ChipType2[] value) {
        _YMZ280BType = value;
    }
    private ChipType2[] _YM2612Type = null;
    public ChipType2[] getYM2612Type() {
        return _YM2612Type;
    }
    private ChipType2[] _SN76489Type = null;
    public ChipType2[] getSN76489Type() {
        return _SN76489Type;
    }
    public void setSN76489Type(ChipType2[] value) {
        _SN76489Type = value;
    }

    // private ChipType2 _YM2151SType = new ChipType2();
    // public getYM2151SType() { return _YM2151SType; }
    // setYM2151SType(ChipType2 ) { _YM2151SType = value; }

    // private ChipType2 _YM2203SType = new ChipType2();
    // public ChipType2 getYM2203SType() { return _YM2203SType; }
    // setYM2203SType(ChipType2 value) { _YM2203SType = value; }

    // private ChipType2 _YM2608SType = new ChipType2();
    // public ChipType2 getYM2608SType() { return _YM2608SType; }
    // setYM2608SType(ChipType2) { _YM2608SType = value; }

    // private ChipType2 _YM2610SType = new ChipType2();
    // public ChipType2 getYM2610SType() { return _YM2610SType; }
    // setYM2610SType(ChipType2 { _YM2610SType = value; }

    // private ChipType2 _YM2612SType = new ChipType2();
    // public ChipType2 getYM2612SType() { return _YM2612SType; }
    // setYM2612SType(ChipType2 { _YM2612SType = value; }

    // private ChipType2 _YMF262SType = new ChipType2();
    // public ChipType2 getYMF262SType() { return _YMF262SType; }
    // setYMF262SType(ChipType2 { _YMF262SType = value; }

    // private ChipType2 _YMF271SType = new ChipType2();
    // public ChipType2 YMF271SType
    // get()
    // {
    // return _YMF271SType;
    // }
    // set {
    // _YMF271SType = value;
    // }

    // private ChipType2 _YMF278BSType = new ChipType2();
    // public ChipType2 YMF278BSType
    // get() {
    // return _YMF278BSType;
    // }
    // set {
    // _YMF278BSType = value;
    // }

    // private ChipType2 _YMZ280BSType = new ChipType2();
    // public ChipType2 YMZ280BSType {
    // get() {
    // return _YMZ280BSType;
    // }
    // set {
    // _YMZ280BSType = value;
    // }

    // private ChipType2 _SN76489SType = new ChipType2();
    // public ChipType2 SN76489SType {
    // get() {
    // return _SN76489SType;
    // }
    // set {
    // _SN76489SType = value;
    // }

    // private ChipType2 _HuC6280SType = new ChipType2();
    // public ChipType2 HuC6280SType {
    // get() {
    // return _HuC6280SType;
    // }
    // set {
    // _HuC6280SType = value;
    // }

    private ChipType2[] _YM3526Type = null;

    public ChipType2[] getYM3526Type() {
        return _YM3526Type;
    }

    public void setYM3526Type(ChipType2[] value) {
        _YM3526Type = value;
    }

    // private ChipType2 _YM3526SType = new ChipType2();
    // public ChipType2 YM3526SType
    // get() {
    // return _YM3526SType;
    // }
    // set {
    // _YM3526SType = value;
    // }

    private ChipType2[] _YM3812Type = null;
    public ChipType2[] getYM3812Type() {
        return _YM3812Type;
    }
    public void setYM3812Type(ChipType2[] value) {
        _YM3812Type = value;
    }

    // private ChipType2 _YM3812SType = new ChipType2();
    // public ChipType2 YM3812SType
    // get() {
    // return _YM3812SType;
    // }
    // set {
    // _YM3812SType = value;
    // }

    private ChipType2[] _Y8950Type = null;
    public ChipType2[] getY8950Type() {
        return _Y8950Type;
    }
    public void setY8950Type(ChipType2[] value) {
        _Y8950Type = value;
    }

    // private ChipType2 _Y8950SType = new ChipType2();
    // public ChipType2 Y8950SType
    // get() {
    // return _Y8950SType;
    // }
    // set {
    // _Y8950SType = value;
    // }

    private ChipType2[] _C140Type = null;
    public ChipType2[] getC140Type() {
        return _C140Type;
    }
    public void setC140Type(ChipType2[] value) {
        _C140Type = value;
    }

    // private ChipType2 _C140SType = new ChipType2();
    // public ChipType2 C140SType
    // get() {
    // return _C140SType;
    // }
    // set {
    // _C140SType = value;
    // }

    private ChipType2[] _SEGAPCMType = null;
    public ChipType2[] getSEGAPCMType() {
        return _SEGAPCMType;
    }
    public void setSEGAPCMType(ChipType2[] value) {
        _SEGAPCMType = value;
    }

    // private ChipType2 _SEGAPCMSType = new ChipType2();
    // public ChipType2 SEGAPCMSType
    // get() {
    // return _SEGAPCMSType;
    // }
    // set {
    // _SEGAPCMSType = value;
    // }

    private ChipType2[] _QSoundType = null;

    public ChipType2[] getQSoundType() {
        return _QSoundType;
    }

    public void setQSoundType(ChipType2[] value) {
        _QSoundType = value;
    }

    private int latencyEmulation = 0;
    public int getLatencyEmulation() {
        return latencyEmulation;
    }
    public void setLatencyEmulation(int value) {
        latencyEmulation = value;
    }
    private int latencySCCI = 0;
    public int getLatencySCCI() {
        return latencySCCI;
    }
    public void setLatencySCCI(int value) {
        latencySCCI = value;
    }
    private boolean hiyorimiMode = true;
    public boolean getHiyorimiMode() {
        return hiyorimiMode;
    }
    public void setHiyorimiMode(boolean value) {
        hiyorimiMode = value;
    }
    private boolean debugDispFrameCounter = false;
    public boolean getDebug_DispFrameCounter() {
        return debugDispFrameCounter;
    }
    public void setDebug_DispFrameCounter(boolean value) {
        debugDispFrameCounter = value;
    }
    private int debugSCCbaseAddress = 0x9800;
    public int getDebug_SCCbaseAddress() {
        return debugSCCbaseAddress;
    }
    public void setDebug_SCCbaseAddress(int value) {
        debugSCCbaseAddress = value;
    }
    private Other other = new Other();
    public Other getOther() {
        return other;
    }
    void setOther(Other value) {
        other = value;
    }
    private Balance balance = new Balance();
    public Balance getBalance() {
        return balance;
    }
    public void setBalance(Balance value) {
        balance = value;
    }
    private Location location = new Location();
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location value) {
        location = value;
    }
    private MidiExport midiExport = new MidiExport();
    public MidiExport getMidiExport() {
        return midiExport;
    }
    void setMidiExport(MidiExport value) {
        midiExport = value;
    }
    private MidiKbd midiKbd = new MidiKbd();
    public MidiKbd getMidiKbd() {
        return midiKbd;
    }
    void setMidiKbd(MidiKbd value) {
        midiKbd = value;
    }
    private Vst vst = new Vst();
    public Vst getVst() {
        return vst;
    }
    void setVst(Vst value) {
        vst = value;
    }
    private MidiOut midiOut = new MidiOut();
    public MidiOut getMidiOut() {
        return midiOut;
    }
    void setMidiOut(MidiOut value) {
        midiOut = value;
    }
    private NSF nsf = new NSF();
    public NSF getNsf() {
        return nsf;
    }
    void setNsf(NSF value) {
        nsf = value;
    }
    private SID sid = new SID();
    public SID getSid() {
        return sid;
    }
    public void setSid(SID value) {
        sid = value;
    }

    private NukedOPN2 nukedOPN2 = new NukedOPN2();

    public NukedOPN2 getNukedOPN2() {
        return nukedOPN2;
    }

    public void setNukedOPN2(NukedOPN2 value) {
        nukedOPN2 = value;
    }

    private AutoBalance autoBalance = new AutoBalance();

    public AutoBalance getAutoBalance() {
        return autoBalance;
    }

    public void setAutoBalance(AutoBalance value) {
        autoBalance = value;
    }

    private Pmd pmd = new Pmd();

    public Pmd getPmd() {
        return pmd;
    }

    void setPmd(Pmd value) {
        pmd = value;
    }

    private MuapJava _muapJava = new MuapJava();

    public MuapJava getMuapJava() {
        return _muapJava;
    }

    public void setMuapJava(MuapJava value) {
        _muapJava = value;
    }

    private ZMusic zMusic = new ZMusic();

    public ZMusic getZMusic() {
        return zMusic;
    }

    public void setZMusic(ZMusic value) {
        zMusic = value;
    }

    private MxDrv mxDrv = new MxDrv();

    public MxDrv getMxDrv() { return mxDrv; }

    public void setMxDrv(MxDrv value) { mxDrv = value; }

    private Mndrv mnDrv = new Mndrv();

    public Mndrv getMnDrv() { return mnDrv; }

    public void setMnDrv(Mndrv value) { mnDrv = value; }

    private Psf psf = new Psf();

    public Psf getPsf() {
        return psf;
    }

    public void setPsf(Psf value) {
        psf = value;
    }

    private Rcs rcs = new Rcs();

    public Rcs getRcs() {
        return rcs;
    }

    public void setRcs(Rcs value) {
        rcs = value;
    }

    public Setting.KeyBoardHook getKeyboardHook() {
        return _keyBoardHook;
    }

    void setKeyBoardHook(KeyBoardHook value) {
        _keyBoardHook = value;
    }

    private boolean unuseRealChip;

    public boolean getUnuseRealChip() {
        return unuseRealChip;
    }

    public void setUnuseRealChip(boolean value) {
        unuseRealChip = value;
    }

    private KeyBoardHook _keyBoardHook = new KeyBoardHook();

    public static class OutputDevice implements Serializable, Cloneable {

        private int deviceType = 1;
        public int getDeviceType() {
            return deviceType;
        }
        public void setDeviceType(int value) {
            deviceType = value;
        }
        private int latency = 300;
        public int getLatency() {
            return latency;
        }
        public void setLatency(int value) {
            latency = value;
        }
        private int waitTime = 500;
        public int getWaitTime() {
            return waitTime;
        }
        public void setWaitTime(int value) {
            waitTime = value;
        }
        private String waveOutDeviceName = "";
        public String getWaveOutDeviceName() {
            return waveOutDeviceName;
        }
        public void setWaveOutDeviceName(String value) {
            waveOutDeviceName = value;
        }
        private String directSoundDeviceName = "";
        public String getDirectSoundDeviceName() {
            return directSoundDeviceName;
        }
        public void setDirectSoundDeviceName(String value) {
            directSoundDeviceName = value;
        }
        private String wasapiDeviceName = "";
        public String getWasapiDeviceName() {
            return wasapiDeviceName;
        }
        public void setWasapiDeviceName(String value) {
            wasapiDeviceName = value;
        }
        private boolean wasapiShareMode = true;
        public boolean getWasapiShareMode() {
            return wasapiShareMode;
        }
        public void setWasapiShareMode(boolean value) {
            wasapiShareMode = value;
        }
        private String asioDeviceName = "";
        public String getAsioDeviceName() {
            return asioDeviceName;
        }
        public void setAsioDeviceName(String value) {
            asioDeviceName = value;
        }
        private int sampleRate = 44100;
        public int getSampleRate() {
            return sampleRate;
        }
        public void setSampleRate(int value) {
            sampleRate = value;
        }

        @Override
        public OutputDevice clone() {
            OutputDevice outputDevice = new OutputDevice();
            outputDevice.deviceType = this.deviceType;
            outputDevice.latency = this.latency;
            outputDevice.waitTime = this.waitTime;
            outputDevice.waveOutDeviceName = this.waveOutDeviceName;
            outputDevice.directSoundDeviceName = this.directSoundDeviceName;
            outputDevice.wasapiDeviceName = this.wasapiDeviceName;
            outputDevice.wasapiShareMode = this.wasapiShareMode;
            outputDevice.asioDeviceName = this.asioDeviceName;
            outputDevice.sampleRate = this.sampleRate;

            return outputDevice;
        }
    }

    public static class Other implements Serializable, Cloneable {
        private boolean useLoopTimes = true;
        public boolean getUseLoopTimes() {
            return useLoopTimes;
        }
        public void setUseLoopTimes(boolean value) {
            useLoopTimes = value;
        }
        private int loopTimes = 2;
        public int getLoopTimes() {
            return loopTimes;
        }
        public void setLoopTimes(int value) {
            loopTimes = value;
        }
        private boolean useGetInst = true;
        public boolean getUseGetInst() {
            return useGetInst;
        }
        public void setUseGetInst(boolean value) {
            useGetInst = value;
        }
        private String defaultDataPath = "";
        public String getDefaultDataPath() {
            return defaultDataPath;
        }
        public void setDefaultDataPath(String value) {
            defaultDataPath = value;
        }
        private EnmInstFormat instFormat = EnmInstFormat.MML2VGM;
        public EnmInstFormat getInstFormat() {
            return instFormat;
        }
        public void setInstFormat(EnmInstFormat value) {
            instFormat = value;
        }
        private int zoom = 2;
        public int getZoom() {
            return zoom;
        }
        public void setZoom(int value) {
            zoom = value;
        }
        private int screenFrameRate = 60;
        public int getScreenFrameRate() {
            return screenFrameRate;
        }
        public void setScreenFrameRate(int value) {
            screenFrameRate = value;
        }
        private boolean autoOpen = false;
        public boolean getAutoOpen() {
            return autoOpen;
        }
        public void setAutoOpen(boolean value) {
            autoOpen = value;
        }
        private boolean dumpSwitch = false;
        public boolean getDumpSwitch() {
            return dumpSwitch;
        }
        public void setDumpSwitch(boolean value) {
            dumpSwitch = value;
        }
        private String dumpPath = "";
        public String getDumpPath() {
            return dumpPath;
        }
        public void setDumpPath(String value) {
            dumpPath = value;
        }
        private boolean wavSwitch = false;
        public boolean getWavSwitch() {
            return wavSwitch;
        }
        public void setWavSwitch(boolean value) {
            wavSwitch = value;
        }
        private String wavPath = "";
        public String getWavPath() {
            return wavPath;
        }
        public void setWavPath(String value) {
            wavPath = value;
        }
        private int filterIndex = 0;
        public int getFilterIndex() {
            return filterIndex;
        }
        public void setFilterIndex(int value) {
            filterIndex = value;
        }
        private String textExt = "txt;doc;hed";
        public String getTextExt() {
            return textExt;
        }
        public void setTextExt(String value) {
            textExt = value;
        }
        private String mmlExt = "mml;gwi;muc;mdl";
        public String getMMLExt() {
            return mmlExt;
        }
        public void setMMLExt(String value) {
            mmlExt = value;
        }
        private String imageExt = "jpg;gif;png;mag";
        public String getImageExt() {
            return imageExt;
        }
        public void setImageExt(String value) {
            imageExt = value;
        }
        private boolean autoOpenText = false;
        public boolean getAutoOpenText() {
            return autoOpenText;
        }
        public void setAutoOpenText(boolean value) {
            autoOpenText = value;
        }
        private boolean autoOpenMML = false;
        public boolean getAutoOpenMML() {
            return autoOpenMML;
        }
        public void setAutoOpenMML(boolean value) {
            autoOpenMML = value;
        }
        private boolean autoOpenImg = false;
        public boolean getAutoOpenImg() {
            return autoOpenImg;
        }
        public void setAutoOpenImg(boolean value) {
            autoOpenImg = value;
        }
        private boolean initAlways = false;
        public boolean getInitAlways() {
            return initAlways;
        }
        public void setInitAlways(boolean value) {
            initAlways = value;
        }
        private boolean emptyPlayList = false;
        public boolean getEmptyPlayList() {
            return emptyPlayList;
        }
        public void setEmptyPlayList(boolean value) {
            emptyPlayList = value;
        }
        boolean exAll = false;
        public boolean getExAll() {
            return exAll;
        }
        public void setExAll(boolean value) {
            exAll = value;
        }
        boolean nonRenderingForPause = false;
        public boolean getNonRenderingForPause() {
            return nonRenderingForPause;
        }
        public void setNonRenderingForPause(boolean value) {
            nonRenderingForPause = value;
        }

        private boolean saveCompiledFile = false;
        public boolean getSaveCompiledFile() { return saveCompiledFile; }

        public void setSaveCompiledFile(boolean value) { saveCompiledFile = value; }

        @Override
        public Other clone() {
            Other other = new Other();
            other.useLoopTimes = this.useLoopTimes;
            other.loopTimes = this.loopTimes;
            other.useGetInst = this.useGetInst;
            other.defaultDataPath = this.defaultDataPath;
            other.instFormat = this.instFormat;
            other.zoom = this.zoom;
            other.screenFrameRate = this.screenFrameRate;
            other.autoOpen = this.autoOpen;
            other.dumpSwitch = this.dumpSwitch;
            other.dumpPath = this.dumpPath;
            other.wavSwitch = this.wavSwitch;
            other.wavPath = this.wavPath;
            other.filterIndex = this.filterIndex;
            other.textExt = this.textExt;
            other.mmlExt = this.mmlExt;
            other.imageExt = this.imageExt;
            other.autoOpenText = this.autoOpenText;
            other.autoOpenMML = this.autoOpenMML;
            other.autoOpenImg = this.autoOpenImg;
            other.initAlways = this.initAlways;
            other.emptyPlayList = this.emptyPlayList;
            other.exAll = this.exAll;
            other.nonRenderingForPause = this.nonRenderingForPause;

            return other;
        }
    }

    @Serdes(beanBinder = JacksonXMLBeanBinder.class)
    @JsonSerialize(using = Balance.BalanceSerializer.class)
    @JsonDeserialize(using = Balance.BalanceDeserializer.class)
    public static class Balance implements Serializable, Cloneable {

        private int masterVolume = 0;

        public int getMasterVolume() {
            if (masterVolume > 20 || masterVolume < -192)
                masterVolume = 0;
            return masterVolume;
        }

        public void setMasterVolume(int value) {
            masterVolume = value;
            if (masterVolume > 20 || masterVolume < -192)
                masterVolume = 0;
        }

        private int midiVolume = 0;

        /**
         * Attenuation of the MIDI path, in the same 2&times;dB unit as the chip volumes.
         * <p>
         * A song played through {@link mdplayer.chips.MidiPlugin} (a MIDI-only ZMS, RCP, MID)
         * renders no samples into the mixer, so neither the per-chip volumes nor
         * {@link #getMasterVolume} -- which {@code Audio.render} applies to the mixer output --
         * reach it: it plays at whatever level the synthesizer at the other end runs at. This is
         * the one slot that does reach it, applied to the channel volume controller the plugin
         * sends. It is per driver preset like every other volume here, because how loud the MIDI
         * side is relative to the chip side is a property of the driver's songs.
         */
        public int getMidiVolume() {
            if (outRange(midiVolume)) midiVolume = 0;
            return midiVolume;
        }

        public void setMidiVolume(int value) {
            midiVolume = outRange(value) ? 0 : value;
        }

        private final Map<String, Integer> masterVolumeSubs = new TreeMap<>();

        /**
         * A master volume for one of the sound sources a driver plays its songs on, in the same
         * 2&times;dB unit, on top of {@link #getMasterVolume}.
         * <p>
         * Some drivers render their songs themselves on one of several sound sources, picked per
         * song (the mfi driver: a synthesizer per chip), which are far apart in level and which
         * the mixer can't tell apart, as no chip of it plays them. This levels them against each
         * other, applied by the driver, before its output is clamped to 16 bits.
         *
         * @param type {@code "group:variant"}, the driver's {@link mdplayer.driver.MasterVolumeSub#getMasterVolumeSubType()},
         *        {@code "yamaha:ma7"}
         * @return 0 for a type without one
         */
        public int getMasterVolumeSub(String type) {
            return masterVolumeSubs.getOrDefault(type, 0);
        }

        public void setMasterVolumeSub(String type, int value) {
            masterVolumeSubs.put(type, outRange(value) ? 0 : value);
        }

        /** every {@link #getMasterVolumeSub} set, by type */
        public Map<String, Integer> getMasterVolumeSubs() {
            return Collections.unmodifiableMap(masterVolumeSubs);
        }

        private final Map<String, Integer> volumes = new HashMap<>();

        private static boolean outRange(int v) {
            return v > 20 || v < -192;
        }

        private static String getKey(String tag, Class<? extends Chip> c) {
            return c.getSimpleName().replaceFirst("Inst$", ".") + tag;
        }

        public int getVolume(String tag, Class<? extends Chip> c) {
            return volumes.getOrDefault(getKey(tag, c), 0);
        }

        public void setVolume(String tag, Class<? extends Chip> c, int v) {
            volumes.put(getKey(tag, c), outRange(v) ? 0 : v );
        }

        /**
         * Maps an XML element name (as used in {@code DefaultVolumeBalance_*.xml} / {@code *.mbc})
         * to the runtime {@code (chipClass, tag)} pair that keys the {@link #volumes} map.
         * This is the bridge that was missing (the per-chip getters/setters below were commented
         * out), so before this, {@code <YM2612Volume>} and friends deserialized into nothing.
         *
         * @param element XML element name
         * @param chip chip class (its simple name feeds {@link #getKey})
         * @param tag mixer tag, e.g. {@code MAIN}/{@code FM}/{@code SSG}
         */
        record VolEntry(String element, Class<? extends Chip> chip, String tag) {}

        /** same value as {@code mdsound.MDSound.Chip.MAIN_TAG}; plugins key single-part chips with it */
        private static final String MAIN = MDSound.Chip.MAIN_TAG;

        /** authoritative, ordered element &harr; (chip, tag) table; enumerated from every
         *  {@code getBalance().getVolume(tag, X.class)} call site across the plugins. */
        static final List<VolEntry> VOL_TABLE = List.of(
            new VolEntry("YM2612Volume", mdplayer.chips.Ym2612Chip.class, MAIN),
            new VolEntry("SN76489Volume", mdplayer.chips.Sn76489Chip.class, MAIN),
            new VolEntry("RF5C68Volume", mdplayer.chips.Rf5C68Chip.class, MAIN),
            new VolEntry("RF5C164Volume", mdplayer.chips.Rf5C164Chip.class, MAIN),
            new VolEntry("PWMVolume", mdplayer.chips.PwmChip.class, MAIN),
            new VolEntry("C140Volume", mdplayer.chips.C140Chip.class, MAIN),
            new VolEntry("OKIM6258Volume", mdplayer.chips.OkiM6258Chip.class, MAIN),
            new VolEntry("OKIM6295Volume", mdplayer.chips.OkiM6295Chip.class, MAIN),
            new VolEntry("SEGAPCMVolume", mdplayer.chips.SegaPcmChip.class, MAIN),
            new VolEntry("AY8910Volume", mdplayer.chips.Ay8910Chip.class, MAIN),
            new VolEntry("YM2413Volume", mdplayer.chips.Ym2413Chip.class, MAIN),
            new VolEntry("HuC6280Volume", mdplayer.chips.HuC6280Chip.class, MAIN),
            new VolEntry("YM2151Volume", mdplayer.chips.Ym2151Chip.class, MAIN),
            new VolEntry("YM2608Volume", mdplayer.chips.Ym2608Chip.class, MAIN),
            new VolEntry("YM2608FMVolume", mdplayer.chips.Ym2608Chip.class, "FM"),
            new VolEntry("YM2608PSGVolume", mdplayer.chips.Ym2608Chip.class, "SSG"),
            new VolEntry("YM2608RhythmVolume", mdplayer.chips.Ym2608Chip.class, "RHYTHM"),
            new VolEntry("YM2608AdpcmVolume", mdplayer.chips.Ym2608Chip.class, "ADPCM"),
            new VolEntry("YM2203Volume", mdplayer.chips.Ym2203Chip.class, MAIN),
            new VolEntry("YM2203FMVolume", mdplayer.chips.Ym2203Chip.class, "FM"),
            new VolEntry("YM2203PSGVolume", mdplayer.chips.Ym2203Chip.class, "PSG"),
            new VolEntry("YM2610Volume", mdplayer.chips.Ym2610Chip.class, MAIN),
            new VolEntry("YM2610FMVolume", mdplayer.chips.Ym2610Chip.class, "FM"),
            new VolEntry("YM2610PSGVolume", mdplayer.chips.Ym2610Chip.class, "SSG"),
            new VolEntry("YM2610AdpcmAVolume", mdplayer.chips.Ym2610Chip.class, "ADPCMA"),
            new VolEntry("YM2610AdpcmBVolume", mdplayer.chips.Ym2610Chip.class, "ADPCMB"),
            new VolEntry("C352Volume", mdplayer.chips.C352Chip.class, MAIN),
            new VolEntry("K054539Volume", mdplayer.chips.K054539Chip.class, MAIN),
            new VolEntry("K053260Volume", mdplayer.chips.K053260Chip.class, MAIN),
            new VolEntry("K051649Volume", mdplayer.chips.K051649Chip.class, MAIN),
            new VolEntry("APUVolume", mdplayer.chips.NesChip.class, MAIN),
            new VolEntry("DMCVolume", mdplayer.chips.NpNesChip.DmcChip.class, MAIN),
            new VolEntry("FDSVolume", mdplayer.chips.NpNesChip.FdsChip.class, MAIN),
            new VolEntry("MMC5Volume", mdplayer.chips.NpNesChip.Mmc5Chip.class, MAIN),
            new VolEntry("N160Volume", mdplayer.chips.NpNesChip.N163Chip.class, MAIN),
            new VolEntry("VRC6Volume", mdplayer.chips.NpNesChip.Vrc6Chip.class, MAIN),
            new VolEntry("VRC7Volume", mdplayer.chips.NpNesChip.Vrc7Chip.class, MAIN),
            new VolEntry("FME7Volume", mdplayer.chips.NpNesChip.Fme7Chip.class, MAIN),
            new VolEntry("NpNesVolume", mdplayer.chips.NpNesChip.class, MAIN),
            new VolEntry("DMGVolume", mdplayer.chips.DmgChip.class, MAIN),
            new VolEntry("GA20Volume", mdplayer.chips.Ga20Chip.class, MAIN),
            new VolEntry("YMZ280BVolume", mdplayer.chips.YmZ280BChip.class, MAIN),
            new VolEntry("YMF271Volume", mdplayer.chips.YmF271Chip.class, MAIN),
            new VolEntry("YMF262Volume", mdplayer.chips.YmF262Chip.class, MAIN),
            new VolEntry("YMF278BVolume", mdplayer.chips.YmF278BChip.class, MAIN),
            new VolEntry("MultiPCMVolume", mdplayer.chips.MultiPcmChip.class, MAIN),
            new VolEntry("QSoundVolume", mdplayer.chips.QSoundChip.class, MAIN),
            new VolEntry("Y8950Volume", mdplayer.chips.Y8950Chip.class, MAIN),
            new VolEntry("YM3526Volume", mdplayer.chips.Ym3526Chip.class, MAIN),
            new VolEntry("YM3812Volume", mdplayer.chips.Ym3812Chip.class, MAIN),
            new VolEntry("PPZ8Volume", mdplayer.chips.Ppz8Chip.class, MAIN),
            new VolEntry("SAA1099Volume", mdplayer.chips.Saa1099Chip.class, MAIN),
            new VolEntry("Cs4231Volume", mdplayer.chips.Cs4231Chip.class, MAIN),
            new VolEntry("Es5503Volume", mdplayer.chips.Es5503Chip.class, MAIN),
            new VolEntry("PokeyVolume", mdplayer.chips.PokeyChip.class, MAIN),
            new VolEntry("Upd7759Volume", mdplayer.chips.Upd7759Chip.class, MAIN),
            new VolEntry("WSwanVolume", mdplayer.chips.WSwanChip.class, MAIN),
            new VolEntry("X1_010Volume", mdplayer.chips.X1_010Chip.class, MAIN),
            new VolEntry("PCM8Volume", mdplayer.chips.Pcm8Chip.class, MAIN),
            new VolEntry("MPCMVolume", mdplayer.chips.MPcmChip.class, MAIN),
            new VolEntry("P86Volume", mdplayer.chips.P86Chip.class, MAIN),
            new VolEntry("PPSVolume", mdplayer.chips.PpsChip.class, MAIN),
            new VolEntry("SidVolume", mdplayer.chips.SidChip.class, MAIN),
            new VolEntry("ZxBeepVolume", mdplayer.chips.ZxBeepChip.class, MAIN)
        );

        private static final Map<String, VolEntry> VOL_BY_ELEMENT = new HashMap<>();
        static {
            for (VolEntry e : VOL_TABLE) VOL_BY_ELEMENT.put(e.element(), e);
        }

        /** every chip class that has a persistable balance slot (for calibration coverage checks) */
        public static List<? extends Class<? extends Chip>> knownChipClasses() {
            return VOL_TABLE.stream().map(VolEntry::chip).distinct().toList();
        }

        /** writes the flat {@code <Balance>} document (MasterVolume, every chip, Gimic) into the runtime map. */
        static class BalanceSerializer extends ValueSerializer<Balance> {
            @Override
            public void serialize(Balance b, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                gen.writeStartObject();
                gen.writeNumberProperty("MasterVolume", b.getMasterVolume());
                gen.writeNumberProperty("MidiVolume", b.getMidiVolume());
                // <MasterVolumeSub type="yamaha:ma7">-28</MasterVolumeSub>, one per type
                for (var e : b.masterVolumeSubs.entrySet()) {
                    gen.writeName("MasterVolumeSub");
                    gen.writeStartObject();
                    if (gen instanceof ToXmlGenerator x) x.setNextIsAttribute(true);
                    gen.writeStringProperty("type", e.getKey());
                    if (gen instanceof ToXmlGenerator x) {
                        x.setNextIsAttribute(false);
                        x.setNextIsUnwrapped(true);
                    }
                    gen.writeNumberProperty("value", e.getValue());
                    gen.writeEndObject();
                }
                for (VolEntry e : VOL_TABLE) {
                    gen.writeNumberProperty(e.element(), b.volumes.getOrDefault(getKey(e.tag(), e.chip()), 0));
                }
                gen.writeNumberProperty("GimicOPNVolume", b.getGimicOPNVolume());
                gen.writeNumberProperty("GimicOPNAVolume", b.getGimicOPNAVolume());
                gen.writeEndObject();
            }
        }

        /** routes each {@code <XxxVolume>} element back through the table into the runtime map. */
        static class BalanceDeserializer extends ValueDeserializer<Balance> {
            @Override
            public Balance deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                Balance b = new Balance();
                if (p.currentToken() == JsonToken.START_OBJECT) {
                    while (p.nextToken() != JsonToken.END_OBJECT) {
                        String name = p.currentName();
                        p.nextToken();
                        switch (name) {
                        case "MasterVolume", "masterVolume" -> b.setMasterVolume(p.getValueAsInt());
                        case "MidiVolume", "midiVolume" -> b.setMidiVolume(p.getValueAsInt());
                        case "MasterVolumeSub" -> readMasterVolumeSub(p, b);
                        case "GimicOPNVolume" -> b.setGimicOPNVolume(p.getValueAsInt());
                        case "GimicOPNAVolume" -> b.setGimicOPNAVolume(p.getValueAsInt());
                        default -> {
                            VolEntry e = VOL_BY_ELEMENT.get(name);
                            if (e != null) {
                                int v = p.getValueAsInt();
                                b.volumes.put(getKey(e.tag(), e.chip()), outRange(v) ? 0 : v);
                            } else {
                                p.skipChildren();
                            }
                        }
                        }
                    }
                }
                return b;
            }
        }

        /** {@code type} is the attribute, the value the element's text ({@code ""}), or {@code value} elsewhere than in xml */
        private static void readMasterVolumeSub(JsonParser p, Balance b) {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                p.skipChildren();
                return;
            }
            String type = null;
            Integer value = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String name = p.currentName();
                p.nextToken();
                switch (name) {
                case "type" -> type = p.getValueAsString();
                case "", "value" -> value = p.getValueAsInt();
                default -> p.skipChildren();
                }
            }
            if (type != null && value != null) b.setMasterVolumeSub(type, value);
        }

        private int _GimicOPNVolume = 0;

        public int getGimicOPNVolume() {
            if (_GimicOPNVolume > 127 || _GimicOPNVolume < 0)
                _GimicOPNVolume = 30;
            return _GimicOPNVolume;
        }

        public void setGimicOPNVolume(int value) {
            _GimicOPNVolume = value;
            if (_GimicOPNVolume > 127 || _GimicOPNVolume < 0)
                _GimicOPNVolume = 30;
        }

        private int _GimicOPNAVolume = 0;

        public int getGimicOPNAVolume() {
            if (_GimicOPNAVolume > 127 || _GimicOPNAVolume < 0)
                _GimicOPNAVolume = 30;
            return _GimicOPNAVolume;
        }

        public void setGimicOPNAVolume(int value) {
            _GimicOPNAVolume = value;
            if (_GimicOPNAVolume > 127 || _GimicOPNAVolume < 0)
                _GimicOPNAVolume = 30;
        }

        @Override
        public Balance clone() {
            Balance balance = new Balance();
            balance.masterVolume = this.masterVolume;
            balance.midiVolume = this.midiVolume;
            balance.masterVolumeSubs.putAll(this.masterVolumeSubs);
            balance.volumes.putAll(this.volumes);

            balance._GimicOPNVolume = this._GimicOPNVolume;
            balance._GimicOPNAVolume = this._GimicOPNAVolume;

            return balance;
        }

        public void save(Path fullPath) {
            try (OutputStream out = Files.newOutputStream(fullPath)) {
                Serdes.Util.serialize(this, out);
            } catch (IOException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        }

        public static Balance load(Path fullPath) {
            if (!Files.exists(fullPath))
                return null;
            try (InputStream in = Files.newInputStream(fullPath)) {
                return load(in);
            } catch (IOException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
                return null;
            }
        }

        /** reads a balance out of an already-open stream, e.g. a bundled preset on the classpath */
        public static Balance load(InputStream in) throws IOException {
            return Serdes.Util.deserialize(in, new Balance());
        }
    }

    public static class Location implements Serializable {
        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PMain = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPMain() {
            if (_PMain.x < 0 || _PMain.y < 0) {
                return new Point(0, 0);
            }
            return _PMain;
        }

        public void setPMain(Point value) {
            _PMain = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PInfo = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPInfo() {
            if (_PInfo.x < 0 || _PInfo.y < 0) {
                return new Point(0, 0);
            }
            return _PInfo;
        }

        public void setPInfo(Point value) {
            _PInfo = value;
        }

        private boolean _OInfo = false;

        public boolean getOInfo() {
            return _OInfo;
        }

        public void setOInfo(boolean value) {
            _OInfo = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PPlayList = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPPlayList() {
            if (_PPlayList.x < 0 || _PPlayList.y < 0) {
                return new Point(0, 0);
            }
            return _PPlayList;
        }

        public void setPPlayList(Point value) {
            _PPlayList = value;
        }

        private boolean _OPlayList = false;

        public boolean getOPlayList() {
            return _OPlayList;
        }

        public void setOPlayList(boolean value) {
            _OPlayList = value;
        }

        @JsonDeserialize(using = DimensionDeserializer.class)
        private Dimension _PPlayListWH = EmptyDimension;

        @JsonDeserialize(using = DimensionDeserializer.class)
        public Dimension getPPlayListWH() {
            if (_PPlayListWH.width < 0 || _PPlayListWH.height < 0) {
                return new Dimension(0, 0);
            }
            return _PPlayListWH;
        }

        public void setPPlayListWH(Dimension value) {
            _PPlayListWH = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PMixer = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPMixer() {
            if (_PMixer.x < 0 || _PMixer.y < 0) {
                return new Point(0, 0);
            }
            return _PMixer;
        }

        void setPMixer(Point value) {
            _PMixer = value;
        }

        private boolean _OMixer = false;

        public boolean getOMixer() {
            return _OMixer;
        }

        public void setOMixer(boolean value) {
            _OMixer = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PMixerWH = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPMixerWH() {
            if (_PMixerWH.x < 0 || _PMixerWH.y < 0) {
                return new Point(0, 0);
            }
            return _PMixerWH;
        }

        void setPMixerWH(Point value) {
            _PMixerWH = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PosMixer = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPosMixer() {
            return _PosMixer;
        }

        public void setPosMixer(Point value) {
            _PosMixer = value;
        }

        private boolean _OpenMixer = false;

        public boolean getOpenMixer() {
            return _OpenMixer;
        }

        void setOpenMixer(boolean value) {
            _OpenMixer = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PosVSTeffectList = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPosVSTeffectList() {
            return _PosVSTeffectList;
        }

        public void setPosVSTeffectList(Point value) {
            _PosVSTeffectList = value;
        }

        private boolean _OpenVSTeffectList = false;

        public boolean getOpenVSTeffectList() {
            return _OpenVSTeffectList;
        }

        public void setOpenVSTeffectList(boolean value) {
            _OpenVSTeffectList = value;
        }

        @JsonDeserialize(using = PointDeserializer.class)
        private Point _PosVisWave = EmptyPoint;

        @JsonDeserialize(using = PointDeserializer.class)
        public Point getPosVisWave() {
            return _PosVisWave;
        }

        public void setPosVisWave(Point value) {
            _PosVisWave = value;
        }

        private boolean _OpenVisWave = false;

        public boolean getOpenVisWave() {
            return _OpenVisWave;
        }

        public void setOpenVisWave(boolean value) {
            _OpenVisWave = value;
        }

        int _ChipSelect;

        public int getChipSelect() {
            return _ChipSelect;
        }

        public void setChipSelect(int value) {
            _ChipSelect = value;
        }

        // The values are flat strings ("x0,y0,x1,y1" / "open0,open1"), not arrays: the XML bean
        // binder round-trips Map<String, String> but not maps of arrays.

        /** saved window positions of provider views, id -> "x0,y0,x1,y1" */
        private Map<String, String> viewPos = new HashMap<>();

        public Map<String, String> getViewPos() {
            return viewPos;
        }

        public void setViewPos(Map<String, String> value) {
            viewPos = value;
        }

        /** which provider views were open on exit, id -> "open0,open1" */
        private Map<String, String> viewOpen = new HashMap<>();

        public Map<String, String> getViewOpen() {
            return viewOpen;
        }

        public void setViewOpen(Map<String, String> value) {
            viewOpen = value;
        }

        private static int[] ints(String s, int n) {
            int[] a = new int[n];
            if (s == null) return a;
            String[] parts = s.split(",");
            for (int i = 0; i < Math.min(n, parts.length); i++) {
                try {
                    a[i] = Integer.parseInt(parts[i].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            return a;
        }

        /** the saved position of a provider view, or null when it has none yet */
        public Point pos(String id, int chipId) {
            String s = viewPos.get(id);
            if (s == null) return null;
            int[] a = ints(s, 4);
            if (a[chipId * 2] <= 0 && a[chipId * 2 + 1] <= 0) return null;
            return new Point(a[chipId * 2], a[chipId * 2 + 1]);
        }

        public void setPos(String id, int chipId, Point p) {
            int[] a = ints(viewPos.get(id), 4);
            a[chipId * 2] = p.x;
            a[chipId * 2 + 1] = p.y;
            viewPos.put(id, a[0] + "," + a[1] + "," + a[2] + "," + a[3]);
        }

        /** whether a provider view was open when the player exited */
        public boolean isOpen(String id, int chipId) {
            String s = viewOpen.get(id);
            if (s == null) return false;
            String[] parts = s.split(",");
            return chipId < parts.length && Boolean.parseBoolean(parts[chipId].trim());
        }

        public void setOpen(String id, int chipId, boolean v) {
            boolean[] a = {isOpen(id, 0), isOpen(id, 1)};
            if (chipId < a.length) a[chipId] = v;
            viewOpen.put(id, a[0] + "," + a[1]);
        }

        public void clearOpen() {
            viewOpen.clear();
        }

        /** the main window's play mode: 0 in order, 1 at random, 2 all songs loop, 3 one song loop */
        private int playMode = 0;
        public int getPlayMode() {
            return playMode;
        }
        public void setPlayMode(int value) {
            playMode = value;
        }

        /** which titles the play list shows: "all", "en" or "ja" */
        private String playListLang = "all";
        public String getPlayListLang() {
            return playListLang;
        }
        public void setPlayListLang(String value) {
            playListLang = value;
        }

        /** the play list's column widths at x1, comma separated in column order, empty for the designer's */
        private String playListColumnWidths = "";
        public String getPlayListColumnWidths() {
            return playListColumnWidths;
        }
        public void setPlayListColumnWidths(String value) {
            playListColumnWidths = value;
        }

        @Override
        public Location clone() {
            Location location = new Location();

            location._PMain = this._PMain;
            location._PInfo = this._PInfo;
            location._OInfo = this._OInfo;
            location._PPlayList = this._PPlayList;
            location._OPlayList = this._OPlayList;
            location._PPlayListWH = this._PPlayListWH;
            location._PMixer = this._PMixer;
            location._OMixer = this._OMixer;
            location._PMixerWH = this._PMixerWH;
            location._PosMixer = this._PosMixer;
            location._OpenMixer = this._OpenMixer;
            location._PosVSTeffectList = this._PosVSTeffectList;
            location._OpenVSTeffectList = this._OpenVSTeffectList;
            location._PosVisWave = this._PosVisWave;
            location._OpenVisWave = this._OpenVisWave;
            location._ChipSelect = this._ChipSelect;

            location.viewPos = new HashMap<>(this.viewPos);
            location.viewOpen = new HashMap<>(this.viewOpen);
            location.playMode = this.playMode;
            location.playListLang = this.playListLang;
            location.playListColumnWidths = this.playListColumnWidths;

            return location;
        }
    }

    public static class NSF implements Serializable, Cloneable {
        private boolean _NESUnmuteOnReset = true;
        private boolean _NESNonLinearMixer = true;
        private boolean _NESPhaseRefresh = true;
        private boolean _NESDutySwap = false;
        private int _FDSLpf = 2000;
        private boolean _FDS4085Reset = false;
        private boolean _FDSWriteDisable8000 = true;
        private boolean _DMCUnmuteOnReset = true;
        private boolean _DMCNonLinearMixer = true;
        private boolean _DMCEnable4011 = true;
        private boolean _DMCEnablePnoise = true;
        private boolean _DMCDPCMAntiClick = false;
        private boolean _DMCRandomizeNoise = true;
        private boolean _DMCTRImute = true;
        private boolean _DMCTRINull = true;
        private boolean _MMC5NonLinearMixer = true;
        private boolean _MMC5PhaseRefresh = true;
        private boolean _N160Serial = false;

        public boolean getNESUnmuteOnReset() {
            return _NESUnmuteOnReset;
        }
        public void setNESUnmuteOnReset(boolean value) {
            _NESUnmuteOnReset = value;
        }
        public boolean getNESNonLinearMixer() {
            return _NESNonLinearMixer;
        }
        public void setNESNonLinearMixer(boolean value) {
            _NESNonLinearMixer = value;
        }
        public boolean getNESPhaseRefresh() {
            return _NESPhaseRefresh;
        }
        public void setNESPhaseRefresh(boolean value) {
            _NESPhaseRefresh = value;
        }
        public boolean getNESDutySwap() {
            return _NESDutySwap;
        }
        public void setNESDutySwap(boolean value) {
            _NESDutySwap = value;
        }
        public int getFDSLpf() {
            return _FDSLpf;
        }
        public void setFDSLpf(int value) {
            _FDSLpf = value;
        }
        public boolean getFDS4085Reset() {
            return _FDS4085Reset;
        }
        public void setFDS4085Reset(boolean value) {
            _FDS4085Reset = value;
        }
        public boolean getFDSWriteDisable8000() {
            return _FDSWriteDisable8000;
        }
        public void setFDSWriteDisable8000(boolean value) {
            _FDSWriteDisable8000 = value;
        }
        public boolean getDMCUnmuteOnReset() {
            return _DMCUnmuteOnReset;
        }
        public void setDMCUnmuteOnReset(boolean value) {
            _DMCUnmuteOnReset = value;
        }
        public boolean getDMCNonLinearMixer() {
            return _DMCNonLinearMixer;
        }
        public void setDMCNonLinearMixer(boolean value) {
            _DMCNonLinearMixer = value;
        }
        public boolean getDMCEnable4011() {
            return _DMCEnable4011;
        }
        public void setDMCEnable4011(boolean value) {
            _DMCEnable4011 = value;
        }
        public boolean getDMCEnablePnoise() {
            return _DMCEnablePnoise;
        }
        public void setDMCEnablePnoise(boolean value) {
            _DMCEnablePnoise = value;
        }
        public boolean getDMCDPCMAntiClick() {
            return _DMCDPCMAntiClick;
        }
        public void setDMCDPCMAntiClick(boolean value) {
            _DMCDPCMAntiClick = value;
        }
        public boolean getDMCRandomizeNoise() {
            return _DMCRandomizeNoise;
        }
        public void setDMCRandomizeNoise(boolean value) {
            _DMCRandomizeNoise = value;
        }
        public boolean getDMCTRImute() {
            return _DMCTRImute;
        }
        public void setDMCTRImute(boolean value) {
            _DMCTRImute = value;
        }
        public boolean getDMCTRINull() {
            return _DMCTRINull;
        }
        void setDMCTRINull(boolean value) {
            _DMCTRINull = value;
        }
        public boolean getMMC5NonLinearMixer() {
            return _MMC5NonLinearMixer;
        }
        public void setMMC5NonLinearMixer(boolean value) {
            _MMC5NonLinearMixer = value;
        }
        public boolean getMMC5PhaseRefresh() {
            return _MMC5PhaseRefresh;
        }
        public void setMMC5PhaseRefresh(boolean value) {
            _MMC5PhaseRefresh = value;
        }
        public boolean getN160Serial() {
            return _N160Serial;
        }
        public void setN160Serial(boolean value) {
            _N160Serial = value;
        }
        private int _HPF = 92;
        public int getHPF() {
            return _HPF;
        }
        public void setHPF(int value) {
            _HPF = value;
        }
        private int _LPF = 112;
        public int getLPF() {
            return _LPF;
        }
        public void setLPF(int value) {
            _LPF = value;
        }
        private boolean _DMCRandomizeTRI = false;
        public boolean getDMCRandomizeTRI() {
            return _DMCRandomizeTRI;
        }
        public void setDMCRandomizeTRI(boolean value) {
            _DMCRandomizeTRI = value;
        }
        private boolean _DMCDPCMReverse = false;
        public boolean getDMCDPCMReverse() {
            return _DMCDPCMReverse;
        }
        public void setDMCDPCMReverse(boolean value) {
            _DMCDPCMReverse = value;
        }

        @Override
        public NSF clone() {
            NSF nsf = new NSF();

            nsf._NESUnmuteOnReset = this._NESUnmuteOnReset;
            nsf._NESNonLinearMixer = this._NESNonLinearMixer;
            nsf._NESPhaseRefresh = this._NESPhaseRefresh;
            nsf._NESDutySwap = this._NESDutySwap;

            nsf._FDSLpf = this._FDSLpf;
            nsf._FDS4085Reset = this._FDS4085Reset;
            nsf._FDSWriteDisable8000 = this._FDSWriteDisable8000;

            nsf._DMCUnmuteOnReset = this._DMCUnmuteOnReset;
            nsf._DMCNonLinearMixer = this._DMCNonLinearMixer;
            nsf._DMCEnable4011 = this._DMCEnable4011;
            nsf._DMCEnablePnoise = this._DMCEnablePnoise;
            nsf._DMCDPCMAntiClick = this._DMCDPCMAntiClick;
            nsf._DMCRandomizeNoise = this._DMCRandomizeNoise;
            nsf._DMCTRImute = this._DMCTRImute;
            nsf._DMCRandomizeTRI = this._DMCRandomizeTRI;
            nsf._DMCDPCMReverse = this._DMCDPCMReverse;
            // nsf._DMCTRINull = this._DMCTRINull;

            nsf._MMC5NonLinearMixer = this._MMC5NonLinearMixer;
            nsf._MMC5PhaseRefresh = this._MMC5PhaseRefresh;

            nsf._N160Serial = this._N160Serial;

            nsf._HPF = this._HPF;
            nsf._LPF = this._LPF;

            return nsf;
        }
    }

    @Override
    public Setting clone() {
        Setting setting = new Setting();
        setting.outputDevice = this.outputDevice.clone();

        setting.ay8910Type = null;
        if (this.ay8910Type != null) {
            setting.ay8910Type = new ChipType2[this.ay8910Type.length];
            for (int i = 0; i < this.ay8910Type.length; i++)
                setting.ay8910Type[i] = this.ay8910Type[i].clone();
        }

        setting.k051649Type = null;
        if (this.k051649Type != null) {
            setting.k051649Type = new ChipType2[this.k051649Type.length];
            for (int i = 0; i < this.k051649Type.length; i++)
                setting.k051649Type[i] = this.k051649Type[i].clone();
        }

        setting.ym2151Type = null;
        if (this.ym2151Type != null) {
            setting.ym2151Type = new ChipType2[this.ym2151Type.length];
            for (int i = 0; i < this.ym2151Type.length; i++)
                setting.ym2151Type[i] = this.ym2151Type[i].clone();
        }

        setting.ym2203Type = null;
        if (this.ym2203Type != null) {
            setting.ym2203Type = new ChipType2[this.ym2203Type.length];
            for (int i = 0; i < this.ym2203Type.length; i++)
                setting.ym2203Type[i] = this.ym2203Type[i].clone();
        }

        setting.ym2413Type = null;
        if (this.ym2413Type != null) {
            setting.ym2413Type = new ChipType2[this.ym2413Type.length];
            for (int i = 0; i < this.ym2413Type.length; i++)
                setting.ym2413Type[i] = this.ym2413Type[i].clone();
        }

        setting.ym2608Type = null;
        if (this.ym2608Type != null) {
            setting.ym2608Type = new ChipType2[this.ym2608Type.length];
            for (int i = 0; i < this.ym2608Type.length; i++)
                setting.ym2608Type[i] = this.ym2608Type[i].clone();
        }

        setting.ym2610Type = null;
        if (this.ym2610Type != null) {
            setting.ym2610Type = new ChipType2[this.ym2610Type.length];
            for (int i = 0; i < this.ym2610Type.length; i++)
                setting.ym2610Type[i] = this.ym2610Type[i].clone();
        }

        setting._YM2612Type = null;
        if (this._YM2612Type != null) {
            setting._YM2612Type = new ChipType2[this._YM2612Type.length];
            for (int i = 0; i < this._YM2612Type.length; i++)
                setting._YM2612Type[i] = this._YM2612Type[i].clone();
        }

        setting._YM3526Type = null;
        if (this._YM3526Type != null) {
            setting._YM3526Type = new ChipType2[this._YM3526Type.length];
            for (int i = 0; i < this._YM3526Type.length; i++)
                setting._YM3526Type[i] = this._YM3526Type[i].clone();
        }

        setting._YM3812Type = null;
        if (this._YM3812Type != null) {
            setting._YM3812Type = new ChipType2[this._YM3812Type.length];
            for (int i = 0; i < this._YM3812Type.length; i++)
                setting._YM3812Type[i] = this._YM3812Type[i].clone();
        }

        setting.ymf262Type = null;
        if (this.ymf262Type != null) {
            setting.ymf262Type = new ChipType2[this.ymf262Type.length];
            for (int i = 0; i < this.ymf262Type.length; i++)
                setting.ymf262Type[i] = this.ymf262Type[i].clone();
        }

        setting._SN76489Type = null;
        if (this._SN76489Type != null) {
            setting._SN76489Type = new ChipType2[this._SN76489Type.length];
            for (int i = 0; i < this._SN76489Type.length; i++)
                setting._SN76489Type[i] = this._SN76489Type[i].clone();
        }

        setting._C140Type = null;
        if (this._C140Type != null) {
            setting._C140Type = new ChipType2[this._C140Type.length];
            for (int i = 0; i < this._C140Type.length; i++)
                setting._C140Type[i] = this._C140Type[i].clone();
        }

        setting._SEGAPCMType = null;
        if (this._SEGAPCMType != null) {
            setting._SEGAPCMType = new ChipType2[this._SEGAPCMType.length];
            for (int i = 0; i < this._SEGAPCMType.length; i++)
                setting._SEGAPCMType[i] = this._SEGAPCMType[i].clone();
        }

        setting.unuseRealChip = this.unuseRealChip;
        setting.fileSearchPathList = this.fileSearchPathList;

        setting.other = this.other.clone();
        setting.balance = this.balance.clone();
        setting.latencyEmulation = this.latencyEmulation;
        setting.latencySCCI = this.latencySCCI;
        setting.debugDispFrameCounter = this.debugDispFrameCounter;
        setting.hiyorimiMode = this.hiyorimiMode;
        setting.location = this.location.clone();
        setting.midiExport = this.midiExport.clone();
        setting.midiKbd = this.midiKbd.clone();
//        setting.vst = this.vst.clone();
        setting.midiOut = this.midiOut.clone();
        setting.nsf = this.nsf.clone();
        setting.sid = this.sid.clone();
        setting.nukedOPN2 = this.nukedOPN2.clone();
        setting.autoBalance = this.autoBalance.clone();
        setting.pmd = this.pmd.clone();

        setting._keyBoardHook = this._keyBoardHook.clone();

        return setting;
    }

    public void save() {
        Path fullPath = Common.settingFilePath;

        fullPath = fullPath.resolve(rb.getString("cntSettingFileName"));

        try (OutputStream sw = Files.newOutputStream(fullPath)) {
            Serdes.Util.serialize(this, sw);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /**
     * Reads the saved settings into the instance everything else already holds — the engine reads
     * {@link #getInstance()}, so loading into a separate object would leave the two disagreeing.
     */
    public static Setting load() {
        try {

            String fn = rb.getString("cntSettingFileName");
            String dir = System.getProperty("mdplayer.setting.dir");
            if (dir != null && !dir.isEmpty()) {
                // an explicit folder, e.g. the tests keep theirs off the user's own settings
                Common.settingFilePath = Path.of(dir);
                Files.createDirectories(Common.settingFilePath);
            } else if (Files.exists(Path.of(System.getProperty("user.dir"), fn))) {
                // If there is a configuration file in the same folder as the application, use that.
                Common.settingFilePath = Path.of(System.getProperty("user.dir"));
            } else {
                // For anything other than the above, use the application data folder.
                Common.settingFilePath = Common.getApplicationDataFolder(true);
            }

            Path fullPath = Common.settingFilePath;
            if (fullPath == null) {

                fullPath = Path.of(rb.getString("cntSettingFileName"));
            } else {

                fullPath = fullPath.resolve(rb.getString("cntSettingFileName"));
            }

            if (Files.exists(fullPath) && Files.size(fullPath) > 10) {
                try (InputStream sr = Files.newInputStream(fullPath)) {
                    // the binder never fills the bean it is handed — it builds a fresh one, so the
                    // loaded state has to be moved into the instance everything already holds
                    Setting loaded = Serdes.Util.deserialize(sr, instance);
                    if (loaded != null && loaded != instance) {
                        copyInto(loaded, instance);
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }

        // fills in whatever the settings file did not carry, and everything on a first run
        instance.init();
        return instance;
    }

    /** Moves a freshly deserialized Setting's state into the singleton, field by field. */
    private static void copyInto(Setting from, Setting to) {
        for (Field f : Setting.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                f.set(to, f.get(from));
            } catch (ReflectiveOperationException e) {
                logger.log(Level.WARNING, "could not restore setting field " + f.getName() + ": " + e);
            }
        }
    }

    public static class PointDeserializer extends ValueDeserializer<Point> {
        @Override
        public Point deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            double x = 0;
            double y = 0;
            if (p.currentToken() == JsonToken.START_OBJECT) {
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String name = p.currentName();
                    p.nextToken();
                    if ("x".equals(name)) {
                        x = p.getValueAsDouble();
                    } else if ("y".equals(name)) {
                        y = p.getValueAsDouble();
                    } else {
                        p.skipChildren();
                    }
                }
            }
            return new Point((int) Math.round(x), (int) Math.round(y));
        }
    }

    private static class DimensionDeserializer extends ValueDeserializer<Dimension> {
        @Override
        public Dimension deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            double width = 0;
            double height = 0;
            if (p.currentToken() == JsonToken.START_OBJECT) {
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String name = p.currentName();
                    p.nextToken();
                    if ("width".equals(name)) {
                        width = p.getValueAsDouble();
                    } else if ("height".equals(name)) {
                        height = p.getValueAsDouble();
                    } else {
                        p.skipChildren();
                    }
                }
            }
            return new Dimension((int) Math.round(width), (int) Math.round(height));
        }
    }

    /**
     * Which of the two X68000 PCM back ends the song being played uses: {@code 0} is X68Sound's own
     * PCM8, {@code 1} is PCM8PP.
     * <p>
     * {@link Pcm8Chip#AUTO} is resolved here, so nobody downstream ever sees it: for an MDX the
     * song itself says which one it needs ({@link MDXPlugin#detectedPcm8Type()}), ZMUSIC and RCS
     * have nothing like that and keep PCM8PP, their default before there was an auto.
     * <p>
     * The chips are shared singletons - one {@link mdplayer.chips.Pcm8Chip} serves ZMS, MDX and RCS
     * alike - while the setting is per driver, so the chip cannot read a section of its own. It asks
     * the plugin that registered it instead, and the plugin that answers is the one whose
     * {@code initChips} chose which instrument to put there: the two can no longer disagree, which
     * they silently did while the chip read ZMUSIC's section for every driver.
     *
     * @see mdplayer.chips.Pcm8Chip#activeIndex
     */
    public int pcm8Type(Plugin plugin) {
        return switch (plugin) {
            case MDXPlugin p -> mxDrv.pcm8Type == Pcm8Chip.AUTO ? p.detectedPcm8Type() : mxDrv.pcm8Type;
            case RCSPlugin _ -> rcs.pcm8type == Pcm8Chip.AUTO ? Pcm8Chip.PCM8PP : rcs.pcm8type;
            default -> zMusic.pcm8Type == Pcm8Chip.AUTO ? Pcm8Chip.PCM8PP : zMusic.pcm8Type;
        };
    }

    /** the PCM8PP option {@link #pcm8Type} {@code 1} is started with; {@code -1} is its default */
    public int pcm8ppsOption(Plugin plugin) {
        return switch (plugin) {
            case MDXPlugin _ -> mxDrv.pcm8ppsOption;
            case RCSPlugin _ -> rcs.pcm8ppsOption;
            default -> zMusic.pcm8ppsOption;
        };
    }

    /**
     * Which of the two MPCM back ends the song being played uses: {@code 0} is X68Sound's own MPCM,
     * {@code 1} is MPCMPP. The same sharing as {@link #pcm8Type}, for {@link mdplayer.chips.MPcmChip}.
     */
    public int mpcmType(Plugin plugin) {
        return switch (plugin) {
            case MNDPlugin _ -> mnDrv.mpcmType;
            default -> zMusic.mpcmType;
        };
    }
}
