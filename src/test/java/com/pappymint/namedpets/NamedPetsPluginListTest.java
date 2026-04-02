package com.pappymint.namedpets;

import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NamedPetsPluginListTest
{
	@Test
	public void clearPOHPetRenderListRemovesAllTrackedPets()
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		NPC firstPet = stubNpc();
		NPC secondPet = stubNpc();

		plugin.addNewPOHPetToRenderList(firstPet);
		plugin.addNewPOHPetToRenderList(secondPet);

		plugin.clearPOHPetRenderList();

		assertFalse(plugin.isPetInPOHRenderList(firstPet));
		assertFalse(plugin.isPetInPOHRenderList(secondPet));
		assertTrue(plugin.getPOHPetRenderList().isEmpty());
	}

	@Test
	public void addNewPOHPetToRenderListDoesNotDuplicateSameNpc()
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		NPC pet = stubNpc();

		plugin.addNewPOHPetToRenderList(pet);
		plugin.addNewPOHPetToRenderList(pet);

		assertTrue(plugin.isPetInPOHRenderList(pet));
		assertTrue(plugin.getPOHPetRenderList().size() == 1);
	}

	@Test
	public void onGameStateChangedClearsTrackedPohPetsWhenLoggedOut()
	{
		NamedPetsPlugin plugin = new NamedPetsPlugin();
		GameStateChanged event = new GameStateChanged();
		NPC pet = stubNpc();

		plugin.addNewPOHPetToRenderList(pet);
		event.setGameState(GameState.LOGIN_SCREEN);

		plugin.onGameStateChanged(event);

		assertTrue(plugin.getPOHPetRenderList().isEmpty());
	}

	private NPC stubNpc()
	{
		return (NPC) Proxy.newProxyInstance(
			NPC.class.getClassLoader(),
			new Class[]{NPC.class},
			(proxy, method, args) ->
			{
				String methodName = method.getName();
				if (methodName.equals("equals"))
				{
					return proxy == args[0];
				}
				if (methodName.equals("hashCode"))
				{
					return System.identityHashCode(proxy);
				}
				if (methodName.equals("toString"))
				{
					return "StubNpc";
				}

				Class<?> returnType = method.getReturnType();
				if (returnType.equals(boolean.class))
				{
					return false;
				}
				if (returnType.equals(int.class))
				{
					return 0;
				}
				if (returnType.equals(long.class))
				{
					return 0L;
				}
				if (returnType.equals(float.class))
				{
					return 0f;
				}
				if (returnType.equals(double.class))
				{
					return 0d;
				}
				return null;
			}
		);
	}
}
