package com.pappymint.namedpets;

import net.runelite.api.gameval.NpcID;
import net.runelite.api.NPC;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NamedPetsConfigManagerTest
{
    @Test
    public void resolvesPohDogIdsToFollowerDogIds()
    {
        assertEquals(NpcID.LABRADOR_YELLOW,
            NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_LABRADOR_YELLOW));
        assertEquals(NpcID.YORKIE_YELLOW,
            NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_YORKIE_YELLOW));
    }

    @Test
    public void doesNotResolveUnknownPohIds()
    {
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_LABRADOR_YELLOW - 1));
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_YORKIE_YELLOW + 1));
        assertNotEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_LABRADOR_BROWN));
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_GROWNCAT_DEFAULT - 1));
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_GROWNCAT_HELL + 1));
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_OVERGROWNCAT_DEFAULT - 1));
        assertEquals(-1, NamedPetsConfigManager.resolveFollowerPetId(NpcID.POH_WILEYCAT_HELL + 1));
    }

    @Test
    public void resolvesEveryCatColourAndStageBeforeMatchingSharedNpcNames()
    {
        int[][] variants = {
            {NpcID.POH_GROWNCAT_DEFAULT, NpcID.GROWNCAT},
            {NpcID.POH_GROWNCAT_LIGHT, NpcID.GROWNCAT_LIGHT},
            {NpcID.POH_GROWNCAT_BROWN, NpcID.GROWNCAT_BROWN},
            {NpcID.POH_GROWNCAT_BLACK, NpcID.GROWNCAT_BLACK},
            {NpcID.POH_GROWNCAT_BROWNGREY, NpcID.GROWNCAT_BROWNGREY},
            {NpcID.POH_GROWNCAT_BLUEGREY, NpcID.GROWNCAT_BLUEGREY},
            {NpcID.POH_GROWNCAT_HELL, NpcID.GROWNCAT_HELL},
            {NpcID.POH_OVERGROWNCAT_DEFAULT, NpcID.OVERGROWNCAT},
            {NpcID.POH_OVERGROWNCAT_LIGHT, NpcID.OVERGROWNCAT_LIGHT},
            {NpcID.POH_OVERGROWNCAT_BROWN, NpcID.OVERGROWNCAT_BROWN},
            {NpcID.POH_OVERGROWNCAT_BLACK, NpcID.OVERGROWNCAT_BLACK},
            {NpcID.POH_OVERGROWNCAT_BROWNGREY, NpcID.OVERGROWNCAT_BROWNGREY},
            {NpcID.POH_OVERGROWNCAT_BLUEGREY, NpcID.OVERGROWNCAT_BLUEGREY},
            {NpcID.POH_OVERGROWNCAT_HELL, NpcID.OVERGROWNCAT_HELL},
            {NpcID.POH_LAZYCAT_DEFAULT, NpcID.LAZYCAT},
            {NpcID.POH_LAZYCAT_LIGHT, NpcID.LAZYCAT_LIGHT},
            {NpcID.POH_LAZYCAT_BROWN, NpcID.LAZYCAT_BROWN},
            {NpcID.POH_LAZYCAT_BLACK, NpcID.LAZYCAT_BLACK},
            {NpcID.POH_LAZYCAT_BROWNGREY, NpcID.LAZYCAT_BROWNGREY},
            {NpcID.POH_LAZYCAT_BLUEGREY, NpcID.LAZYCAT_BLUEGREY},
            {NpcID.POH_LAZYCAT_HELL, NpcID.LAZYCAT_HELL},
            {NpcID.POH_WILEYCAT_DEFAULT, NpcID.WILEYCAT},
            {NpcID.POH_WILEYCAT_LIGHT, NpcID.WILEYCAT_LIGHT},
            {NpcID.POH_WILEYCAT_BROWN, NpcID.WILEYCAT_BROWN},
            {NpcID.POH_WILEYCAT_BLACK, NpcID.WILEYCAT_BLACK},
            {NpcID.POH_WILEYCAT_BROWNGREY, NpcID.WILEYCAT_BROWNGREY},
            {NpcID.POH_WILEYCAT_BLUEGREY, NpcID.WILEYCAT_BLUEGREY},
            {NpcID.POH_WILEYCAT_HELL, NpcID.WILEYCAT_HELL}
        };
        NamedPetsConfigManager manager = managerWithSavedCat();
        for (int[] variant : variants) {
            assertEquals("POH cat " + variant[0], variant[1],
                manager.getPOHFollowerPetId(stubNpc(variant[0], "Cat")));
        }
    }

    @Test
    public void retainsNameFallbackForOtherPets()
    {
        NamedPetsConfigManager manager = managerWithSavedCat();
        assertEquals(NpcID.GROWNCAT, manager.getPOHFollowerPetId(stubNpc(-100, "Cat")));
        assertEquals(-1, manager.getPOHFollowerPetId(stubNpc(-100, "Unknown")));
        assertEquals(-1, manager.getPOHFollowerPetId(stubNpc(-100, null)));
    }

    private static NamedPetsConfigManager managerWithSavedCat()
    {
        return new NamedPetsConfigManager(null, null) {
            @Override
            public List<Integer> getAllSavedPetIds() {
                return Arrays.asList(NpcID.GROWNCAT);
            }

            @Override
            public String getPetNPCName(int petId) {
                return "Cat";
            }
        };
    }

    private static NPC stubNpc(int id, String name)
    {
        return (NPC) Proxy.newProxyInstance(NPC.class.getClassLoader(), new Class[]{NPC.class},
            (proxy, method, args) -> method.getName().equals("getId") ? id : name);
    }
}
