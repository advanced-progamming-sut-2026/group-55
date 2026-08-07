package pvz.view;

public class ConsoleView implements MenuView {
    @Override
    public void showSuccess(String message) {
        System.out.println(message);
    }

    @Override
    public void showError(String errorMessage) {
        System.out.println(errorMessage);
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showRegisterWelcome() {
        showMessage("Welcome to Plants vs Zombies!");
        showMessage("To start playing, please register a new account.");
        showMessage("Already have an account? Just login!");
    }
}
