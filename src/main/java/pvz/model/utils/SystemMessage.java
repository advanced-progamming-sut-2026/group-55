package pvz.model.utils;

public enum SystemMessage implements Message {

    MENU_REQUIRES_LOGIN("Please login to your account first to access this menu."),
    MENU_ALREADY_LOGGED_IN("You are already logged in."),
    MENU_NAVIGATION_NOT_ALLOWED("menu navigation is not allowed"),

    SUCCESS_REGISTRATION("Registration successful. Please pick a security question:\n1. What is your favorite color?\n2. What is your pet's name?\n3. What city were you born in?"),
    SUCCESS_CREATION("user created successfully!"),
    LOGIN_SUCCESS("user logged in successfully!"),
    PASSWORD_CHANGED_SUCCESS("password changed successfully!"),
    LOGOUT_SUCCESS("Logged out successfully."),

    ENTERED_REGISTER("entered register menu"),
    ENTERED_MAIN("entered main menu"),
    ENTERED_GAME("entered game menu"),
    ENTERED_CHAPTER("entered chapter"),
    ENTERED_COLLECTION("entered collection menu"),
    ENTERED_GREENHOUSE("entered greenhouse"),
    ENTERED_TRAVEL_LOG("entered travel log"),
    SHOWING_LEADERBOARD("showing leaderboard"),

    INVALID_COMMAND("invalid command"),
    INVALID_USERNAME("invalid username format"),
    INVALID_NICKNAME("invalid nickname length"),
    INVALID_EMAIL("invalid email format"),
    INVALID_GENDER("invalid gender"),

    PASSWORDS_MISMATCH("passwords dont match"),
    WEAK_PASS_LENGTH("weak password: password length must be at least 8 characters"),
    WEAK_PASS_CHARS("weak password: password contains invalid characters"),
    WEAK_PASS_UPPER("weak password: must contain at least one uppercase letter"),
    WEAK_PASS_LOWER("weak password: must contain at least one lowercase letter"),
    WEAK_PASS_DIGIT("weak password: must contain at least one digit"),
    WEAK_PASS_SPECIAL("weak password: must contain at least one special character"),

    LOGIN_FAILED("username and password dont match"),
    FORGET_PASS_FAILED("username or email is incorrect"),
    ANSWERS_MISMATCH("answers dont match"),
    ANSWER_INCORRECT("answer is incorrect. Returning to login menu."),
    INVALID_QUESTION_NUMBER("invalid question number"),
    ENTER_NEW_PASSWORD("Please enter your new password:"),

    LOGOUT_REQUIRED_MAIN("Use logout command to exit Main menu."),
    LOGOUT_NOT_ALLOWED("use the 'menu exit' to leave this menu."),
    USER_NOT_LOGGED_IN("You must be logged in."),

    INVALID_DIFFICULTY("difficulty level must be between 1 and 5"),

    PROFILE_SAME_USERNAME(" New username cant be the same as the current username."),
    PROFILE_SAME_NICKNAME("New nickname cant be the same as the current nickname."),
    PROFILE_SAME_EMAIL("New email cant be the same as the current email."),
    PROFILE_WRONG_OLD_PASSWORD("Old password is incorrect."),
    PROFILE_SAME_PASSWORD("New password cant be the same as the current password."),

    PROFILE_USERNAME_CHANGED("Username changed successfully."),
    PROFILE_NICKNAME_CHANGED("Nickname changed successfully."),
    PROFILE_EMAIL_CHANGED("Email changed successfully."),
    PROFILE_PASSWORD_CHANGED("Password changed successfully."),

    PROFILE_INVALID_USERNAME("Invalid username format."),
    PROFILE_USERNAME_EXISTS("Username already exists."),
    PROFILE_INVALID_NICKNAME("Invalid nickname format."),
    PROFILE_INVALID_EMAIL("Invalid email format."),

    CHAPTER_LOCKED("Chapter is locked!"),

    NEWS_NO_UNREAD("You have no new news."),
    NEWS_UNREAD_HEADER("-- Unread News --"),
    NEWS_EMPTY_INBOX("Your news inbox is empty."),
    NEWS_ALL_HEADER("-- All News --"),
    NEWS_STATUS_READ("[Read]"),
    NEWS_STATUS_NEW("[New]"),

    COLLECTION_NOT_ENOUGH_COINS("Not enough coins to purchase this plant."),
    COLLECTION_NOT_ENOUGH_SEEDS("Not enough coins or seed packets."),
    COLLECTION_PLANT_UPGRADED("Plant upgraded successfully."),
    COLLECTION_PLANT_PURCHASED("Plant purchased successfully."),
    COLLECTION_ALREADY_OWNED("You already own this plant."),
    COLLECTION_ITEM_NOT_FOUND("The requested plant or zombie does not exist."),
    COLLECTION_MAX_LEVEL_REACHED("already at maximum level."),
    COLLECTION_HEADER_ALL_PLANTS("All Plants in Game"),
    COLLECTION_HEADER_YOUR_PLANTS("Your Unlocked Plants"),
    COLLECTION_HEADER_ZOMBIES("Seen Zombies"),

