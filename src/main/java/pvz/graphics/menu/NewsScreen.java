package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.libpvz.textures.TextureBank;
import pvz.model.account.NewsItem;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

public class NewsScreen extends Group {

    private static final float PANEL_WIDTH = 430f;
    private static final float PANEL_HEIGHT = 400f;

    private final TextureBank textures;
    private final Skin skin;
    private final AppState appState;
    private final UserManager userManager;
    private final Table newsTable;
    private Texture overlayTexture;

    public NewsScreen(
            TextureBank textures,
            Skin skin,
            AppState appState,
            UserManager userManager
    ) {
        this.textures = textures;
        this.skin = skin;
        this.appState = appState;
        this.userManager = userManager;
        this.newsTable = new Table();

        setSize(1280f, 720f);
        buildUI();
        setVisible(false);
    }

    private void buildUI() {
        Image overlay = new Image(createOverlayDrawable());
        overlay.setColor(1f, 1f, 1f, 0.55f);
        overlay.setSize(getWidth(), getHeight());
        addActor(overlay);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.setPosition(
                (getWidth() - PANEL_WIDTH) / 2f,
                (getHeight() - PANEL_HEIGHT) / 2f
        );
        addActor(panel);

        Table header = new Table();

        Image back = new Image(
                textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL")
        );
        back.setSize(35f, 35f);

        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });

        header.add(back).size(45f).left().padLeft(5f);

        Label title = new Label("NEWS", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.15f);

        header.add(title).expandX().fillX().center();
        header.add().size(45f);

        panel.add(header)
                .width(PANEL_WIDTH)
                .height(50f)
                .top()
                .row();

        newsTable.defaults().pad(4f);

        ScrollPane scroll = new ScrollPane(newsTable, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        panel.add(scroll)
                .width(PANEL_WIDTH - 20f)
                .height(PANEL_HEIGHT - 60f)
                .expand()
                .fill()
                .padBottom(8f);
    }

    private Drawable createOverlayDrawable() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();

        overlayTexture = new Texture(pixmap);
        pixmap.dispose();

        return new TextureRegionDrawable(new TextureRegion(overlayTexture));
    }

    private void refreshNews() {
        newsTable.clear();

        User user = appState.getCurrentUser();

        if (user == null || user.getAllNews().isEmpty()) {
            Label empty = new Label("No news available.", skin);
            empty.setColor(Color.BLACK);
            empty.setFontScale(1.05f);
            newsTable.add(empty).center();
            return;
        }

        for (NewsItem news : user.getAllNews()) {
            Table item = new Table();

            Label title = new Label(news.getTitle(), skin);
            title.setColor(Color.BLACK);
            title.setFontScale(1.05f);

            Label date = new Label(news.getDate(), skin);
            date.setColor(Color.DARK_GRAY);
            date.setFontScale(0.8f);

            Label status = new Label(news.isRead() ? "" : "NEW", skin);
            status.setColor(Color.RED);
            status.setFontScale(0.8f);

            Label message = new Label(news.getMessage(), skin);
            message.setColor(Color.BLACK);
            message.setWrap(true);
            message.setAlignment(Align.left);

            item.add(title).left().expandX().fillX();
            item.add(date).right().padRight(8f);
            item.add(status).right().row();

            item.add(message)
                    .left()
                    .expandX()
                    .fillX()
                    .colspan(3)
                    .padTop(2f)
                    .row();

            newsTable.add(item)
                    .width(PANEL_WIDTH - 40f)
                    .left()
                    .fillX()
                    .row();
        }
    }

    public void show() {
        refreshNews();
        appState.setCurrentMenu(MenuName.NEWS);
        setVisible(true);
        toFront();

        User user = appState.getCurrentUser();
        if (user != null && user.hasUnreadNews()) {
            String username = user.getUsername();
            user.markAllAsRead();

            if (!userManager.save()) {
                userManager.reload();
                User reloadedUser = userManager.find(
                        candidate -> candidate.getUsername().equals(username)
                );
                appState.setCurrentUser(reloadedUser);
                refreshNews();
            }
        }
    }

    public void hide() {
        setVisible(false);
        appState.setCurrentMenu(MenuName.MAIN);
    }

    public void dispose() {
        if (overlayTexture != null) {
            overlayTexture.dispose();
            overlayTexture = null;
        }
    }
}
