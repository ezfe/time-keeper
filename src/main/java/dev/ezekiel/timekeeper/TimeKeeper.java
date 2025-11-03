package dev.ezekiel.timekeeper;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeKeeper implements ModInitializer {
	public static final String MOD_ID = "time-keeper";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final TimeZone tz = Calendar.getInstance().getTimeZone();
    private static double sunrise = 0.0;
    private static double sunset = 0.0;

    private short counter = 0;

	@Override
	public void onInitialize() {
        AutoConfig.register(TimeKeeperConfig.class, JanksonConfigSerializer::new);
        TimeKeeperConfig config = AutoConfig.getConfigHolder(TimeKeeperConfig.class).getConfig();

        Location l = new Location(config.latitude, config.longitude);
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(l, tz.getID());
        Calendar sunriseCal = calculator.getOfficialSunriseCalendarForDate(GregorianCalendar.getInstance());
        Calendar sunsetCal = calculator.getOfficialSunsetCalendarForDate(GregorianCalendar.getInstance());
        sunrise = sunriseCal.get(Calendar.HOUR_OF_DAY) + ((double) sunriseCal.get(Calendar.MINUTE)) / 60;
        sunset = sunsetCal.get(Calendar.HOUR_OF_DAY) + ((double) sunsetCal.get(Calendar.MINUTE)) / 60;

        ServerTickEvents.END_WORLD_TICK.register((world) -> {
            if (world.isClientSide()) {
                LOGGER.info("Client side world tick skipping");
                return;
            }

            counter += 1;
            if (counter > 100) {
                counter = 0;

                GameRules gr = world.getGameRules();
                if (gr.getBoolean(GameRules.RULE_DAYLIGHT)) {
                    gr.getRule(GameRules.RULE_DAYLIGHT).set(false, world.getServer());
                    LOGGER.info("Daylight cycle stopped");
                }

                int mcTime = getMcTime();
                world.setDayTime(mcTime % 24000);
            }
        });
	}

    private static int getMcTime() {
        Calendar nowCal = GregorianCalendar.getInstance();
        double now = nowCal.get(Calendar.HOUR_OF_DAY) + ((double) nowCal.get(Calendar.MINUTE)) / 60;

        double mcTime;
        if (now < sunrise || now > sunset) {
            double nightLength = (24 - sunset) + sunrise;
            double percentThroughNight;
            if (now > sunset) {
                percentThroughNight = (now - sunset) / nightLength;
            } else {
                percentThroughNight = (now + (24 - sunset)) / nightLength;
            }
            mcTime = (12650 + (10700 * percentThroughNight));
        } else {
            double dayLength = sunset - sunrise;
            double percentThroughDay = (now - sunrise) / dayLength;
            mcTime = (12000 * percentThroughDay);
        }
        return (int) mcTime;
    }
}