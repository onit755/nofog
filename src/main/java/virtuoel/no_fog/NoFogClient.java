package virtuoel.no_fog;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import virtuoel.no_fog.api.NoFogConfig;
import virtuoel.no_fog.util.AutoConfigUtils;
import virtuoel.no_fog.util.DummyNoFogConfig;
import virtuoel.no_fog.util.FogToggleType;
import virtuoel.no_fog.util.FogToggles;
import virtuoel.no_fog.util.ModLoaderUtils;
import virtuoel.no_fog.util.ReflectionUtils;
import virtuoel.no_fog.util.TagCompatibility;

public class NoFogClient implements ClientModInitializer
{
	public static final String MOD_ID = "no_fog";
	
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	public static final boolean CONFIGS_LOADED = ModLoaderUtils.isModLoaded("cloth_config") || ModLoaderUtils.isModLoaded("cloth-config") || ModLoaderUtils.isModLoaded("cloth-config2");
	
	public static final Supplier<NoFogConfig> CONFIG = !CONFIGS_LOADED ? () -> DummyNoFogConfig.INSTANCE : AutoConfigUtils.CONFIG;
	
	public NoFogClient()
	{
		
	}
	
	@Override
	public void onInitializeClient()
	{
		if (CONFIGS_LOADED)
		{
			AutoConfigUtils.initialize();
		}
		
		TagCompatibility.FluidTags.WATER.getClass();
	}
	
	public static final float FOG_START = -8.0F;
	public static final float FOG_END = 1_000_000.0F;
	
	public static float getFogDistance(FogToggleType type, Entity entity, float fogDistance, boolean start)
	{
		return isToggleEnabled(type, entity) ? fogDistance : start ? FOG_START : FOG_END;
	}
	
	private static boolean loggedError = false;
	
	public static boolean isToggleEnabled(FogToggleType type, Entity entity)
	{
		final String dimension = entity.getEntityWorld().getRegistryKey().getValue().toString();
		
		final NoFogConfig config = NoFogClient.CONFIG.get();
		final FogToggles globalToggles = config.getGlobalToggles();
		final FogToggles dimensionToggles = config.getDimensionToggles().computeIfAbsent(dimension, FogToggles::new);
		
		final String biomeId;
		try
		{
			biomeId = ReflectionUtils.getBiomeId(entity);
		}
		catch (Throwable e)
		{
			if (!loggedError)
			{
				loggedError = true;
				NoFogClient.LOGGER.catching(e);
			}
			
			return type.apply(dimensionToggles)
				.orElse(type.apply(globalToggles)
				.orElse(type.defaultToggle));
		}
		
		final FogToggles biomeToggles = config.getBiomeToggles().computeIfAbsent(biomeId, FogToggles::new);
		
		return type.apply(biomeToggles).orElse(
			type.apply(dimensionToggles).orElse(
			type.apply(globalToggles).orElse(type.defaultToggle)));
	}
	
	public static Identifier id(String name)
	{
		return new Identifier(MOD_ID, name);
	}
}
