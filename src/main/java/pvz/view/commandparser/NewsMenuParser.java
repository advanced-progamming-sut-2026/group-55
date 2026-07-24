package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.NewsCommand;

public class NewsMenuParser {

    public Command parse(String input) {
        if (input == null || input.isBlank())
            return new Command.EmptyCommand();

        String trimmed = input.trim();

        if (trimmed.equals("menu news show-unread")) {
            return new NewsCommand(NewsCommand.Action.SHOW_UNREAD);
        }

        if (trimmed.equals("menu news show-all")) {
            return new NewsCommand(NewsCommand.Action.SHOW_ALL);
        }

        return new Command.RawTextCommand(trimmed);
    }
}