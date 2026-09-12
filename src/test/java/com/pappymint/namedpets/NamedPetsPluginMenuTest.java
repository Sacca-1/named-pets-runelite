package com.pappymint.namedpets;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NamedPetsPluginMenuTest
{
	@Test
	public void petNameMenuOptionsAreVisibleByDefault()
	{
		NamedPetsConfig config = new NamedPetsConfig()
		{
		};

		assertTrue(config.showPetNameMenuOptions());
	}

	@Test
	public void menuNameOptionsCanBeHidden() throws Exception
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		NamedPetsConfig config = (NamedPetsConfig) Proxy.newProxyInstance(
			NamedPetsConfig.class.getClassLoader(),
			new Class[]{NamedPetsConfig.class},
			(proxy, method, args) -> method.getName().equals("showPetNameMenuOptions")
				? false
				: defaultValue(method.getReturnType())
		);
		MenuOpened menuOpened = new MenuOpened();

		setField(plugin, "config", config);

		plugin.onMenuOpened(menuOpened);
	}

	@Test
	public void postMenuSortReplacesFollowerNameBeforeMenuOpens() throws Exception
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		NPC follower = stubNpc(123, "Rocky");
		AtomicReference<String> target = new AtomicReference<>("<col=ffff00>Rocky");
		MenuEntry menuEntry = stubMenuEntry(follower, target);

		Client client = (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class[]{Client.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getFollower":
						return follower;
					case "getMenuEntries":
						return new MenuEntry[]{menuEntry};
					default:
						return defaultValue(method.getReturnType());
				}
			}
		);
		NamedPetsConfig config = (NamedPetsConfig) Proxy.newProxyInstance(
			NamedPetsConfig.class.getClassLoader(),
			new Class[]{NamedPetsConfig.class},
			(proxy, method, args) -> method.getName().equals("replaceMenuPetName")
				? true
				: defaultValue(method.getReturnType())
		);
		NamedPetsConfigManager petConfigManager = new NamedPetsConfigManager(plugin, null)
		{
			@Override
			public String getSavedPetName(int petId)
			{
				return "Pebble";
			}
		};

		setField(plugin, "client", client);
		setField(plugin, "config", config);
		setField(plugin, "pluginConfigManager", petConfigManager);

		plugin.onPostMenuSort(new PostMenuSort());

		assertEquals("<col=ffff00>Pebble", target.get());
	}

	private static NPC stubNpc(int id, String name)
	{
		return (NPC) Proxy.newProxyInstance(
			NPC.class.getClassLoader(),
			new Class[]{NPC.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getId":
						return id;
					case "getName":
						return name;
					default:
						return defaultValue(method.getReturnType());
				}
			}
		);
	}

	private static MenuEntry stubMenuEntry(NPC npc, AtomicReference<String> target)
	{
		return (MenuEntry) Proxy.newProxyInstance(
			MenuEntry.class.getClassLoader(),
			new Class[]{MenuEntry.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getNpc":
						return npc;
					case "getTarget":
						return target.get();
					case "setTarget":
						target.set((String) args[0]);
						return proxy;
					default:
						return defaultValue(method.getReturnType());
				}
			}
		);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception
	{
		Field field = NamedPetsPlugin.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Object defaultValue(Class<?> type)
	{
		if (!type.isPrimitive())
		{
			return null;
		}
		if (type == boolean.class)
		{
			return false;
		}
		if (type == char.class)
		{
			return '\0';
		}
		return 0;
	}
}
