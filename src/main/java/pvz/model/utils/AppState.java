package pvz.model.utils;

import pvz.model.account.User;

public class AppState {
    private MenuName currentMenu = MenuName.REGISTER;
    private User currentUser;
    private boolean running = true;

    private boolean stayLoggedIn = false;

    public MenuName getCurrentMenu() {
        return currentMenu;
    }

    public void setCurrentMenu(MenuName currentMenu) {
        this.currentMenu = currentMenu;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    private int difficultyLevel = 3;

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int level) {
        this.difficultyLevel = level;
    }

}