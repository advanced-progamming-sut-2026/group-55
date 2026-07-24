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
}