package pvz.model.account;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NewsItem {

    private String title;
    private String message;
    private String date;
    private boolean isRead;

    public NewsItem(String title, String message) {
        this.title = title;
        this.message = message;
        this.date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.isRead = false;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }
}
