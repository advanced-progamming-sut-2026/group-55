package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.ShopCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopParser {

    private static final Pattern BUY_PATTERN = Pattern.compile("^shop buy -i\\s+(\\d+)\\s+-n\\s+(\\d+)(?:\\s+-t\\s+([a-zA-Z0-9_]+))?$");

    public Command parse(String input) {
        input = input.trim();

        if (input.equalsIgnoreCase("shop list")) {
            return new ShopCommand(ShopCommand.Action.SHOW_LIST);
        }

        if (input.equalsIgnoreCase("shop daily")) {
            return new ShopCommand(ShopCommand.Action.SHOW_DAILY);
        }

        if (input.toLowerCase().startsWith("shop buy")) {
            Matcher matcher = BUY_PATTERN.matcher(input);
            if (matcher.matches()) {
                int itemId = Integer.parseInt(matcher.group(1));
                int count = Integer.parseInt(matcher.group(2));
                String plantType = matcher.group(3);

                return new ShopCommand(ShopCommand.Action.BUY, itemId, count, plantType);
            }
        }

        return null;
    }
}