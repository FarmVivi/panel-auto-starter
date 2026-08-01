package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Rend un menu sous forme de lignes de chat cliquables.
 * <p>
 * Toujours disponible : il ne repose que sur le protocole de chat, que les deux
 * proxys savent produire depuis toujours. C'est le rendu de repli, et le seul
 * qui ne puisse pas être mis en défaut par une version de jeu trop récente.
 * <p>
 * Le détail d'une entrée est mis au survol plutôt qu'affiché sous elle : une
 * liste de serveurs deviendrait vite un mur de texte qui chasse le reste du
 * chat.
 */
public final class ChatMenuRenderer implements MenuRenderer {

    /** Nom sous lequel ce rendu se désigne dans la configuration. */
    public static final String NAME = "chat";

    private static final Component RULE =
            Component.text("─".repeat(28), NamedTextColor.DARK_GRAY);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable(CommonPlayer viewer) {
        return true;
    }

    @Override
    public void open(CommonPlayer viewer, Menu menu) {
        Component page = RULE
                .appendNewline()
                .append(Component.text(" ").append(menu.title()));

        for (MenuItem item : menu.items()) {
            page = page.appendNewline().append(render(item));
        }

        viewer.sendMessage(page.appendNewline().append(RULE));
    }

    private Component render(MenuItem item) {
        Component line = Component.text(" ").append(item.label());

        if (!item.lore().isEmpty()) {
            Component tooltip = null;
            for (Component detail : item.lore()) {
                tooltip = tooltip == null ? detail : tooltip.appendNewline().append(detail);
            }
            line = line.hoverEvent(HoverEvent.showText(tooltip));
        }

        if (item.isActionable()) {
            line = line.append(Component.text("  ▸", NamedTextColor.DARK_GRAY))
                    .clickEvent(ClickEvent.runCommand("/" + item.command()));
        } else {
            // Une entree inerte doit se voir comme telle, sinon le joueur clique
            // dans le vide en se demandant ce qui ne marche pas.
            line = line.decorate(TextDecoration.ITALIC);
        }

        return line;
    }
}
