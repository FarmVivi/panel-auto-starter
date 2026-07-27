package fr.farmvivi.panelautostarter.bungee.ping;

import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import net.md_5.bungee.api.Favicon;

public class BungeeFavicon implements CommonFavicon {
    private final Favicon favicon;

    public BungeeFavicon(Favicon favicon) {
        this.favicon = favicon;
    }

    public Favicon getFavicon() {
        return favicon;
    }

    @Override
    public boolean equals(Object o) {
        return favicon.equals(o);
    }

    @Override
    public int hashCode() {
        return favicon.hashCode();
    }

    @Override
    public String toString() {
        return favicon.toString();
    }
}
