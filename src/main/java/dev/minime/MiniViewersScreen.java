package dev.minime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Привязка зрителей: ник Twitch → ник Minecraft. Чибик появляется только после привязки. */
public class MiniViewersScreen extends Screen {
    private static final int ROWS = 6;
    private final Screen parent;
    private EditBox twitchBox, mcBox;
    private int page = 0;
    private String status = "";

    public MiniViewersScreen(Screen parent) {
        super(Component.literal("Зрители"));
        this.parent = parent;
    }

    private Config cfg() { return MiniMeClient.config; }

    private int y0() { return this.height / 2 - 105; }

    @Override
    protected void init() {
        int x0 = this.width / 2 - 150, y0 = y0();

        twitchBox = new EditBox(this.font, x0, y0 + 14, 100, 20, Component.literal("Ник на Twitch"));
        twitchBox.setMaxLength(25);
        addRenderableWidget(twitchBox);
        mcBox = new EditBox(this.font, x0 + 104, y0 + 14, 100, 20, Component.literal("Ник в Minecraft"));
        mcBox.setMaxLength(16);
        addRenderableWidget(mcBox);
        addRenderableWidget(Button.builder(Component.literal("Привязать"), b -> {
            String t = twitchBox.getValue().trim().replace("@", "").toLowerCase();
            String m = mcBox.getValue().trim();
            if (!t.matches("[a-z0-9_]{1,25}")) { status = "Неверный ник Twitch"; return; }
            if (!m.matches("[A-Za-z0-9_]{3,16}")) { status = "Неверный ник Minecraft"; return; }
            cfg().link(t, m);
            cfg().save();
            MiniManager.onLinked(t, m);
            status = t + " → " + m + ": чибик появится после его сообщения";
            this.rebuildWidgets();
        }).bounds(x0 + 208, y0 + 14, 92, 20).build());

        List<Config.Viewer> vs = cfg().viewers;
        for (int r = 0; r < ROWS; r++) {
            int idx = page * ROWS + r;
            if (idx >= vs.size()) break;
            final String tw = vs.get(idx).twitch;
            addRenderableWidget(Button.builder(Component.literal("убрать"), b -> {
                cfg().unlink(tw);
                cfg().save();
                MiniManager.onUnlinked(tw);
                this.rebuildWidgets();
            }).bounds(x0 + 236, y0 + 42 + r * 22, 64, 20).build());
        }

        int yp = y0 + 42 + ROWS * 22 + 2;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> { if (page > 0) { page--; this.rebuildWidgets(); } })
                .bounds(x0, yp, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if ((page + 1) * ROWS < cfg().viewers.size()) { page++; this.rebuildWidgets(); }
        }).bounds(x0 + 260, yp, 40, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Только привязанные: " + (cfg().requireLink ? "да" : "нет")), b -> {
            cfg().requireLink = !cfg().requireLink;
            b.setMessage(Component.literal("Только привязанные: " + (cfg().requireLink ? "да" : "нет")));
        }).bounds(x0, yp + 26, 148, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Привязка !mini ник: " + (cfg().chatLink ? "да" : "нет")), b -> {
            cfg().chatLink = !cfg().chatLink;
            b.setMessage(Component.literal("Привязка !mini ник: " + (cfg().chatLink ? "да" : "нет")));
        }).bounds(x0 + 152, yp + 26, 148, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x0, yp + 52, 300, 20).build());
    }

    @Override
    public void onClose() {
        cfg().save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Theme.Look look = Theme.menu(cfg());
        int txt = 0xFF000000 | look.text();
        int x0 = this.width / 2 - 150, y0 = y0();
        Theme.box(g, this.width / 2 - 165, y0 - 30, 330, 42 + ROWS * 22 + 2 + 52 + 20 + 34, look, 1f);
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(this.font, Component.literal("Зрители: Twitch → Minecraft"), this.width / 2, y0 - 20, txt);
        g.text(this.font, "Ник на Twitch", x0, y0 + 3, txt, false);
        g.text(this.font, "Ник в Minecraft", x0 + 104, y0 + 3, txt, false);
        List<Config.Viewer> vs = cfg().viewers;
        for (int r = 0; r < ROWS; r++) {
            int idx = page * ROWS + r;
            if (idx >= vs.size()) break;
            g.text(this.font, vs.get(idx).twitch + "  →  " + vs.get(idx).mc, x0, y0 + 48 + r * 22, txt, false);
        }
        if (vs.isEmpty()) g.text(this.font, "Пока никого. Зритель может написать в чат: !mini НикМайна", x0, y0 + 48, txt, false);
        int yp = y0 + 42 + ROWS * 22 + 2;
        g.centeredText(this.font, Component.literal((page + 1) + " / " + Math.max(1, (vs.size() + ROWS - 1) / ROWS)), this.width / 2, yp + 6, txt);
        if (!status.isEmpty()) g.text(this.font, status, x0, yp + 78, txt, false);
    }
}
