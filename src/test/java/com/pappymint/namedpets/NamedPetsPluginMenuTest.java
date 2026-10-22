package com.pappymint.namedpets;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
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

	@Test
	public void pohMenuNamesDistinguishCatColoursWithoutAFollower() throws Exception
	{
		NPC orangeCat = stubNpc(NpcID.POH_GROWNCAT_DEFAULT, "Cat");
		NPC whiteCat = stubNpc(NpcID.POH_GROWNCAT_LIGHT, "Cat");
		NPC unrelatedCat = stubNpc(NpcID.POH_GROWNCAT_DEFAULT, "Cat");
		AtomicReference<String> orangeTarget = new AtomicReference<>("<col=ffff00>Cat");
		AtomicReference<String> whiteTarget = new AtomicReference<>("<col=ffff00>Cat");
		AtomicReference<String> unrelatedTarget = new AtomicReference<>("<col=ffff00>Cat");
		NamedPetsPlugin plugin = pluginWithMenu(null, new MenuEntry[]{
			stubMenuEntry(orangeCat, orangeTarget), stubMenuEntry(whiteCat, whiteTarget),
			stubMenuEntry(unrelatedCat, unrelatedTarget)
		}, Map.of(NpcID.GROWNCAT, "Marmalade", NpcID.GROWNCAT_LIGHT, "Snowball"), true, true);
		plugin.addNewPOHPetToRenderList(orangeCat);
		plugin.addNewPOHPetToRenderList(whiteCat);

		plugin.onPostMenuSort(new PostMenuSort());

		assertEquals("<col=ffff00>Marmalade", orangeTarget.get());
		assertEquals("<col=ffff00>Snowball", whiteTarget.get());
		assertEquals("<col=ffff00>Cat", unrelatedTarget.get());
	}

	@Test
	public void unnamedPetsKeepTheirOriginalMenuTargets() throws Exception
	{
		NPC follower = stubNpc(123, "Rocky");
		NPC cat = stubNpc(NpcID.POH_GROWNCAT_LIGHT, "Cat");
		AtomicReference<String> followerTarget = new AtomicReference<>("<col=ffff00>Rocky");
		AtomicReference<String> catTarget = new AtomicReference<>("<col=ffff00>Cat");
		NamedPetsPlugin plugin = pluginWithMenu(follower, new MenuEntry[]{
			stubMenuEntry(follower, followerTarget), stubMenuEntry(cat, catTarget)
		}, Map.of(NpcID.GROWNCAT, "Marmalade"), true, true);
		plugin.addNewPOHPetToRenderList(cat);

		plugin.onPostMenuSort(new PostMenuSort());

		assertEquals("<col=ffff00>Rocky", followerTarget.get());
		assertEquals("<col=ffff00>Cat", catTarget.get());
	}

	@Test
	public void pohMenuNamesUseExistingDogMapping() throws Exception
	{
		NPC dog = stubNpc(NpcID.POH_LABRADOR_BROWN, "Labrador");
		AtomicReference<String> target = new AtomicReference<>("<col=ffff00>Labrador");
		NamedPetsPlugin plugin = pluginWithMenu(null, new MenuEntry[]{stubMenuEntry(dog, target)},
			Map.of(NpcID.LABRADOR_BROWN, "Cocoa"), true, true);
		plugin.addNewPOHPetToRenderList(dog);

		plugin.onPostMenuSort(new PostMenuSort());

		assertEquals("<col=ffff00>Cocoa", target.get());
	}

	@Test
	public void menuReplacementRespectsBothSettings() throws Exception
	{
		for (boolean replacementEnabled : new boolean[]{false, true})
		{
			NPC cat = stubNpc(NpcID.POH_GROWNCAT_DEFAULT, "Cat");
			AtomicReference<String> target = new AtomicReference<>("<col=ffff00>Cat");
			NamedPetsPlugin plugin = pluginWithMenu(null, new MenuEntry[]{stubMenuEntry(cat, target)},
				Map.of(NpcID.GROWNCAT, "Marmalade"), replacementEnabled, !replacementEnabled);
			plugin.addNewPOHPetToRenderList(cat);

			plugin.onPostMenuSort(new PostMenuSort());

			assertEquals("<col=ffff00>Cat", target.get());
		}
	}

	private static NamedPetsPlugin pluginWithMenu(NPC follower, MenuEntry[] entries,
		Map<Integer, String> savedNames, boolean replaceMenuNames, boolean pohEnabled) throws Exception
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
			new Class[]{Client.class}, (proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getFollower":
						return follower;
					case "getMenuEntries":
						return entries;
					default:
						return defaultValue(method.getReturnType());
				}
			});
		NamedPetsConfig config = new NamedPetsConfig()
		{
			@Override
			public boolean replaceMenuPetName() { return replaceMenuNames; }

			@Override
			public boolean petNamesPOHEnabled() { return pohEnabled; }
		};
		NamedPetsConfigManager manager = new NamedPetsConfigManager(plugin, null)
		{
			@Override
			public String getSavedPetName(int petId) { return savedNames.get(petId); }
		};
		setField(plugin, "client", client);
		setField(plugin, "config", config);
		setField(plugin, "pluginConfigManager", manager);
		return plugin;
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
					case "equals":
						return proxy == args[0];
					case "hashCode":
						return System.identityHashCode(proxy);
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
