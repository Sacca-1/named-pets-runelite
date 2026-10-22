package com.pappymint.namedpets;

import net.runelite.client.config.ConfigManager;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NamedPetsConfigManager {
    private final ConfigManager configManager;

    public static final String CONFIG_GROUP = "namedPets";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_COLOR = "color";
    public static final String CONFIG_NPC_NAME = "npcName";

    @Inject
    NamedPetsConfigManager(NamedPetsPlugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
    }

    private String nameConfigKey(int petId) {
        return CONFIG_NAME + "-" + petId;
    }
    private String colorConfigKey(int petId) {
        return CONFIG_COLOR + "-" + petId;
    }
    private String npcNameConfigKey(int petId) {
        return CONFIG_NPC_NAME + "-" + petId;
    }

    // *** Pet Name Getters/Setters ***
    public String getSavedPetName(int petId) {
        return configManager.getRSProfileConfiguration(CONFIG_GROUP, nameConfigKey(petId));
    }
    public void setPetName(int petId, String petName) {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, nameConfigKey(petId), petName);
    }
    public void unsetPetName(int petId) {
        configManager.unsetRSProfileConfiguration(CONFIG_GROUP, nameConfigKey(petId));
    }

    // *** Pet Color Getters/Setters ***
    public String getSavedPetColor(int petId) {
        return configManager.getRSProfileConfiguration(CONFIG_GROUP, colorConfigKey(petId));
    }
    public void setPetColor(int petId, Color petNameColor) {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, colorConfigKey(petId), petNameColor);
    }
    public void unsetPetColor(int petId) {
        configManager.unsetRSProfileConfiguration(CONFIG_GROUP, colorConfigKey(petId));
    }

    // *** Pet NPC Name Getters/Setters ***
    public String getPetNPCName(int petId) {
        return configManager.getRSProfileConfiguration(CONFIG_GROUP, npcNameConfigKey(petId));
    }
    public void setPetNPCName(int petId, String npcName) {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, npcNameConfigKey(petId), npcName);
    }

    int getPOHFollowerPetId(NPC pohPet) {
        int followerPetId = resolveFollowerPetId(pohPet.getId());
        if (followerPetId != -1) {
            return followerPetId;
        }

        String npcName = pohPet.getName();
        if (npcName != null) {
            for (int savedPetId : getAllSavedPetIds()) {
                if (Objects.equals(npcName, getPetNPCName(savedPetId))) {
                    return savedPetId;
                }
            }
        }
        return -1;
    }

    /** Resolve POH variants before falling back to NPC names shared by multiple pets. */
    static int resolveFollowerPetId(int pohPetId) {
        switch (pohPetId) {
            case NpcID.POH_SKILLPET_THIEVING:
                return NpcID.SKILLPET_THIEVING;
            case NpcID.POH_SKILLPET_THIEVING_PANDA:
                return NpcID.SKILLPET_THIEVING_PANDA;
            case NpcID.POH_SKILLPET_THIEVING_TANUKI:
                return NpcID.SKILLPET_THIEVING_TANUKI;
            case NpcID.POH_DOM_PET:
                return NpcID.DOM_PET;
            case NpcID.POH_CALLISTO_PET:
                return NpcID.CALLISTOPET;
            case NpcID.POH_CALLISTO_PET_LEGACY:
                return NpcID.CALLISTOPET_LEGACY;
        }
        if (pohPetId >= NpcID.POH_LABRADOR_YELLOW && pohPetId <= NpcID.POH_YORKIE_YELLOW) {
            return pohPetId + NpcID.LABRADOR_YELLOW - NpcID.POH_LABRADOR_YELLOW;
        }
        if (pohPetId >= NpcID.POH_GROWNCAT_DEFAULT && pohPetId <= NpcID.POH_GROWNCAT_HELL) {
            return pohPetId + NpcID.GROWNCAT - NpcID.POH_GROWNCAT_DEFAULT;
        }
        if (pohPetId >= NpcID.POH_OVERGROWNCAT_DEFAULT && pohPetId <= NpcID.POH_OVERGROWNCAT_HELL) {
            return pohPetId + NpcID.OVERGROWNCAT - NpcID.POH_OVERGROWNCAT_DEFAULT;
        }

        // Lazy and wily followers put the light variant before the default variant.
        if (pohPetId == NpcID.POH_LAZYCAT_DEFAULT) {
            return NpcID.LAZYCAT;
        }
        if (pohPetId == NpcID.POH_LAZYCAT_LIGHT) {
            return NpcID.LAZYCAT_LIGHT;
        }
        if (pohPetId >= NpcID.POH_LAZYCAT_BROWN && pohPetId <= NpcID.POH_LAZYCAT_HELL) {
            return pohPetId + NpcID.LAZYCAT_BROWN - NpcID.POH_LAZYCAT_BROWN;
        }
        if (pohPetId == NpcID.POH_WILEYCAT_DEFAULT) {
            return NpcID.WILEYCAT;
        }
        if (pohPetId == NpcID.POH_WILEYCAT_LIGHT) {
            return NpcID.WILEYCAT_LIGHT;
        }
        if (pohPetId >= NpcID.POH_WILEYCAT_BROWN && pohPetId <= NpcID.POH_WILEYCAT_HELL) {
            return pohPetId + NpcID.WILEYCAT_BROWN - NpcID.POH_WILEYCAT_BROWN;
        }
        return -1;
    }

    public List<Integer> getAllSavedPetIds() {
        List<Integer> idList = new ArrayList<>();

        final String profileKey = configManager.getRSProfileKey();
        List<String> configKeys = configManager.getRSProfileConfigurationKeys(CONFIG_GROUP, profileKey, CONFIG_NAME);

        for (String key : configKeys) {
            String prefix = CONFIG_NAME + "-";
            if (key.startsWith(prefix)) {
                idList.add(Integer.valueOf(key.replace(prefix, "")));
            }
        }

        return idList;
    }
}