    PLANT_SELECTION_HEADER_UNLOCKED("-- Your Unlocked Plants --"),
    PLANT_SELECTION_NO_PLANTS("No plants selected yet."),

    PLANT_SELECTION_INVALID_NAME("Invalid plant name."),
    PLANT_SELECTION_LOCKED("Plant is locked."),
    PLANT_SELECTION_ALREADY_SELECTED("Plant is already selected."),
    PLANT_SELECTION_SLOTS_FULL("Your plant slots are full."),
    PLANT_SELECTION_NOT_IN_SELECTION("Plant is not in your selection."),

    PLANT_SELECTION_ADDED("Plant added to selection successfully."),
    PLANT_SELECTION_REMOVED("Plant removed from selection."),

    PLANT_SELECTION_NOT_OWNED("You dont own this plant."),
    PLANT_SELECTION_ALREADY_BOOSTED("This plant is already boosted."),
    PLANT_SELECTION_NOT_ENOUGH_DIAMONDS("Not enough diamonds."),

    PLANT_SELECTION_BOOSTED_SUCCESS("Plant boosted successfully!"),

    PLANT_SELECTION_EMPTY_START("You must select at least one plant to start the game!"),
    PLANT_SELECTION_START_GAME("Starting game with selected plants"),


    GREENHOUSE_NOT_ENOUGH_COINS("Not enough coins to unlock this pot."),
    GREENHOUSE_NOT_ENOUGH_DIAMONDS("Not enough diamonds."),
    GREENHOUSE_PLANTED_SUCCESS("Plant planted successfully."),
    GREENHOUSE_ALREADY_READY("This plant is already ready."),
    GREENHOUSE_INVALID_COORDINATES("invalid coordinates"),
    GREENHOUSE_LOCKED("pot is locked"),
    GREENHOUSE_NOT_EMPTY("pot is not empty"),
    GREENHOUSE_EMPTY("no plant in this pot"),
    GREENHOUSE_NOT_READY("plant is not ready yet"),
    GREENHOUSE_NOT_GROWING("plant is not growing"),
    GREENHOUSE_ALREADY_UNLOCKED("pot is already unlocked"),
    GREENHOUSE_COLLECTED_SUCCESS("plant collected successfully"),
    GREENHOUSE_GROWN_SUCCESS("plant growth accelerated"),
    GREENHOUSE_UNLOCKED_SUCCESS("pot unlocked successfully"),

    SHOP_INVALID_COUNT("Invalid count. Must be at least 1."),
    SHOP_INVALID_ITEM_ID("Invalid item ID."),
    SHOP_UNKNOWN_ITEM_TYPE("Unknown item type."),
    SHOP_POTS_MAX_CAPACITY("Not enough locked pots available. Max capacity is 20."),
    SHOP_NOT_ENOUGH_COINS("Not enough coins."),
    SHOP_PLANT_FOOD_MAX_CAPACITY("Cannot hold more than 3 Plant Foods."),
    SHOP_NOT_ENOUGH_DIAMONDS("Not enough diamonds."),
    SHOP_NO_UNLOCKED_PLANTS("No unlocked plants available."),
    SHOP_PLANT_TYPE_REQUIRED("Plant type (-t) is required for selected seed packets."),
    SHOP_PLANT_NOT_UNLOCKED("You can only buy seeds for unlocked plants."),
    SHOP_DAILY_OFFER_ONCE("Daily offer can only be purchased once."),
    SHOP_DAILY_OFFER_ALREADY_BOUGHT("You have already purchased today's daily offer."),
    SHOP_DAILY_OFFER_NO_PLANTS("No unlocked plants available for a daily offer."),

    SHOP_HEADER_ITEMS("--- Shop Items ---"),
    SHOP_HEADER_DAILY("--- Daily Offer ---"),
    SHOP_PURCHASE_SUCCESS("Purchase successful!"),
    SHOP_PURCHASE_FAILED("Purchase failed: "),
    SHOP_NOT_LOGGED_IN("You must be logged in to use the shop."),
    SHOP_INVALID_COMMAND("Invalid Shop Command."),

    LOADING_DATA_FAILED("Failed to load game data!");


    private final String message;

    SystemMessage(String message){
        this.message = message; }

    @Override
    public String getMessage() { return message; }

    private static final String[] SECURITY_QUESTIONS = {
            "What is your favorite color?",
            "What is your pet's name?",
            "What city were you born in?"
    };

    public static String getSecurityQuestion(int number) {
        if (number >= 1 && number <= SECURITY_QUESTIONS.length) {
            return SECURITY_QUESTIONS[number - 1];
        }
        return "Invalid security question.";
    }
}