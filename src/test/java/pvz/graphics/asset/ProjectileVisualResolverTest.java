package pvz.graphics.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.BowlingBulbProjectile;
import pvz.model.entity.projectile.DirectionalProjectile;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileFamily;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;

class ProjectileVisualResolverTest {
    private World world;
    private ProjectileVisualResolver resolver;

    @BeforeEach
    void setUp() {
        world = new World(
                new Game(),
                new Board(9, 5),
                new BattleResources(50, 0)
        );
        resolver = new ProjectileVisualResolver();
    }

    @Test
    void peaFamilyUsesRealPeaArtAndTypeTint() {
        Projectile pea = new Projectile(
                world,
                "Snow Pea projectile",
                2,
                3,
                0,
                20,
                ProjectileType.ICE,
                7,
                HorizontalDirection.RIGHT,
                ProjectileHitLimit.singleHit(),
                ProjectileFamily.PEA
        );

        ProjectileVisualResolver.Visual visual = resolver.visual(pea);

        assertEquals("IMAGE_PROJECTILEPEA", visual.textureId());
        assertEquals(ProjectileVisualResolver.Flight.STRAIGHT,
                visual.flight());
        assertTrue(visual.tint().b > visual.tint().r);
    }

    @Test
    void directionalCactusUsesItsPamAndDirectionalFlight() {
        DirectionalProjectile cactus = new DirectionalProjectile(
                world,
                "Cactus projectile",
                2,
                3,
                30,
                ProjectileType.NORMAL,
                7,
                ShotVector.UP_RIGHT
        );

        ProjectileVisualResolver.Visual visual = resolver.visual(cactus);

        assertTrue(visual.pamPath().contains("T_CACTUS_PROJECTILE"));
        assertEquals("idle", visual.clip());
        assertEquals(ProjectileVisualResolver.Flight.DIRECTIONAL,
                visual.flight());
    }

    @Test
    void bowlingBulbColorNameSelectsMatchingPam() {
        BowlingBulbProjectile blue = new BowlingBulbProjectile(
                world,
                "Bowling Bulb Blue projectile",
                2,
                3,
                120,
                ProjectileType.NORMAL
        );

        ProjectileVisualResolver.Visual visual = resolver.visual(blue);

        assertTrue(visual.pamPath().contains("PROJECTILE2"));
        assertEquals(ProjectileVisualResolver.Flight.BOWLING,
                visual.flight());
    }
}
