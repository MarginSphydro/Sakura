package dev.sakura.gui.account.list;

import dev.sakura.account.type.MinecraftAccount;
import dev.sakura.gui.theme.SakuraTheme;
import dev.sakura.manager.Managers;
import dev.sakura.nanovg.NanoVGRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.nanovg.NanoVG;

import java.util.List;
import java.util.Objects;

public final class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountEntry> {

    private String searchFilter;

    public AccountListWidget(MinecraftClient minecraftClient, int width, int height, int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight);
    }

    public void populateEntries() {
        clearEntries();
        final List<MinecraftAccount> accounts = Managers.ACCOUNT.getAccounts();
        if (!accounts.isEmpty()) {
            for (final MinecraftAccount account : accounts) {
                addEntry(new AccountEntry(account));
            }
            setSelected(getEntry(0));
        }
    }

    @Override
    public int getRowWidth() {
        return width - 10;
    }

    private int getContentsHeight() {
        return this.children().size() * this.itemHeight + 4;
    }

    @Override
    public int getMaxScrollY() {
        return Math.max(0, this.getContentsHeight() - (this.getBottom() - this.getY() - 4));
    }

    protected int getScrollbarX() {
        return this.getX() + this.width - 6;
    }

    protected void renderList(DrawContext context, int mouseX, int mouseY, float delta) {
        List<AccountEntry> entries = children();
        if (searchFilter != null && !searchFilter.isEmpty()) {
            entries = entries.stream()
                    .filter((entry) -> entry.getAccount().username()
                            .toLowerCase()
                            .contains(searchFilter.toLowerCase()))
                    .toList();
        }

        int x = getRowLeft();
        int width = getRowWidth();
        int height = itemHeight - 4;
        int size = entries.size();

        for (int i = 0; i < size; ++i) {
            int y = getRowTop(i);
            int m = getRowBottom(i);
            if (m >= getY() && y <= getBottom()) {
                AccountEntry entry = entries.get(i);
                final boolean isHovered = Objects.equals(getHoveredEntry(), entry);
                final boolean isSelected = Objects.equals(getSelectedOrNull(), entry);

                // Draw selection/hover highlight using NanoVG
                if (isSelected || isHovered) {
                    NanoVGRenderer.INSTANCE.draw(vg -> {
                        NanoVG.nvgBeginPath(vg);
                        NanoVG.nvgRoundedRect(vg, x, y, width, height, SakuraTheme.ROUNDING);
                        NanoVG.nvgFillColor(vg, SakuraTheme.color(SakuraTheme.SELECTION));
                        NanoVG.nvgFill(vg);
                    });
                }

                boolean active = client.getSession() != null && client.getSession().getUsername().equalsIgnoreCase(entry.getAccount().username());
                entry.render(context, i, y, x, width, height, mouseX, mouseY, active, delta);
            }
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Custom render to avoid default scrollbar and styling

        // Clip content to list area
        context.enableScissor(getX(), getY(), getRight(), getBottom());

        this.renderList(context, mouseX, mouseY, delta);

        context.disableScissor();

        // Custom Scrollbar
        int maxScroll = getMaxScrollY();
        if (maxScroll > 0) {
            int scrollbarX = getScrollbarX();
            int scrollbarY = getY();
            int scrollbarHeight = getHeight();
            int contentHeight = getContentsHeight();

            int barHeight = (int) ((float) (scrollbarHeight * scrollbarHeight) / (float) contentHeight);
            barHeight = MathHelper.clamp(barHeight, 32, scrollbarHeight - 8);

            int barTop = (int) this.getScrollY() * (scrollbarHeight - barHeight) / maxScroll + scrollbarY;
            if (barTop < scrollbarY) {
                barTop = scrollbarY;
            }

            int finalBarHeight = barHeight;
            int finalBarTop = barTop;

            NanoVGRenderer.INSTANCE.draw(vg -> {
                // Track
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgRoundedRect(vg, scrollbarX, scrollbarY, 6, scrollbarHeight, 3);
                NanoVG.nvgFillColor(vg, SakuraTheme.color(0, 0, 0, 30));
                NanoVG.nvgFill(vg);

                // Thumb
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgRoundedRect(vg, scrollbarX, finalBarTop, 6, finalBarHeight, 3);
                NanoVG.nvgFillColor(vg, SakuraTheme.color(SakuraTheme.PRIMARY, 0.8f));
                NanoVG.nvgFill(vg);
            });
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final AccountEntry entry = getEntryAtPosition(mouseX, mouseY);
        if (entry != null) {
            setSelected(entry);
        }
        if (getSelectedOrNull() != null) {
            return getSelectedOrNull().mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }
}
