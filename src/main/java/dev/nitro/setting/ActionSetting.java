package dev.nitro.setting;

/** A button in the settings pane. */
public class ActionSetting extends Setting<Boolean> {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name, false);
        this.action = action;
    }

    public void run() { action.run(); }
}
