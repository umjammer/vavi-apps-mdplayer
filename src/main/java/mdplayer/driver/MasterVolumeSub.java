/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.driver;

import java.util.List;
import java.util.Map;


/**
 * A driver that renders its songs itself on one of several sound sources, picked per song, which
 * are far apart in level: it levels them by the preset's
 * {@link mdplayer.Setting.Balance#getMasterVolumeSub(String) sub master volume} of the one playing
 * ({@code <MasterVolumeSub type="yamaha:ma7">}), and says which that is and which else there are,
 * so the calibration tool can measure every one of them on the same songs.
 * <p>
 * A type is {@code "group:variant"}: the songs of a group are the ones that could play on each
 * variant of it (the mfi driver: the chip, and a synthesizer that sounds as it).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-21 nsano initial version <br>
 */
public interface MasterVolumeSub {

    /** the type of the song loaded, {@code "yamaha:ma7"}, null before one is */
    String getMasterVolumeSubType();

    /** every type the song loaded could play on here, the one it does first */
    List<String> getMasterVolumeSubTypes();

    /** the system properties that make the song loaded next play on {@code type} */
    Map<String, String> masterVolumeSubProperties(String type);
}
