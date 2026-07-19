package pvz.model.service;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.utils.SystemMessage;
import pvz.model.utils.AuthValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {
    private final UserManager userManager;
    private User currentUser;

    public AuthService(UserManager userManager) {
        this.userManager = userManager;
    }

    public User processInitialRegistration(String username, String password, String confirm, String nickname, String email, String gender) throws Exception {

        if (!AuthValidator.isValidUsername(username)) throw new Exception(SystemMessage.INVALID_USERNAME.getMessage());

        SystemMessage passErr = AuthValidator.getPasswordWeaknessReason(password);
        if (passErr != null) throw new Exception(passErr.getMessage());

        if (!password.equals(confirm)) throw new Exception(SystemMessage.PASSWORDS_MISMATCH.getMessage());
        if (!AuthValidator.isValidNickname(nickname)) throw new Exception(SystemMessage.INVALID_NICKNAME.getMessage());
        if (!AuthValidator.isValidEmail(email)) throw new Exception(SystemMessage.INVALID_EMAIL.getMessage());
        if (!AuthValidator.isValidGender(gender)) throw new Exception(SystemMessage.INVALID_GENDER.getMessage());

        if (userManager.userExists(username)) throw new Exception("user with username " + username + " already exists");

        User newUser = new User(username, hashPasswordSHA256(password), nickname, email, gender);
        this.currentUser = newUser;
        userManager.add(currentUser);
        userManager.save();

        return newUser;
    }

    public boolean saveQuestionAnswer(int questionNumber, String answer, String answerConfirm) throws Exception {
        if (!answer.equals(answerConfirm)) {
            throw new Exception(SystemMessage.ANSWERS_MISMATCH.getMessage());
        }
        if (questionNumber < 0 || questionNumber > 3) {
            throw new Exception(SystemMessage.INVALID_QUESTION_NUMBER.getMessage());
        }

        currentUser.setSecurityQuestionNumber(questionNumber);
        currentUser.setSecurityAnswer(answer);
        userManager.save();

        return true;
    }

    public String hashPasswordSHA256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}