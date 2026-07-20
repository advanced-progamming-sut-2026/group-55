package pvz.model.utils;

public enum SystemMessage implements Message {

    MENU_REQUIRES_LOGIN("Please login to your account first to access this menu."),
    MENU_ALREADY_LOGGED_IN("You are already logged in."),

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
    USER_NOT_LOGGED_IN("You must be logged in."),

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

    NEWS_LOGIN_REQUIRED("Please login to your account first."),
    NEWS_NO_UNREAD("You have no new news."),
    NEWS_UNREAD_HEADER("-- Unread News --"),
    NEWS_EMPTY_INBOX("Your news inbox is empty."),
    NEWS_ALL_HEADER("-- All News --"),
    NEWS_STATUS_READ("[Read]"),
    NEWS_STATUS_NEW("[New]"),

    COLLECTION_NOT_ENOUGH_COINS("Not enough coins to purchase this plant."),
    COLLECTION_NOT_ENOUGH_SEEDS("Not enough seed packets to upgrade this plant."),
    COLLECTION_PLANT_PURCHASED("Plant purchased successfully."),
    COLLECTION_PLANT_UPGRADED("Plant upgraded successfully."),
    COLLECTION_ALREADY_OWNED("You already own this plant."),
    COLLECTION_ITEM_NOT_FOUND("The requested plant or zombie does not exist."),

    COLLECTION_HEADER_ALL_PLANTS("All Plants in Game"),
    COLLECTION_HEADER_YOUR_PLANTS("Your Unlocked Plants"),
    COLLECTION_HEADER_ZOMBIES("Seen Zombies"),

    LOADING_DATA_FAILED("Failed to load game data!"),

    PLANT_SELECTION_HEADER_UNLOCKED("-- Your Unlocked Plants --"),
    PLANT_SELECTION_NO_PLANTS("No plants selected yet."),

    PLANT_SELECTION_INVALID_NAME("Invalid plant name."),
    PLANT_SELECTION_LOCKED("Plant is locked."),
    PLANT_SELECTION_ALREADY_SELECTED("Plant is already selected."),
    PLANT_SELECTION_SLOTS_FULL("Your plant slots are full."),
    PLANT_SELECTION_NOT_IN_SELECTION("Plant is not in your selection."),

    PLANT_SELECTION_ADDED("Plant added to selection successfully."),
    PLANT_SELECTION_REMOVED("Plant removed from selection."),

    PLANT_SELECTION_NOT_OWNED("You do not own this plant."),
    PLANT_SELECTION_ALREADY_BOOSTED("This plant is already boosted."),
    PLANT_SELECTION_NOT_ENOUGH_DIAMONDS("Not enough diamonds."),

    PLANT_SELECTION_BOOSTED_SUCCESS("Plant boosted successfully!"),

    PLANT_SELECTION_EMPTY_START("You must select at least one plant to start the game!"),
    PLANT_SELECTION_START_GAME("Starting game with selected plants");

    private final String message;

    SystemMessage(String message){
        this.message = message; }

    @Override
    public String getMessage() { return message; }
}