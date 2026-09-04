package pvz.graphics;

import java.util.Objects;
import java.util.function.BiConsumer;
import pvz.view.MenuView;

public final class GraphicalMenuView implements MenuView {
    private final BiConsumer<String, Boolean> messageConsumer;

    public GraphicalMenuView(BiConsumer<String, Boolean> messageConsumer) {
        this.messageConsumer = Objects.requireNonNull(
                messageConsumer,
                "message consumer cannot be null"
        );
    }

    @Override
    public void showSuccess(String message) {
        messageConsumer.accept(message, false);
    }

    @Override
    public void showError(String errorMessage) {
        messageConsumer.accept(errorMessage, true);
    }

    @Override
    public void showMessage(String message) {
        messageConsumer.accept(message, false);
    }

    @Override
    public void showRegisterWelcome() {
        // The plant selection screen has no registration welcome message.
    }
}
