package pvz.model.utils;

public class AuthValidator {

    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9\\-]+$");
    }

    public static SystemMessage getPasswordWeaknessReason(String password) {
        if (password == null || password.length() < 8) return SystemMessage.WEAK_PASS_LENGTH;

        String allowedCharsRegex = "^[a-zA-Z0-9?><,\"'\\\\/:;|\\[\\]}{+=()*&^%$#!]+$";
        if (!password.matches(allowedCharsRegex)) return SystemMessage.WEAK_PASS_CHARS;

        if (password.equals(password.toLowerCase())) return SystemMessage.WEAK_PASS_UPPER;
        if (password.equals(password.toUpperCase())) return SystemMessage.WEAK_PASS_LOWER;
        if (!password.matches(".*\\d.*")) return SystemMessage.WEAK_PASS_DIGIT;

        String specialCharsRegex = ".*[?><,\"'\\\\/:;|\\[\\]}{+=()*&^%$#!].*";
        if (!password.matches(specialCharsRegex)) return SystemMessage.WEAK_PASS_SPECIAL;

        return null;
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null && nickname.matches("^.{3,30}$");
    }

    public static boolean isValidEmail(String email) {

        String regex = "^(?!.*\\.\\.)[a-zA-Z0-9]([a-zA-Z0-9.\\-_]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9.\\-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$";
        return email != null && email.matches(regex);
    }

    public static boolean isValidGender(String gender) {
        return gender != null && gender.matches("(?i)^(male|female)$");
    }
}