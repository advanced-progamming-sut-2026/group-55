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

        if (!(command instanceof NewsCommand newsCmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();

        if (currentUser == null) {
            view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
            return null;
        }

        if (newsCmd.getAction() == NewsCommand.Action.SHOW_UNREAD) {
            showUnreadNews(currentUser);
        } else if (newsCmd.getAction() == NewsCommand.Action.SHOW_ALL) {
            showAllNews(currentUser);
        }

        return null;
    }

    private void showUnreadNews(User user) {
        List<NewsItem> unread = user.getUnreadNews();

        if (unread.isEmpty()) {
            view.showSuccess(SystemMessage.NEWS_NO_UNREAD.getMessage());
            return;
        }

        view.showSuccess(SystemMessage.NEWS_UNREAD_HEADER.getMessage());

        for (NewsItem news : unread) {
            showNews(news);
        }

        user.markAllAsRead();
        userManager.save();
    }

    private void showAllNews(User user) {
        List<NewsItem> all = user.getAllNews();

        if (all.isEmpty()) {
            view.showSuccess(SystemMessage.NEWS_EMPTY_INBOX.getMessage());
            return;
        }

        view.showSuccess(SystemMessage.NEWS_ALL_HEADER.getMessage());

        for (NewsItem news : all) {
            String status = news.isRead()
                    ? SystemMessage.NEWS_STATUS_READ.getMessage()
                    : SystemMessage.NEWS_STATUS_NEW.getMessage();

            view.showSuccess(status);
            showNews(news);
        }
    }

    private void showNews(NewsItem news) {
        view.showSuccess("Date: " + news.getDate());
        view.showSuccess("Title: " + news.getTitle());
        view.showSuccess("Message: " + news.getMessage());
    }
}
