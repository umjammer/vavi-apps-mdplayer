package mdplayer.form.kb;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import mdplayer.Audio;
import mdplayer.Chip;
import mdplayer.Setting;
import mdplayer.form.SettingTab;
import mdplayer.form.View;
import mdplayer.form.VisVolume;
import mdplayer.form.inst.InstWriter;
import mdplayer.form.sys.FormMain;
import mdsound.MDSound;


/**
 * What one chip contributes to the GUI, found via {@link ServiceLoader}.
 * <p>
 * Adding a chip's panel means implementing this next to the panel's form and registering it in
 * {@code META-INF/services} — the main window's menus, open/close bookkeeping, per-frame screen
 * updates and window-position persistence all iterate the provider list instead of naming chips.
 * A provider does not have to be chip-backed: panels like MIDI or the register-test window
 * simply have no {@link #chip()}.
 */
public interface ViewProvider {

    /** stable identifier; keys saved window state and names generated menu items */
    String id();

    /** caption of this view's menu item, e.g. "OPM" for the YM2151 */
    default String menuText() {
        return id();
    }

    /**
     * nested-menu category — the subpackage this view's form lived in before the kb package was
     * flattened ("psg", "opl", "opn", "opx", "pcm", "nes", "wf", "driver"); null = top level
     */
    default String category() {
        return null;
    }

    /** the chip this view shows, or null when the view is not tied to a chip (MIDI, RegTest ...) */
    default Class<? extends Chip> chip() {
        return null;
    }

    /** whether one view exists per chip instance (primary/secondary) or just one in total */
    default boolean perChip() {
        return true;
    }

    /** whether this view is offered in the open-other-panel menu */
    default boolean hasMenuItem() {
        return true;
    }

    /** whether this provider's chip is offered in the register-dump (RegTest) menu */
    default boolean hasRegisterDump() {
        return false;
    }

    /** creates the form; null when this provider has no panel of its own (SID: register dump only) */
    View create(FormMain frm, int chipId, int zoom);

    /** where a newly opened view lands relative to the main window */
    default Point defaultOffset() {
        return new Point(0, 264);
    }

    /** window title */
    default String title(int chipId) {
        return perChip() ? "%s (%s)".formatted(id(), chipId == 0 ? "Primary" : "Secondary") : id();
    }

    /** how many primary/secondary instances this view can have */
    default int instances() {
        return perChip() ? 2 : 1;
    }

    /** the pages this provider contributes to the settings dialog, in tab order */
    default List<SettingTab> settingTabs() {
        return List.of();
    }

    /**
     * The chips this provider answers for — usually just {@link #chip()}, but a panel may drive
     * more than one (the NES panel also shows the DMC).
     */
    default List<Class<? extends Chip>> chips() {
        return chip() == null ? List.of() : List.of(chip());
    }

    /**
     * One fader slot of the mixer window: where it sits on the mixer skin, which chip volume the
     * fader drives ({@code tag} as {@code mdsound.MDSound.Chip.MAIN_TAG} or a part name like "FM"),
     * and which key of the meter map feeds its level display at what damping.
     */
    record MixerSlot(int slot, String tag, Class<? extends Chip> chip, String visKey, int visDiv) {
    }

    /** the mixer fader slots this provider's chip owns; empty = not on the mixer */
    default List<MixerSlot> mixerSlots() {
        return List.of();
    }

    /**
     * Copies the tone of one of this chip's channels to the clipboard, in the instrument-text
     * format the user chose. The default asks {@link InstWriter#of} for the chosen format's
     * writer; chips with a format quirk of their own (OPLL and friends) override.
     */
    default void getInstCh(Component parent, Audio audio, Setting setting, int ch, int chipId) {
        InstWriter w = InstWriter.of(setting.getOther().getInstFormat());
        if (w != null) w.write(parent, audio, chip(), ch, chipId);
    }

    /**
     * Feeds this chip's level meters, writing the mixer's meter map under the keys its
     * {@link #mixerSlots()} declared. The default reads the chip's overall reported volume;
     * chips with part meters (FM/SSG/rhythm...) or their own level source override.
     */
    default void updateMeters(Audio audio, VisVolume visVolume) {
        for (MixerSlot s : mixerSlots()) {
            if (MDSound.Chip.MAIN_TAG.equals(s.tag())) {
                visVolume.put(s.visKey(), Meters.chipVolume(audio, s.chip()) * 5);
            }
        }
    }

    /** every registered provider, in registration order */
    static List<ViewProvider> providers() {
        return Holder.providers;
    }

    /** the provider registered under this {@link #id()} */
    static ViewProvider byId(String id) {
        for (ViewProvider p : Holder.providers) {
            if (p.id().equals(id)) return p;
        }
        throw new IllegalArgumentException("no such view provider: " + id);
    }

    /** loads once; the service file's order is the menu order */
    class Holder {

        private Holder() {
        }

        private static final List<ViewProvider> providers = new ArrayList<>();

        static {
            for (ViewProvider p : ServiceLoader.load(ViewProvider.class)) {
                providers.add(p);
            }
        }
    }
}
