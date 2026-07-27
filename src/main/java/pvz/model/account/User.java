package pvz.model.account;

import pvz.model.greenhouse.Greenhouse;
import pvz.model.shop.DailyOffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private Greenhouse greenhouse;
    private Set<String> storedBoosts;
    private int plantFoodCount = 0;
    private DailyOffer dailyOffer;

    private List<String> unlockedChapters;
    private int difficultyLevel = 3;

    private List<NewsItem> newsList;
    private List<PlayerPlant> unlockedPlants;
    private List<String> seenZombies;

    public User(String username, String passwordHash, String nickname, String email, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.stayLoggedIn = false;

        this.unlockedChapters = new ArrayList<>();
        this.unlockedChapters.add("ancient-egypt");

        this.unlockedPlants = new ArrayList<>();
        this.unlockedPlants.add(new PlayerPlant("peashooter"));
        this.unlockedPlants.add(new PlayerPlant("sunflower"));

        this.greenhouse = new Greenhouse();
        this.storedBoosts = new HashSet<>();
        this.newsList = new ArrayList<>();
        this.seenZombies = new ArrayList<>();
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
        if (amount < 0) return false;
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    public int getDiamonds() { return diamonds; }
    public void addDiamonds(int amount) { this.diamonds += amount; }

    public boolean spendDiamonds(int amount) {
        if (amount < 0) return false;
        if (this.diamonds >= amount) {
            this.diamonds -= amount;
            return true;
        }
        return false;
    }

    public boolean isChapterUnlocked(String chapterName) {
        if (this.unlockedChapters == null) {
            this.unlockedChapters = new ArrayList<>();
            this.unlockedChapters.add("ancient-egypt");
        }
        return this.unlockedChapters.contains(chapterName);
    }


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

    public List<PlayerPlant> getUnlockedPlants() {
        if (this.unlockedPlants == null) {
            this.unlockedPlants = new ArrayList<>();
            this.unlockedPlants.add(new PlayerPlant("peashooter"));
            this.unlockedPlants.add(new PlayerPlant("sunflower"));
        }
        return unlockedPlants;
    }

    public List<String> getSeenZombies() {
        if (this.seenZombies == null) this.seenZombies = new ArrayList<>();
        return seenZombies;
    }

    public void addPlant(PlayerPlant plant) {
        getUnlockedPlants().add(plant);
    }

    public PlayerPlant getOwnedPlant(String plantName) {
        for (PlayerPlant plant : getUnlockedPlants()) {
            if (plant.getPlantName().equalsIgnoreCase(plantName)) return plant;
        }
        return null;
    }

    public void addSeenZombie(String zombieName) {
        if (!getSeenZombies().contains(zombieName)) getSeenZombies().add(zombieName);
    }

    public Greenhouse getGreenhouse() {
        if (this.greenhouse == null) this.greenhouse = new Greenhouse();
        return greenhouse;
    }

    public Set<String> getStoredBoosts() {
        if (this.storedBoosts == null) this.storedBoosts = new HashSet<>();
        return storedBoosts;
    }

    public void addStoredBoost(String plantName) {
        getStoredBoosts().add(plantName.toLowerCase());
    }

    public void removeStoredBoost(String plantName) {
        getStoredBoosts().remove(plantName.toLowerCase());
    }

    public boolean hasStoredBoost(String plantName) {
        return getStoredBoosts().contains(plantName.toLowerCase());
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public boolean addPlantFood(int amount) {
        if (this.plantFoodCount + amount > 3) {
            return false;
        }
        this.plantFoodCount += amount;
        return true;
    }

    public DailyOffer getDailyOffer() {
        return dailyOffer;
    }

    public void setDailyOffer(DailyOffer dailyOffer) {
        this.dailyOffer = dailyOffer;
    }
}
