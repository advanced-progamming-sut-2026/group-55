package pvz.model.utils;

public enum SystemMessage implements Message {

    SUCCESS_REGISTRATION("Registration successful. Please pick a security question:\n1. What is your favorite color?\n2. What is your pet's name?\n3. What city were you born in?"),
    SUCCESS_CREATION("user created successfully!"),
    LOGIN_SUCCESS("user logged in successfully!"),
    PASSWORD_CHANGED_SUCCESS("password changed successfully!"),
    LOGOUT_SUCCESS("Logged out successfully."),

    INVALID_COMMAND("invalid command"),
    MENU_ENTERED_REGISTER("entered register menu"),
    MENU_ENTERED_MAIN("menu entered main"),
    EXITING_GAME("Exiting game"),

    INVALID_USERNAME("invalid username format"),
    INVALID_NICKNAME("invalid nickname length"),
    INVALID_EMAIL("invalid email format"),
    INVALID_GENDER("invalid gender"),
    PASSWORDS_MISMATCH("passwords do not match"),
    WEAK_PASS_LENGTH("weak password: password length must be at least 8 characters"),
    WEAK_PASS_CHARS("weak password: password contains invalid characters"),
    WEAK_PASS_UPPER("weak password: must contain at least one uppercase letter"),
    WEAK_PASS_LOWER("weak password: must contain at least one lowercase letter"),
    WEAK_PASS_DIGIT("weak password: must contain at least one digit"),
    WEAK_PASS_SPECIAL("weak password: must contain at least one special character"),

    LOGIN_FAILED("username and password do not match"),
    FORGET_PASS_FAILED("username or email is incorrect"),
    ANSWERS_MISMATCH("answers do not match"),
    ANSWER_INCORRECT("answer is incorrect. Returning to login menu."),
    INVALID_QUESTION_NUMBER("invalid question number"),
    ENTER_NEW_PASSWORD("Please enter your new password:"),

    LOGOUT_REQUIRED_MAIN("Use logout command to exit Main menu."),
    LOGOUT_NOT_ALLOWED("use the 'menu exit' to leave this menu."),

    INVALID_DIFFICULTY("difficulty level must be between 1 and 5"),

    PROFILE_SAME_USERNAME("Error: New username cannot be the same as the current username."),
    PROFILE_SAME_NICKNAME("Error: New nickname cannot be the same as the current nickname."),
    PROFILE_SAME_EMAIL("Error: New email cannot be the same as the current email."),
    PROFILE_WRONG_OLD_PASSWORD("Error: Old password is incorrect."),
    PROFILE_SAME_PASSWORD("Error: New password cannot be the same as the current password."),

    PROFILE_USERNAME_CHANGED("Username changed successfully."),
    PROFILE_NICKNAME_CHANGED("Nickname changed successfully."),
    PROFILE_EMAIL_CHANGED("Email changed successfully."),
    PROFILE_PASSWORD_CHANGED("Password changed successfully."),

    CHAPTER_LOCKED("Chapter is locked!"),

    USER_NOT_LOGGED_IN("You must be logged in.");

    private final String message;

    SystemMessage(String message){
        this.message = message; }

    @Override
    public String getMessage() { return message; }
}