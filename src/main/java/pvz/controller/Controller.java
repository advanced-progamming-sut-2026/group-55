package pvz.controller;

import pvz.model.command.Command;
import pvz.model.utils.Message;

public interface Controller {
    Message handle(Command command);
}