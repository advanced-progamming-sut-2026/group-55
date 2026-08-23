package pvz.controller;

import java.util.List;
import java.util.Objects;
import pvz.model.account.UserManager;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelProgressService;
import pvz.model.adventure.LevelSpec;
import pvz.model.command.ChapterCommand;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public final class ChapterController extends BaseController {
    private final LevelCatalog levelCatalog;
    private final LevelProgressService levelProgressService;

    public ChapterController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            LevelCatalog levelCatalog,
            LevelProgressService levelProgressService
    ) {
        super(appState, userManager, view);
        this.levelCatalog = Objects.requireNonNull(
                levelCatalog,
                "level catalog cannot be null"
        );
        this.levelProgressService = Objects.requireNonNull(
                levelProgressService,
                "level progress service cannot be null"
        );
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof ChapterCommand chapterCommand)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }
        switch (chapterCommand.getAction()) {
            case SHOW_LEVELS -> showLevels();
            case ENTER_LEVEL -> enterLevel(chapterCommand.getLevel());
        }
        return null;
    }

    private void showLevels() {
        List<LevelSpec> levels = selectedChapterLevels();
        if (levels.isEmpty()) {
            view.showError("This chapter has no configured levels.");
            return;
        }
        view.showMessage("--- LEVELS ---");
        for (LevelSpec level : levels) {
            LevelProgressService.LevelState state =
                    levelProgressService.state(
                            appState.getCurrentUser(),
                            level
                    );
            view.showMessage(
                    level.number() + ". " + level.name()
                            + " [" + level.id() + "] - " + state
            );
        }
        view.showMessage("Use: select level -l <id-or-number>");
    }

    private void enterLevel(String idOrNumber) {
        String chapterId = appState.getSelectedChapter();
        LevelSpec level = levelCatalog.findLevelInChapter(
                chapterId,
                idOrNumber
        );
        if (level == null) {
            view.showError("Unknown level in selected chapter.");
            return;
        }
        if (!levelProgressService.isUnlocked(
                appState.getCurrentUser(),
                level
        )) {
            view.showError(
                    "This level is locked. Complete the previous level first."
            );
            return;
        }
        appState.setSelectedLevelId(level.id());
        appState.setCurrentMenu(MenuName.PLANT_SELECTION);
        view.showSuccess(
                "Level selected: " + level.name()
                        + ". Select plants, then use 'start game'."
        );
    }

    private List<LevelSpec> selectedChapterLevels() {
        String chapterId = appState.getSelectedChapter();
        if (chapterId == null) {
            return List.of();
        }
        return levelCatalog.levelsInChapter(chapterId);
    }

    @Override
    protected void handleMenuExit() {
        appState.setSelectedChapter(null);
        appState.setSelectedLevelId(null);
        super.handleMenuExit();
    }
}
