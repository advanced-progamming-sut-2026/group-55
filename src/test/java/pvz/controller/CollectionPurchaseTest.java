package pvz.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import pvz.data.*;
import pvz.graphics.GraphicalMenuView;
import pvz.model.account.*;
import pvz.model.command.CollectionCommand;
import pvz.model.entity.plant.*;
import pvz.model.entity.plant.level.PlantLevelCostTable;
import pvz.model.utils.AppState;
import static org.junit.jupiter.api.Assertions.*;

class CollectionPurchaseTest {
    @TempDir Path directory;

    @Test void failedSaveRestoresPurchaseAndPreservesExistingProgress() {
        Fixture f = new Fixture(false);
        int coins = f.user.getCoins();
        var plants = List.copyOf(f.user.getUnlockedPlants());
        var news = List.copyOf(f.user.getAllNews());
        f.purchase();
        assertEquals(coins, f.user.getCoins());
        assertEquals(plants, f.user.getUnlockedPlants());
        assertEquals(news, f.user.getAllNews());
        assertEquals(List.of(true), f.errors);
        assertSame(f.user, f.state.getCurrentUser());
        f.manager.succeeds = true;
        f.purchase();
        assertNotNull(f.user.getOwnedPlant("Test Plant"));
        assertEquals(coins - 2000, f.user.getCoins());
    }

    @Test void successfulPurchaseChargesOnceAndRejectsDuplicate() {
        Fixture f = new Fixture(true);
        int coins = f.user.getCoins();
        int news = f.user.getAllNews().size();
        f.purchase();
        f.purchase();
        assertEquals(coins - 2000, f.user.getCoins());
        assertEquals(news + 1, f.user.getAllNews().size());
        assertEquals(List.of(false, true), f.errors);
        assertEquals(1, f.manager.saves);
    }

    @Test void insufficientCoinsDoesNotMutateOrSave() {
        Fixture f = new Fixture(true);
        f.user.spendCoins(f.user.getCoins());
        int news = f.user.getAllNews().size();
        f.purchase();
        assertNull(f.user.getOwnedPlant("Test Plant"));
        assertEquals(0, f.user.getCoins());
        assertEquals(news, f.user.getAllNews().size());
        assertEquals(0, f.manager.saves);
        assertEquals(List.of(true), f.errors);
    }

    private class Fixture {
        final User user = new User("tester", "hash", "Tester", "test@example.com", "other");
        final AppState state = new AppState();
        final List<Boolean> errors = new ArrayList<>();
        final TestManager manager;
        final CollectionController controller;
        Fixture(boolean succeeds) {
            manager = new TestManager(succeeds);
            user.addCoins(5000);
            user.addNews("Existing", "Keep this progress");
            state.setCurrentUser(user);
            PlantSpec spec = new PlantSpec(999, "Test Plant", PlantCategory.values()[0],
                    Set.of(), 100, 100, "20", "", "", "", "", "", 1, 1);
            controller = new CollectionController(state, manager,
                    new GraphicalMenuView((message, error) -> errors.add(error)),
                    new PlantData(Map.of("test plant", spec), Map.of(999, spec),
                            PlantLevelCostTable.defaults()),
                    new ZombieData(Map.of(), Map.of(), Map.of(), Map.of()));
        }
        void purchase() {
            controller.handle(new CollectionCommand(
                    CollectionCommand.Action.PURCHASE_PLANT, "Test Plant"));
        }
    }

    private class TestManager extends UserManager {
        boolean succeeds;
        int saves;
        TestManager(boolean succeeds) {
            super(directory.resolve("users.json").toString());
            this.succeeds = succeeds;
        }
        @Override public boolean save() { saves++; return succeeds; }
    }
}
