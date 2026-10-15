package dev.nitro.setting;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min; this.max = max; this.step = step;
    }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        this.value = Math.round(c / step) * step;
    }
}
