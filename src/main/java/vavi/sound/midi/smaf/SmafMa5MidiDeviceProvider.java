/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.InputStream;
import java.util.Properties;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;


/**
 * SmafMa5MidiDeviceProvider.
 * <p>
 * The one device is {@link SmafMa5Synthesizer}, offered whether or not mmftool is on this
 * machine: that is said by {@link SmafMa5Synthesizer#open()}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
public class SmafMa5MidiDeviceProvider extends MidiDeviceProvider {

    static final String version;

    static {
        String v = "undefined";
        try (InputStream is = SmafMa5MidiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-apps-mdplayer/pom.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                v = props.getProperty("version", v);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        version = v;
    }

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return new MidiDevice.Info[] {SmafMa5Synthesizer.info};
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info) throws IllegalArgumentException {
        if (info == SmafMa5Synthesizer.info) {
            return new SmafMa5Synthesizer();
        }
        throw new IllegalArgumentException(String.valueOf(info));
    }
}
