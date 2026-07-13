
## 1- interface Updatable

هر چیزی که با گذر زمان کار میکنه (گیاه، زامبی، تیر، تولید خورشید و...) باید اینو
پیاده کنه:

    public interface Updatable {
        void update(long tick);
    }

موتور بازی توی هر تیک تمام Updatable ها رو update ‌می کن.

## 2- Entity

public abstract class Entity implements Updatable {
    protected int row;
    protected double health;
}
- و row همیشه int باقی می مون اگر قرار بر این بود که زامبی سطرش رو تغییر بده
   از targetRow و یک شمارنده laneChangeTicksLeft استفاده میکنیم.
  تا وقتی جابجایی تموم نشده، زامبی مال همون سطر قبلیه.

## 3- Appstate

یه کلاس AppState داریم که توش currentMenu (یه enum) و currentUser هست.
حلقه اصلی تو Main یه switch روی منوی فعلیه و هر منو کنترلر خودش رو داره.
جابجایی بین منوها فقط یعنی عوض کردن currentMenu، هیچ کنترلری مستقیم
کنترلر دیگه رو صدا نمیزنه.

## 4- SaveManager
فقط کلاس SaveManager با فایل JSON کار می کن و وظیفه سیو و لود اطلاعات رو داره.
