package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.NewsCommand;

public class NewsMenuParser {
    public Command parse(String input) {
        String trimmedInput = input.trim();

        if (trimmedInput.equalsIgnoreCase("menu news show-unread")) {
            return new NewsCommand(NewsCommand.Action.SHOW_UNREAD);
        }
        else if (trimmedInput.equalsIgnoreCase("menu news show-all")) {
            return new NewsCommand(NewsCommand.Action.SHOW_ALL);
        }

        return new Command.EmptyCommand();
    }
}