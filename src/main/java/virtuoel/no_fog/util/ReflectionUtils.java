package virtuoel.no_fog.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.RegistryWorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import virtuoel.no_fog.NoFogClient;

public class ReflectionUtils
{
	public static final MethodHandle FOG_DENSITY, FOG_START, FOG_END;
	public static final RegistryKey<Registry<Fluid>> FLUID_KEY;
	public static final RegistryKey<Registry<Biome>> BIOME_KEY;
	public static final RegistryKey<Registry<DimensionType>> DIMENSION_TYPE_KEY;
	public static final Registry<Biome> BUILTIN_BIOME_REGISTRY;
	public static final StatusEffect BLINDNESS, DARKNESS;
	
	static
	{
		final Int2ObjectMap<MethodHandle> h = new Int2ObjectArrayMap<MethodHandle>();
		
		final Lookup lookup = MethodHandles.lookup();
		String mapped = "unset";
		Class<?> clazz;
		Method m;
		
		try
		{
			final boolean is116 = VersionUtils.MINOR == 16;
			
			if (is116)
			{
				clazz = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
				m = clazz.getMethod("fogDensity", float.class);
				h.put(0, lookup.unreflect(m));
				m = clazz.getMethod("fogStart", float.class);
				h.put(1, lookup.unreflect(m));
				m = clazz.getMethod("fogEnd", float.class);
				h.put(2, lookup.unreflect(m));
			}
		}
		catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException e1)
		{
			NoFogClient.LOGGER.error("Current name lookup: {}", mapped);
			NoFogClient.LOGGER.catching(e1);
		}
		
		FOG_DENSITY = h.get(0);
		FOG_START = h.get(1);
		FOG_END = h.get(2);
		FLUID_KEY = Registry.FLUID_KEY;
		BIOME_KEY = Registry.BIOME_KEY;
		DIMENSION_TYPE_KEY = Registry.DIMENSION_TYPE_KEY;
		BUILTIN_BIOME_REGISTRY = BuiltinRegistries.BIOME;
		BLINDNESS = StatusEffects.BLINDNESS;
		DARKNESS = null; // TODO 1.19
	}
	
	public static <E> Registry<E> getDynamicRegistry(RegistryWorldView w, RegistryKey<? extends Registry<E>> key)
	{
		return w.getRegistryManager().get(key);
	}
	
	public static String getBiomeId(Entity entity)
	{
		final Biome biome = entity.world.getBiome(new BlockPos(entity.getPos()));
		return getId(getDynamicRegistry(entity.world, BIOME_KEY), biome).toString();
	}
	
	public static boolean hasStatusEffect(LivingEntity entity, StatusEffect effect)
	{
		return entity.hasStatusEffect(effect);
	}
	
	public static Set<Identifier> getIds(Registry<?> registry)
	{
		return registry.getIds();
	}
	
	public static <V> Identifier getId(Registry<V> registry, V entry)
	{
		return registry.getId(entry);
	}
	
	public static void setFogDensity(float f) throws Throwable
	{
		if (FOG_DENSITY != null)
		{
			FOG_DENSITY.invokeExact(f);
		}
	}
	
	public static void setFogStart(float f) throws Throwable
	{
		if (FOG_START != null)
		{
			FOG_START.invokeExact(f);
		}
	}
	
	public static void setFogEnd(float f) throws Throwable
	{
		if (FOG_END != null)
		{
			FOG_END.invokeExact(f);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Field field, Object object, Supplier<T> defaultValue)
	{
		try
		{
			return (T) field.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			return defaultValue.get();
		}
	}
	
	public static Optional<Field> getField(final Optional<Class<?>> classObj, final String fieldName)
	{
		return classObj.map(c ->
		{
			try
			{
				final Field f = c.getDeclaredField(fieldName);
				f.setAccessible(true);
				return f;
			}
			catch (SecurityException | NoSuchFieldException e)
			{
				return null;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(final Optional<Class<?>> classObj, final String fieldName, final Object object, final T defaultValue)
	{
		return getField(classObj, fieldName).map(f ->
		{
			try
			{
				return (T) f.get(object);
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				return defaultValue;
			}
		}).orElse(defaultValue);
	}
	
	public static void setField(final Optional<Class<?>> classObj, final String fieldName, final Object object, final Object value)
	{
		getField(classObj, fieldName).ifPresent(f ->
		{
			try
			{
				f.set(object, value);
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				
			}
		});
	}
	
	public static Optional<Method> getMethod(final Optional<Class<?>> classObj, final String methodName, final Class<?>... args)
	{
		return classObj.map(c ->
		{
			try
			{
				final Method m = c.getMethod(methodName, args);
				m.setAccessible(true);
				return m;
			}
			catch (SecurityException | NoSuchMethodException e)
			{
				return null;
			}
		});
	}
	
	public static <T> Optional<Constructor<T>> getConstructor(final Optional<Class<T>> clazz, final Class<?>... params)
	{
		return clazz.map(c ->
		{
			try
			{
				return c.getConstructor(params);
			}
			catch (NoSuchMethodException | SecurityException e)
			{
				return null;
			}
		});
	}
	
	public static Optional<Class<?>> getClass(final String className, final String... classNames)
	{
		Optional<Class<?>> ret = getClass(className);
		
		for (final String name : classNames)
		{
			if (ret.isPresent())
			{
				return ret;
			}
			
			ret = getClass(name);
		}
		
		return ret;
	}
	
	public static Optional<Class<?>> getClass(final String className)
	{
		try
		{
			return Optional.of(Class.forName(className));
		}
		catch (ClassNotFoundException e)
		{
			return Optional.empty();
		}
	}
	
	public static final ReflectionUtils INSTANCE = new ReflectionUtils();
	
	private ReflectionUtils()
	{
		
	}
}
