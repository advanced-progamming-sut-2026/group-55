package pvz.controller;

import pvz.model.account.NewsItem;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.NewsCommand;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

import java.util.List;

public class NewsController extends BaseController {

    public NewsController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (command instanceof NewsCommand) {
            NewsCommand newsCmd = (NewsCommand) command;
            User currentUser = appState.getCurrentUser();

            if (newsCmd.getAction() == NewsCommand.Action.SHOW_UNREAD) {
                List<NewsItem> unread = currentUser.getUnreadNews();
                if (unread.isEmpty()) {
                    view.showSuccess(SystemMessage.NEWS_NO_UNREAD.getMessage());
                } else {
                    view.showSuccess(SystemMessage.NEWS_UNREAD_HEADER.getMessage());
                    for (NewsItem news : unread) {
                        view.showSuccess("* " + news.getMessage());
                    }
                    currentUser.markAllAsRead();
                    userManager.save();
                }
            }
            else if (newsCmd.getAction() == NewsCommand.Action.SHOW_ALL) {
                List<NewsItem> all = currentUser.getAllNews();
                if (all.isEmpty()) {
                    view.showSuccess(SystemMessage.NEWS_EMPTY_INBOX.getMessage());
                } else {
                    view.showSuccess(SystemMessage.NEWS_ALL_HEADER.getMessage());
                    for (NewsItem news : all) {
                        String status = news.isRead() ?
                                SystemMessage.NEWS_STATUS_READ.getMessage() :
                                SystemMessage.NEWS_STATUS_NEW.getMessage();

                        view.showSuccess(status + " " + news.getMessage());
                    }
                }
            }
            return null;
        }

        return null;
    }
}