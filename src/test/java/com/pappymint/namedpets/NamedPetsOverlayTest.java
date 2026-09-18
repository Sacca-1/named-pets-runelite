package com.pappymint.namedpets;

import net.runelite.api.gameval.NpcID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NamedPetsOverlayTest
{
    @Test
    public void resolvesPohDogIdsToFollowerDogIds()
    {
        assertEquals(NpcID.LABRADOR_YELLOW,
            NamedPetsOverlay.resolveFollowerPetId(NpcID.POH_LABRADOR_YELLOW));
        assertEquals(NpcID.YORKIE_YELLOW,
            NamedPetsOverlay.resolveFollowerPetId(NpcID.POH_YORKIE_YELLOW));
    }

    @Test
    public void doesNotResolveNonDogPohIds()
    {
        assertEquals(-1, NamedPetsOverlay.resolveFollowerPetId(NpcID.POH_LABRADOR_YELLOW - 1));
        assertEquals(-1, NamedPetsOverlay.resolveFollowerPetId(NpcID.POH_YORKIE_YELLOW + 1));
        assertNotEquals(-1, NamedPetsOverlay.resolveFollowerPetId(NpcID.POH_LABRADOR_BROWN));
    }
}
