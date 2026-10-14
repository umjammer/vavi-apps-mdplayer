package mdplayer.form;

import javax.swing.JPanel;

import mdplayer.Setting;


/**
 * One page of the settings dialog.
 * <p>
 * A tab builds its own controls, fills them from the {@link Setting} being edited, and writes
 * them back when the dialog is accepted. The dialog itself only collects tabs — its own and the
 * ones the {@link mdplayer.form.kb.ViewProvider}s contribute — so adding a chip never means
 * editing the dialog.
 * <p>
 * A tab lays its controls out itself, with layout managers. The captions still come from the
 * designer's resource bundle by component name, so a tab keeps the {@code tp...} name its page had
 * when all of this was one class.
 */
public abstract class SettingTab extends JPanel {

    /** the setting being edited — the dialog's working copy, set when the tab is registered */
    protected Setting setting;

    /**
     * Where this page sorts among the dialog's tabs; the dialog orders every collected tab —
     * its own and the providers' — by this, so a new tab picks a number between its neighbours.
     */
    public int order() {
        return Integer.MAX_VALUE;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    /**
     * Validates this page before the dialog is accepted; return false to keep the dialog open.
     * A tab may ask the user and let them continue anyway.
     */
    public boolean check() {
        return true;
    }

    /** controls &lt;- setting */
    public abstract void load(Setting setting);

    /** setting &lt;- controls */
    public abstract void apply(Setting setting);
}
