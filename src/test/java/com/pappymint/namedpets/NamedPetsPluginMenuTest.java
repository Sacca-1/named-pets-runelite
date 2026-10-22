package com.pappymint.namedpets;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
		return pluginWithMenu(follower, entries, savedNames, replaceMenuNames, pohEnabled, new ArrayList<>());
	}

	private static NamedPetsPlugin pluginWithMenu(NPC follower, MenuEntry[] entries,
		Map<Integer, String> savedNames, boolean replaceMenuNames, boolean pohEnabled,
		List<CreatedMenuEntry> createdEntries) throws Exception
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
					case "createMenuEntry":
						CreatedMenuEntry created = new CreatedMenuEntry();
						createdEntries.add(created);
						return created.entry;
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

			@Override
			public String getSavedPetColor(int petId) { return null; }
		};
		setField(plugin, "client", client);
		setField(plugin, "config", config);
		setField(plugin, "pluginConfigManager", manager);
		return plugin;
	}

	@Test
	public void menuOpenedRenamesPohPetsAndAddsNamingOptionsOncePerPet() throws Exception
	{
		int[] pohIds = {NpcID.POH_SKILLPET_THIEVING, NpcID.POH_DOM_PET, NpcID.POH_CALLISTO_PET};
		String[] originalNames = {"Rocky", "Dom", "Callisto cub"};
		Map<Integer, String> savedNames = Map.of(NpcID.SKILLPET_THIEVING, "Sir Rocky",
			NpcID.DOM_PET, "Doom", NpcID.CALLISTOPET, "Bear");
		String[] customNames = {"Sir Rocky", "Doom", "Bear"};
		List<CreatedMenuEntry> created = new ArrayList<>();
		MenuEntry[] entries = new MenuEntry[6];
		NPC[] pets = new NPC[3];
		for (int i = 0; i < pets.length; i++)
		{
			pets[i] = stubNpc(pohIds[i], originalNames[i]);
			entries[i * 2] = stubMenuEntry(pets[i], new AtomicReference<>("<col=ffff00>" + originalNames[i]));
			entries[i * 2 + 1] = stubMenuEntry(pets[i], new AtomicReference<>("<col=ffff00>" + originalNames[i]));
		}
		NamedPetsPlugin plugin = pluginWithMenu(null, entries, savedNames, true, true, created);
		for (NPC pet : pets)
		{
			plugin.addNewPOHPetToRenderList(pet);
		}

		MenuOpened opened = new MenuOpened();
		opened.setMenuEntries(entries);
		plugin.onMenuOpened(opened);
		// Both callbacks can run for the same menu; custom names containing the NPC name must stay intact.
		plugin.onPostMenuSort(new PostMenuSort());

		assertEquals(6, created.size());
		for (int i = 0; i < pets.length; i++)
		{
			assertEquals("<col=ffff00>" + customNames[i], entries[i * 2].getTarget());
			assertEquals("<col=ffff00>" + customNames[i], entries[i * 2 + 1].getTarget());
			assertEquals("Name", created.get(i * 2).option);
			assertEquals("Color Name", created.get(i * 2 + 1).option);
			assertEquals("<col=ffff00>" + customNames[i], created.get(i * 2).target);
			assertEquals(MenuAction.RUNELITE, created.get(i * 2 + 1).type);
		}
	}

	@Test
	public void pohColorRemovalUsesFollowerConfigurationId() throws Exception
	{
		NPC pet = stubNpc(NpcID.POH_CALLISTO_PET, "Callisto cub");
		MenuEntry[] entries = {stubMenuEntry(pet, new AtomicReference<>("<col=ffff00>Callisto cub"))};
		List<CreatedMenuEntry> created = new ArrayList<>();
		NamedPetsPlugin plugin = pluginWithMenu(null, entries, Map.of(), true, true, created);
		AtomicReference<Integer> removedColor = new AtomicReference<>();
		setField(plugin, "pluginConfigManager", new NamedPetsConfigManager(plugin, null)
		{
			@Override
			public String getSavedPetName(int id) { return id == NpcID.CALLISTOPET ? "Bear" : null; }

			@Override
			public String getSavedPetColor(int id) { return id == NpcID.CALLISTOPET ? "#ff0000" : null; }

			@Override
			public void unsetPetColor(int id) { removedColor.set(id); }
		});
		plugin.addNewPOHPetToRenderList(pet);
		MenuOpened opened = new MenuOpened();
		opened.setMenuEntries(entries);

		plugin.onMenuOpened(opened);

		assertEquals(3, created.size());
		CreatedMenuEntry remove = created.get(2);
		assertEquals("Remove Name Color", remove.option);
		assertEquals(MenuAction.RUNELITE, remove.type);
		remove.onClick.accept(remove.entry);
		assertEquals(Integer.valueOf(NpcID.CALLISTOPET), removedColor.get());
	}

	private static class CreatedMenuEntry
	{
		String option;
		String target;
		MenuAction type;
		Consumer<MenuEntry> onClick;
		final MenuEntry entry = (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(),
			new Class[]{MenuEntry.class}, (proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "setOption": option = (String) args[0]; return proxy;
					case "setTarget": target = (String) args[0]; return proxy;
					case "setType": type = (MenuAction) args[0]; return proxy;
					case "onClick": onClick = (Consumer<MenuEntry>) args[0]; return proxy;
					default: return defaultValue(method.getReturnType());
				}
			});
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
