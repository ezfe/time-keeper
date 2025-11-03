package dev.ezekiel.timekeeper;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name="time-keeper")
public class TimeKeeperConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public double latitude = 42.26268;

    @ConfigEntry.Gui.Tooltip
    public double longitude = -71.80249;
}
