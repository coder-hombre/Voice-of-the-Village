package com.foogly.voiceofthevillage.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * Contains contextual information about the game state during a villager interaction.
 * Used to provide relevant context to AI services for generating appropriate responses.
 */
public class GameContext {
    private final Level level;
    private final BlockPos villagerPosition;
    private final long gameTime;
    private final long dayTime;
    private final boolean isDay;
    private final boolean isRaining;
    private final String biome;
    private final List<Entity> nearbyEntities;
    private final String dimensionName;

    /**
     * Creates a new game context instance.
     *
     * @param level            The world level
     * @param villagerPosition Position of the villager
     * @param gameTime         Current game time
     * @param dayTime          Current day time
     * @param isDay            Whether it's currently day
     * @param isRaining        Whether it's currently raining
     * @param biome            Name of the current biome
     * @param nearbyEntities   List of nearby entities
     * @param dimensionName    Name of the current dimension
     */
    public GameContext(Level level, BlockPos villagerPosition, long gameTime, long dayTime,
                      boolean isDay, boolean isRaining, String biome, List<Entity> nearbyEntities,
                      String dimensionName) {
        this.level = level;
        this.villagerPosition = villagerPosition;
        this.gameTime = gameTime;
        this.dayTime = dayTime;
        this.isDay = isDay;
        this.isRaining = isRaining;
        this.biome = biome;
        this.nearbyEntities = nearbyEntities;
        this.dimensionName = dimensionName;
    }

    /**
     * Creates a simplified game context with basic information.
     *
     * @param level            The world level
     * @param villagerPosition Position of the villager
     * @return Basic game context
     */
    public static GameContext basic(Level level, BlockPos villagerPosition) {
        long gameTime = level.getGameTime();
        long dayTime = level.getDayTime() % 24000L;
        boolean isDay = dayTime < 12000L;
        boolean isRaining = level.isRaining();
        String biome = level.getBiome(villagerPosition).toString();
        String dimensionName = level.dimension().location().toString();

        return new GameContext(level, villagerPosition, gameTime, dayTime, isDay, isRaining,
                             biome, List.of(), dimensionName);
    }

    /**
     * Gets a human-readable description of the current time of day.
     *
     * @return Time description (e.g., "morning", "afternoon", "evening", "night")
     */
    public String getTimeDescription() {
        if (dayTime < 3000L) {
            return "early morning";
        } else if (dayTime < 6000L) {
            return "morning";
        } else if (dayTime < 9000L) {
            return "midday";
        } else if (dayTime < 12000L) {
            return "afternoon";
        } else if (dayTime < 15000L) {
            return "evening";
        } else if (dayTime < 18000L) {
            return "dusk";
        } else {
            return "night";
        }
    }

    /**
     * Gets a description of the current weather.
     *
     * @return Weather description
     */
    public String getWeatherDescription() {
        if (isRaining) {
            if (level.isThundering()) {
                return "stormy";
            } else {
                return "rainy";
            }
        } else {
            return "clear";
        }
    }

    /**
     * Gets the current game day number.
     *
     * @return Day number since world creation
     */
    public long getGameDay() {
        return gameTime / 24000L;
    }

    // Getters
    public Level getLevel() {
        return level;
    }

    public BlockPos getVillagerPosition() {
        return villagerPosition;
    }

    public long getGameTime() {
        return gameTime;
    }

    public long getDayTime() {
        return dayTime;
    }

    public boolean isDay() {
        return isDay;
    }

    public boolean isRaining() {
        return isRaining;
    }

    public String getBiome() {
        return biome;
    }

    public List<Entity> getNearbyEntities() {
        return nearbyEntities;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameContext that = (GameContext) o;
        return gameTime == that.gameTime &&
               dayTime == that.dayTime &&
               isDay == that.isDay &&
               isRaining == that.isRaining &&
               Objects.equals(villagerPosition, that.villagerPosition) &&
               Objects.equals(biome, that.biome) &&
               Objects.equals(dimensionName, that.dimensionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(villagerPosition, gameTime, dayTime, isDay, isRaining, biome, dimensionName);
    }

    @Override
    public String toString() {
        return "GameContext{" +
               "villagerPosition=" + villagerPosition +
               ", gameDay=" + getGameDay() +
               ", timeDescription='" + getTimeDescription() + '\'' +
               ", weatherDescription='" + getWeatherDescription() + '\'' +
               ", biome='" + biome + '\'' +
               ", dimensionName='" + dimensionName + '\'' +
               '}';
    }
}