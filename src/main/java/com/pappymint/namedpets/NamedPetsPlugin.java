package com.pappymint.namedpets;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@PluginDescriptor(
	name = "Named Pets",
	description = "Right click & give your pets a name!",
	tags = {"name", "pet"}
)
public class NamedPetsPlugin extends Plugin
{
	private NamedPetsConfigManager pluginConfigManager;

	@Inject
	private Client client;
	@Inject
	private NamedPetsConfig config;
	@Inject
	private ChatboxPanelManager chatboxPanelManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private NamedPetsOverlay petNameOverlay;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ColorPickerManager colorPickerManager;

	@Provides
	NamedPetsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NamedPetsConfig.class);
	}

	private ArrayList<NPC> pohPets = new ArrayList<>();

	@Override
	protected void startUp() throws Exception {
		pluginConfigManager = new NamedPetsConfigManager(this, configManager);
		clearPOHPetRenderList();
		overlayManager.add(petNameOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clearPOHPetRenderList();
		overlayManager.remove(petNameOverlay);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened menuOpened)
	{
		replacePetMenuTargets(menuOpened.getMenuEntries());

		if (!config.showPetNameMenuOptions())
		{
			return;
		}

		addPetMenuOptions(menuOpened.getMenuEntries());
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort postMenuSort)
	{
		replacePetMenuTargets(client.getMenuEntries());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (config.petNamesPOHEnabled()) {
			NPC spawnedNpc = event.getNpc();
			NPCComposition npcComposition = spawnedNpc.getComposition();
			String[] actions = npcComposition.getActions();

			// Has menu option 'Pick-up' and is not following the player
			if (Arrays.asList(actions).contains("Pick-up") && !npcComposition.isFollower()) {
                addNewPOHPetToRenderList(spawnedNpc);
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		// Unsubscribe pet from render list
		NPC npcDespawned = event.getNpc();
		removePOHPetFromRenderList(npcDespawned);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			clearPOHPetRenderList();
		}
	}

	private int getMenuPetId(NPC npc)
	{
		if (npc == null) {
			return -1;
		}
		if (npc == client.getFollower()) {
			return npc.getId();
		}
		if (config.petNamesPOHEnabled() && isPetInPOHRenderList(npc)) {
			return pluginConfigManager.getPOHFollowerPetId(npc);
		}
		return -1;
	}

	private void addPetMenuOptions(MenuEntry[] menuEntries)
	{
		Set<NPC> addedPets = new HashSet<>();
		for (int entryIndex = 0; entryIndex < menuEntries.length; entryIndex++) {
			MenuEntry entry = menuEntries[entryIndex];
			NPC targetedNpc = entry.getNpc();
			int petId = getMenuPetId(targetedNpc);
			if (petId != -1 && addedPets.add(targetedNpc)) {
				addNamePetMenuOption(targetedNpc, petId, entryIndex, entry);
				addColorNameMenuOption(petId, entryIndex, entry);
			}
		}
	}

	private void replacePetMenuTargets(MenuEntry[] menuEntries)
	{
		if (!config.replaceMenuPetName())
		{
			return;
		}

		for (MenuEntry menuEntry : menuEntries)
		{
			NPC targetedNpc = menuEntry.getNpc();
			if (targetedNpc == null || targetedNpc.getName() == null)
			{
				continue;
			}

			int petId = getMenuPetId(targetedNpc);
			if (petId == -1)
			{
				continue;
			}
			String npcName = targetedNpc.getName();
			String petName = getExistingPetName(petId);
			String target = menuEntry.getTarget();
			if (!petName.isEmpty() && target != null && Text.removeTags(target).equals(npcName))
			{
				menuEntry.setTarget(target.replace(npcName, petName));
			}
		}
	}

	private void addNamePetMenuOption(NPC pet, int petId, int index, MenuEntry menuEntry) {
		client.createMenuEntry(index)
			.setOption("Name")
			.setTarget(menuEntry.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(e -> onNameMenuEntryOptionClicked(pet, petId));
	}

	private void addColorNameMenuOption(int petId, int index, MenuEntry menuEntry) {
		if (!getExistingPetName(petId).isEmpty()) {
			// Add remove color option if color is set

			client.createMenuEntry(index)
				.setOption("Color Name")
				.setTarget(menuEntry.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					Color existingColor = getExistingPetNameColor(petId);

					SwingUtilities.invokeLater(() ->
					{
						RuneliteColorPicker colorPicker = colorPickerManager.create(client,
							existingColor, "Pet Name Color", false);
						colorPicker.setOnClose(color -> saveNameColor(petId, color));
						colorPicker.setVisible(true);
					});
				});


			if (
				pluginConfigManager.getSavedPetColor(petId) != null &&
				!pluginConfigManager.getSavedPetColor(petId).isEmpty()
			) {
				client.createMenuEntry(index + 1)
						.setOption("Remove Name Color")
						.setTarget(menuEntry.getTarget())
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							pluginConfigManager.unsetPetColor(petId);
						});
			}
		}
	}

	private void onNameMenuEntryOptionClicked(NPC pet, int petId) {
		String petNPCName = pet.getName();
		chatboxPanelManager.openTextInput("Name your " + petNPCName)
			.value(getExistingPetName(petId))
			.onDone((input) ->
			{
				savePetName(petId, petNPCName, input);
			})
			.build();
	}

	/**
	 * Save a pet name into config manager
	 * @param petId Pet Id
	 * @param petNPCName NPC name of pet
	 * @param petName Custom pet name to save
	 */
	private void savePetName(int petId, String petNPCName, String petName)
	{
		if (Objects.equals(petName, "") || petName == null) {
			pluginConfigManager.unsetPetName(petId);
		} else {
			pluginConfigManager.setPetName(petId, petName);
			pluginConfigManager.setPetNPCName(petId, petNPCName);
		}
	}

	private String getExistingPetName(int petNpcId)
	{
		String name = pluginConfigManager.getSavedPetName(petNpcId);
		if (name != null) {
			return name;
		}
		return "";
	}

	/**
	 * Saves a color to display for an individual pet
	 * @param petNpcId Pet NPC to set colored name for
	 * @param nameColor Color to save
	 */
	private void saveNameColor(int petNpcId, Color nameColor)
	{
		pluginConfigManager.setPetColor(petNpcId, nameColor);
	}

	private Color getExistingPetNameColor(int petNpcId)
	{
		String savedColor = pluginConfigManager.getSavedPetColor(petNpcId);
		if (savedColor != null) {
			return Color.decode(savedColor);
		}
		return Color.white;
	}

	public ArrayList<NPC> getPOHPetRenderList() {
		return pohPets;
	}

	public void addNewPOHPetToRenderList(NPC newPet)
	{
		if (!isPetInPOHRenderList(newPet)) {
			pohPets.add(newPet);
		}
	}

	public void removePOHPetFromRenderList(NPC petToRemove)
	{
		if (isPetInPOHRenderList(petToRemove)) {
			pohPets.remove(petToRemove);
		}
	}

	public boolean isPetInPOHRenderList(NPC pet)
	{
		return pohPets.contains(pet);
	}

	void clearPOHPetRenderList()
	{
		pohPets.clear();
	}
}
