package pvz.model.account;

public class NewsItem {
    private String message;
    private boolean isRead;

    public NewsItem(String message) {
        this.message = message;
        this.isRead = false;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }
}