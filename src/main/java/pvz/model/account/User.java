package pvz.model.account;

import pvz.model.currency.CurrencyWallet;
import pvz.model.greenhouse.Greenhouse;
import pvz.model.shop.DailyOffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class User implements CurrencyWallet {

    private static final int MAX_PLANT_FOOD = 3;
    private static final String DEFAULT_CHAPTER = "ancient-egypt";

    private static final List<String> DEFAULT_UNLOCKED_PLANT_NAMES = List.of(
            "Sunflower",
            "Sunflowertwin",
            "Sunshroom",
            "PrimalSunflower",
            "Peashooter",
            "Repeater",
            "Threepeater",
            "SnowPea",
            "Rotobaga",
            "PeaPod",
            "SplitPea",
            "Citron",
            "BowlingBulb",
            "FirePeashooter",
            "Starfruit",
            "GooPeashooter",
            "MegaGatlingPea",
            "Seashroom",
            "Puffshroom",
            "Cactus",
            "Fumeshroom",
            "Cabbagepult",
            "Kernelpult",
            "Melonpult",
            "WinterMelon",
            "Wallnut",
            "Tallnut",
            "Explodeonut",
            "Pumpkin"
    );

    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;

    private int securityQuestionNumber;
    private String securityAnswer;
    private boolean stayLoggedIn;

    private int coins;
    private int diamonds;

    private int gamesPlayed;
    private int clearedStages;
    private int maxMewPoint;

    private Greenhouse greenhouse;
    private Set<String> storedBoosts;
    private int plantFoodCount;
    private DailyOffer dailyOffer;

    private List<String> unlockedChapters;
    private AdventureProgress adventureProgress;
    private int difficultyLevel = 3;
    private int gameSpeed=1;
    private boolean showGrid=false;
    private boolean debugMode=false;

    private List<NewsItem> newsList;
    private List<PlayerPlant> unlockedPlants;
    private List<String> seenZombies;

    public User(
            String username,
            String passwordHash,
            String nickname,
            String email,
            String gender
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.stayLoggedIn = false;

        this.unlockedChapters = new ArrayList<>();
        this.unlockedChapters.add(DEFAULT_CHAPTER);
        this.adventureProgress = new AdventureProgress();

        this.unlockedPlants = createDefaultUnlockedPlants();

        this.greenhouse = new Greenhouse();
        this.storedBoosts = new HashSet<>();
        this.newsList = new ArrayList<>();
        this.seenZombies = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getSecurityQuestionNumber() {
        return securityQuestionNumber;
    }

    public void setSecurityQuestionNumber(int number) {
        this.securityQuestionNumber = number;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String answer) {
        this.securityAnswer = answer;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    @Override
    public int getCoins() {
        return coins;
    }

    @Override
    public void addCoins(int amount) {
        long result = (long) coins + amount;

        coins = result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    public boolean spendCoins(int amount) {
        if (amount < 0 || coins < amount) {
            return false;
        }

        coins -= amount;
        return true;
    }

    @Override
    public int getDiamonds() {
        return diamonds;
    }

    @Override
    public void addDiamonds(int amount) {
        long result = (long) diamonds + amount;

        diamonds = result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    public boolean spendDiamonds(int amount) {
        if (amount < 0 || diamonds < amount) {
            return false;
        }

        diamonds -= amount;
        return true;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void incrementGamesPlayed() {
        if (gamesPlayed < Integer.MAX_VALUE) {
            gamesPlayed++;
        }
    }

    public int getClearedStages() {
        return clearedStages;
    }

    public void setClearedStages(int clearedStages) {
        this.clearedStages = clearedStages;
    }

    public int getMaxMewPoint() {
        return maxMewPoint;
    }

    public void setMaxMewPoint(int maxMewPoint) {
        this.maxMewPoint = maxMewPoint;
    }

    public Greenhouse getGreenhouse() {
        if (greenhouse == null) {
            greenhouse = new Greenhouse();
        }

        return greenhouse;
    }

    public List<PlayerPlant> getUnlockedPlants() {
        if (unlockedPlants == null) {
            unlockedPlants = createDefaultUnlockedPlants();
        } else {
            Set<String> existingPlantNames = new HashSet<>();
            for (PlayerPlant p : unlockedPlants) {
                existingPlantNames.add(p.getPlantName().toLowerCase());
            }

            for (String defaultName : DEFAULT_UNLOCKED_PLANT_NAMES) {
                if (!existingPlantNames.contains(defaultName.toLowerCase())) {
                    unlockedPlants.add(new PlayerPlant(defaultName));
                }
            }
        }

        return unlockedPlants;
    }

    public void addPlant(PlayerPlant plant) {
        getUnlockedPlants().add(plant);
    }

    public PlayerPlant getOwnedPlant(String plantName) {
        for (PlayerPlant plant : getUnlockedPlants()) {
            if (plant.getPlantName().equalsIgnoreCase(plantName)) {
                return plant;
            }
        }

        return null;
    }

    public Set<String> getStoredBoosts() {
        if (storedBoosts == null) {
            storedBoosts = new HashSet<>();
        }

        return storedBoosts;
    }

    public void addStoredBoost(String plantName) {
        getStoredBoosts().add(normalizePlantName(plantName));
    }

    public boolean hasStoredBoost(String plantName) {
        return getStoredBoosts().contains(
                normalizePlantName(plantName)
        );
    }

    public void removeStoredBoost(String plantName) {
        getStoredBoosts().remove(
                normalizePlantName(plantName)
        );
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public void clearPlantFood() {
        this.plantFoodCount = 0;
    }

    public boolean addPlantFood(int amount) {
        long newAmount = (long) plantFoodCount + amount;

        if (amount < 0 || newAmount > MAX_PLANT_FOOD) {
            return false;
        }

        plantFoodCount = (int) newAmount;
        return true;
    }

    public DailyOffer getDailyOffer() {
        return dailyOffer;
    }

    public void setDailyOffer(DailyOffer dailyOffer) {
        this.dailyOffer = dailyOffer;
    }

    public boolean isChapterUnlocked(String chapterName) {
        return getUnlockedChapters().contains(chapterName);
    }

    public void unlockChapter(String chapterName) {
        if (!getUnlockedChapters().contains(chapterName)) {
            getUnlockedChapters().add(chapterName);
        }
    }

    public AdventureProgress getAdventureProgress() {
        if (adventureProgress == null) {
            adventureProgress = new AdventureProgress();
        }
        return adventureProgress;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }
    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getGameSpeed(){return gameSpeed;}
    public void setGameSpeed(int gameSpeed){this.gameSpeed=gameSpeed;}
    public boolean isShowGrid(){return showGrid;}
    public void setShowGrid(boolean showGrid){this.showGrid=showGrid;}
    public boolean isDebugMode(){return debugMode;}
    public void setDebugMode(boolean debugMode){this.debugMode=debugMode;}

    public void addNews(String title, String message) {
        getNewsList().add(new NewsItem(title, message));
    }

    public List<NewsItem> getUnreadNews() {
        return getNewsList().stream()
                .filter(news -> !news.isRead())
                .toList();
    }

    public List<NewsItem> getAllNews() {
        return getNewsList();
    }

    public void markAllAsRead() {
        for (NewsItem news : getNewsList()) {
            news.setRead(true);
        }
    }

    public boolean hasUnreadNews() {
        return getNewsList().stream()
                .anyMatch(news -> !news.isRead());
    }

    public List<String> getSeenZombies() {
        if (seenZombies == null) {
            seenZombies = new ArrayList<>();
        }

        return seenZombies;
    }

    public boolean addSeenZombie(String zombieId) {
        Objects.requireNonNull(zombieId, "zombie id cannot be null");
        String checkedId = zombieId.strip();
        if (checkedId.isEmpty()) {
            throw new IllegalArgumentException("zombie id cannot be blank");
        }

        boolean alreadySeen = getSeenZombies().stream().anyMatch(
                seen -> seen.equalsIgnoreCase(checkedId)
        );
        if (alreadySeen) {
            return false;
        }

        getSeenZombies().add(checkedId);
        return true;
    }

    private List<String> getUnlockedChapters() {
        if (unlockedChapters == null) {
            unlockedChapters = new ArrayList<>();
            unlockedChapters.add(DEFAULT_CHAPTER);
        }

        return unlockedChapters;
    }

    private List<NewsItem> getNewsList() {
        if (newsList == null) {
            newsList = new ArrayList<>();
        }

        return newsList;
    }

    private static List<PlayerPlant> createDefaultUnlockedPlants() {
        List<PlayerPlant> plants = new ArrayList<>();

        for (String plantName : DEFAULT_UNLOCKED_PLANT_NAMES) {
            plants.add(new PlayerPlant(plantName));
        }

        return plants;
    }

    private String normalizePlantName(String plantName) {
        return plantName.toLowerCase();
    }
}
