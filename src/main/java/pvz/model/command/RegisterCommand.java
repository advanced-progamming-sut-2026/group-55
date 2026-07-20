package pvz.model.command;

public class RegisterCommand implements Command {

    public enum Action {
        REGISTER,
        PICK_QUESTION
    }

    private final Action action;

    private final String username;
    private final String password;
    private final String passwordConfirm;
    private final String nickname;
    private final String email;
    private final String gender;

    private final Integer questionNumber;
    private final String answer;
    private final String answerConfirm;

    private RegisterCommand(Action action, String username, String password, String passwordConfirm,
                            String nickname, String email, String gender,
                            Integer questionNumber, String answer, String answerConfirm) {
        this.action = action;
        this.username = username;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.questionNumber = questionNumber;
        this.answer = answer;
        this.answerConfirm = answerConfirm;
    }

    public static RegisterCommand createRegister(String username, String password, String passwordConfirm,
                                                 String nickname, String email, String gender) {
        return new RegisterCommand(Action.REGISTER, username, password, passwordConfirm,
                nickname, email, gender, null, null, null);
    }

    public static RegisterCommand createPickQuestion(int questionNumber, String answer, String answerConfirm) {
        return new RegisterCommand(Action.PICK_QUESTION, null, null, null,
                null, null, null, questionNumber, answer, answerConfirm);
    }

    public Action getAction() { return action; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public Integer getQuestionNumber() { return questionNumber; }
    public String getAnswer() { return answer; }
    public String getAnswerConfirm() { return answerConfirm; }
}