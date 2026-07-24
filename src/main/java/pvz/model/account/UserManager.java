package pvz.model.account;

import com.google.gson.reflect.TypeToken;
import pvz.model.BaseManager;

import java.util.*;

public class UserManager extends BaseManager<User> {
    public UserManager(String filePath) {
        super(filePath, new TypeToken<ArrayList<User>>(){}.getType());}

    public boolean userExists(String username) {
        return this.exists(u -> u.getUsername().equals(username));}

    public void updateDifficulty(String username, int newDifficulty) {
        User user = this.find(u -> u.getUsername().equals(username));
        if (user != null) {
            user.setDifficultyLevel(newDifficulty);
            this.save();
        }
    }
}

