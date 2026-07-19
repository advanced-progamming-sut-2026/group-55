package pvz.model.account;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private final String gender;
    private int securityQuestionNumber;
    private String securityAnswer;
    private boolean stayLoggedIn;

    private int coins = 0;
    private int diamonds = 0;

    private int gamesPlayed = 0;
    private int clearedStages = 0;
    private int maxMewPoint = 0;

    private List<String> unlockedChapters;

    private int difficultyLevel = 3;

    public User(String username, String passwordHash, String nickname, String email, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.unlockedChapters = new ArrayList<>();
        this.unlockedChapters.add("chapter1");
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() { return passwordHash; }
    public void setPassword(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getSecurityQuestionNumber() { return securityQuestionNumber; }
    public void setSecurityQuestionNumber(int n) { this.securityQuestionNumber = n; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String a) { this.securityAnswer = a; }

    public boolean isStayLoggedIn() {return stayLoggedIn;}
    public void setStayLoggedIn(boolean stayLoggedIn) {this.stayLoggedIn = stayLoggedIn;}

    public int getCoins() { return coins; }
    public void addCoins(int amount) { this.coins += amount; }

    public int getDiamonds() { return diamonds; }
    public void addDiamonds(int amount) { this.diamonds += amount; }

    public boolean isChapterUnlocked(String chapterName) {return unlockedChapters.contains(chapterName);}

    public int getDifficultyLevel() {return difficultyLevel;}
    public void setDifficultyLevel(int difficultyLevel) {this.difficultyLevel = difficultyLevel;}

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getClearedStages() { return clearedStages; }
    public void setClearedStages(int clearedStages) { this.clearedStages = clearedStages; }

    public int getMaxMewPoint() { return maxMewPoint; }
    public void setMaxMewPoint(int maxMewPoint) { this.maxMewPoint = maxMewPoint; }

}