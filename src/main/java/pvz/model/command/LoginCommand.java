package pvz.model.command;

public class LoginCommand implements Command {

    public enum Action { LOGIN, FORGET_PASSWORD, ANSWER }

    private final Action action;
    private final String username;
    private final String password;
    private final boolean stayLoggedIn;
    private final String email;
    private final String answer;

    private LoginCommand(Action action, String username, String password, boolean stayLoggedIn,
                         String email, String answer) {
        this.action = action;
        this.username = username;
        this.password = password;
        this.stayLoggedIn = stayLoggedIn;
        this.email = email;
        this.answer = answer;
    }

    public static LoginCommand createLogin(String username, String password, boolean stayLoggedIn) {
        return new LoginCommand(Action.LOGIN, username, password, stayLoggedIn, null, null);
    }

    public static LoginCommand createForgetPassword(String username, String email) {
        return new LoginCommand(Action.FORGET_PASSWORD, username, null, false, email, null);
    }

    public static LoginCommand createAnswer(String answer) {
        return new LoginCommand(Action.ANSWER, null, null, false, null, answer);
    }

    public Action getAction() { return action; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public String getEmail() { return email; }
    public String getAnswer() { return answer; }
}