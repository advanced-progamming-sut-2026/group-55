package pvz.model.account;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;
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

    private List<NewsItem> newsList = new ArrayList<>();
    private List<PlayerPlant> unlockedPlants = new ArrayList<>();
    private List<String> seenZombies = new ArrayList<>();

    public User(String username, String passwordHash, String nickname, String email, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.stayLoggedIn = false;

        this.unlockedChapters = new ArrayList<>();
        this.unlockedChapters.add("ancient_egypt");

        this.unlockedPlants.add(new PlayerPlant("peashooter"));
        this.unlockedPlants.add(new PlayerPlant("sunflower"));
    }


    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return passwordHash; }
    public void setPassword(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void setGender(String gender) { this.gender = gender; }
    public String getGender() { return gender; }

    public int getSecurityQuestionNumber() { return securityQuestionNumber; }
    public void setSecurityQuestionNumber(int n) { this.securityQuestionNumber = n; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String a) { this.securityAnswer = a; }

    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public void setStayLoggedIn(boolean stayLoggedIn) { this.stayLoggedIn = stayLoggedIn; }

    public int getCoins() { return coins; }
    public void addCoins(int amount) { this.coins += amount; }

    public boolean spendCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;}

        return false;
    }

    public int getDiamonds() { return diamonds; }
    public void addDiamonds(int amount) { this.diamonds += amount; }

    public boolean isChapterUnlocked(String chapterName) { return unlockedChapters.contains(chapterName); }
    public void unlockChapter(String chapterName) {
        if (!this.unlockedChapters.contains(chapterName)) {
            this.unlockedChapters.add(chapterName);}}

    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getClearedStages() { return clearedStages; }
    public void setClearedStages(int clearedStages) { this.clearedStages = clearedStages; }

    public int getMaxMewPoint() { return maxMewPoint; }
    public void setMaxMewPoint(int maxMewPoint) { this.maxMewPoint = maxMewPoint; }

    public void addNews(String message) {
        if (this.newsList == null) this.newsList = new ArrayList<>();
        this.newsList.add(new NewsItem(message));
    }

    public List<NewsItem> getUnreadNews() {
        if (this.newsList == null) return new ArrayList<>();
        return newsList.stream().filter(n -> !n.isRead()).toList();
    }

    public List<NewsItem> getAllNews() {
        if (this.newsList == null) return new ArrayList<>();
        return newsList;
    }

    public void markAllAsRead() {
        if (this.newsList == null) return;
        for (NewsItem news : newsList) news.setRead(true);
    }

    public boolean hasUnreadNews() {
        if (this.newsList == null) return false;
        return newsList.stream().anyMatch(n -> !n.isRead());
    }

    public List<PlayerPlant> getUnlockedPlants() { return unlockedPlants; }
    public List<String> getSeenZombies() { return seenZombies; }

    public void addPlant(PlayerPlant plant) { this.unlockedPlants.add(plant); }

    public PlayerPlant getOwnedPlant(String plantName) {
        for (PlayerPlant plant : unlockedPlants) {
            if (plant.getPlantName().equalsIgnoreCase(plantName)) return plant;
        }
        return null;
    }

    public void addSeenZombie(String zombieName) {
        if (!seenZombies.contains(zombieName)) seenZombies.add(zombieName);
    }
    public boolean spendDiamonds(int amount) {
        if (this.diamonds >= amount) {
            this.diamonds -= amount;
            return true;
        }
        return false;
    }
}