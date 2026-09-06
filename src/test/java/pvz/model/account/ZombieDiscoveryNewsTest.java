package pvz.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZombieDiscoveryNewsTest {
    @Test
    void firstDiscoveryCreatesExactlyOneUnreadNewsItem() {
        User user = new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );

        assertTrue(user.discoverZombie("cone-head", "Cone Head"));
        assertEquals(1, user.getSeenZombies().size());
        assertEquals(1, user.getUnreadNews().size());
        assertEquals(
                "Zombie Discovered",
                user.getUnreadNews().get(0).getTitle()
        );
        assertEquals(
                "Cone Head has been discovered!",
                user.getUnreadNews().get(0).getMessage()
        );

        assertFalse(user.discoverZombie("CONE-HEAD", "Cone Head"));
        assertEquals(1, user.getSeenZombies().size());
        assertEquals(1, user.getUnreadNews().size());
    }
}
