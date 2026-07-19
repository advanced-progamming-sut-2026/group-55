package pvz.model.Command;

public interface Command {

    class MenuEnterCommand implements Command {
        private final String menuName;

        public MenuEnterCommand(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuName() { return menuName; }
    }

    class MenuShowCurrentCommand implements Command {}

    class MenuExitCommand implements Command {}

    class MenuLogoutCommand implements Command {}

    class RawTextCommand implements Command {
        private final String text;

        public RawTextCommand(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }
    
    class EmptyCommand implements Command {}
}