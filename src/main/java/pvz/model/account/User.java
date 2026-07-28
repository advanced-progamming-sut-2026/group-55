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



    public User(String username,
                String passwordHash,
                String nickname,
                String email,
                String gender) {

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

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {

        long result = (long) coins + amount;

        coins = result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    public boolean spendCoins(int amount) {

        if(amount < 0)
            return false;

        if(coins >= amount){

            coins -= amount;
            return true;
        }
        return false;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addDiamonds(int amount){

        long result = (long) diamonds + amount;

        diamonds = result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    public boolean spendDiamonds(int amount){

        if(amount < 0)
            return false;

        if(diamonds >= amount){

            diamonds -= amount;
            return true;
        }
        return false;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getClearedStages() {
        return clearedStages;
    }

    public int getMaxMewPoint() {
        return maxMewPoint;
    }

    public Greenhouse getGreenhouse(){

        if(greenhouse == null)
            greenhouse = new Greenhouse();

        return greenhouse;
    }

    public List<PlayerPlant> getUnlockedPlants(){

        if(unlockedPlants == null){

            unlockedPlants = new ArrayList<>();

            unlockedPlants.add(new PlayerPlant("peashooter"));
            unlockedPlants.add(new PlayerPlant("sunflower"));
        }

        return unlockedPlants;
    }

    public void addPlant(PlayerPlant plant){

        getUnlockedPlants().add(plant);
    }

    public PlayerPlant getOwnedPlant(String name){

        for(PlayerPlant plant : getUnlockedPlants()){

            if(plant.getPlantName().equalsIgnoreCase(name))
                return plant;
        }

        return null;
    }

    public Set<String> getStoredBoosts(){

        if(storedBoosts == null)
            storedBoosts = new HashSet<>();

        return storedBoosts;
    }

    public void addStoredBoost(String plantName){

        getStoredBoosts().add(plantName.toLowerCase());
    }
    public boolean hasStoredBoost(String plantName){

        return getStoredBoosts().contains(plantName.toLowerCase());
    }


    public void removeStoredBoost(String plantName){

        getStoredBoosts().remove(plantName.toLowerCase());
    }

    public int getPlantFoodCount(){

        return plantFoodCount;
    }

    public boolean addPlantFood(int amount){

        if(plantFoodCount + amount > 3)
            return false;

        plantFoodCount += amount;

        return true;
    }

    public DailyOffer getDailyOffer(){

        return dailyOffer;
    }

    public void setDailyOffer(DailyOffer dailyOffer){

        this.dailyOffer = dailyOffer;
    }

    public boolean isChapterUnlocked(String chapter){

        if(unlockedChapters == null){

            unlockedChapters = new ArrayList<>();
            unlockedChapters.add("ancient-egypt");
        }

        return unlockedChapters.contains(chapter);
    }

    public void setDifficultyLevel(int level){

        this.difficultyLevel = level;
    }

    public List<NewsItem> getUnreadNews(){

        if(newsList == null)
            return new ArrayList<>();

        return newsList.stream()
                .filter(news -> !news.isRead())
                .toList();
    }


    public List<NewsItem> getAllNews(){

        if(newsList == null)
            return new ArrayList<>();

        return newsList;
    }


    public void markAllAsRead(){

        if(newsList == null)
            return;

        for(NewsItem news : newsList)
            news.setRead(true);
    }
}
