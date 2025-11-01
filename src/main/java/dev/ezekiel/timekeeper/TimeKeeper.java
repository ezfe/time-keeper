package dev.ezekiel.timekeeper;

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
    public static double LATITUDE = 42.26268;
    public static double LONGITUDE = -71.80249;

    private short counter = 0;

	@Override
	public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register((world) -> {
            if (world.isClientSide()) {
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

                Location l = new Location(LATITUDE, LONGITUDE);
                SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(l, tz.getID());

                Calendar nowCal = GregorianCalendar.getInstance();
                Calendar sunriseCal = calculator.getOfficialSunriseCalendarForDate(GregorianCalendar.getInstance());
                Calendar sunsetCal = calculator.getOfficialSunsetCalendarForDate(GregorianCalendar.getInstance());

                double sunrise = (double) (sunriseCal.get(Calendar.HOUR_OF_DAY) + ((double) sunriseCal.get(Calendar.MINUTE)) / 60);
                double sunset = (double) (sunsetCal.get(Calendar.HOUR_OF_DAY) + ((double) sunsetCal.get(Calendar.MINUTE)) / 60);
                double now = (double) (nowCal.get(Calendar.HOUR_OF_DAY) + ((double) nowCal.get(Calendar.MINUTE)) / 60);

                int mcTime;
                if (now < sunrise || now > sunset) {
                    double nightLength = (24 - sunset) + (sunrise);
                    double percentThroughNight = 0.0;
                    if (now > sunset) {
                        percentThroughNight = (now - sunset) / (nightLength);
                    } else {
                        percentThroughNight = (now + (24 - sunset)) / (nightLength);
                    }
                    mcTime = (int) (12000 + (12000 * percentThroughNight));
                } else {
                    double dayLength = sunset - sunrise;

                    double percentThroughDay = (now - sunrise) / (sunset - sunrise);

                    mcTime = (int) (12000 * percentThroughDay);
                }

                world.setDayTime(mcTime);
            }

//            if (world.getDayTime() % 20 == 0) {
//                LOGGER.info("Time is passing.");
//                world.set
//            }
        });
	}
}