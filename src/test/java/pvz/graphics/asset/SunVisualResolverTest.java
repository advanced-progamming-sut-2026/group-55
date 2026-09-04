package pvz.graphics.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunType;

class SunVisualResolverTest {

    @Test
    void canonicalSunValuesUseExactRequestedSizeRatios() {
        Game game = new Game();
        World world = world(game);
        SunVisualResolver resolver = new SunVisualResolver();

        float small = size(resolver, world, 25);
        float normal = size(resolver, world, 50);
        float big = size(resolver, world, 75);
        float special = size(resolver, world, 100);

        assertEquals(0.96f, normal, 0.0001f);
        assertEquals(normal * 0.5f, small, 0.0001f);
        assertEquals(normal * 1.5f, big, 0.0001f);
        assertEquals(normal * 2.0f, special, 0.0001f);
    }

    @Test
    void radioactiveSunUsesBluePamStateAndExplosionVariations() {
        Game game = new Game();
        World world = world(game);
        SunVisualResolver resolver = new SunVisualResolver();
        Sun radioactive = Sun.fromSky(
                world,
                SunType.RADIOACTIVE,
                4.5,
                2.5,
                50
        );

        Sun normal = Sun.fromSky(
                world,
                SunType.NORMAL,
                4.5,
                2.5,
                50
        );
        SunVisualResolver.Visual visual = resolver.resolve(radioactive, 0L);
        SunVisualResolver.Visual later = resolver.resolve(radioactive, 20L);
        SunVisualResolver.Visual normalVisual = resolver.resolve(normal, 0L);

        assertEquals("blue", visual.clipName());
        assertEquals(normalVisual.sizeInCell(), visual.sizeInCell(), 0.0001f);
        assertEquals(0.96f, visual.sizeInCell(), 0.0001f);
        assertEquals(visual.sizeInCell(), later.sizeInCell(), 0.0001f);
        assertNotEquals(com.badlogic.gdx.graphics.Color.WHITE, visual.tint());
        assertEquals(3, resolver.explosionClips().size());
        assertEquals("animation", resolver.explosionClip(0));
        assertEquals("animation2", resolver.explosionClip(1));
        assertEquals("animation3", resolver.explosionClip(2));
        assertEquals("animation", resolver.explosionClip(3));
    }

    private float size(
            SunVisualResolver resolver,
            World world,
            int value
    ) {
        Sun sun = Sun.recovered(world, 1.5, 1.5, value);
        return resolver.resolve(sun, 0L).sizeInCell();
    }

    private World world(Game game) {
        return new World(
                game,
                new Board(9, 5),
                new BattleResources(0, 0),
                new Random(1)
        );
    }
}
