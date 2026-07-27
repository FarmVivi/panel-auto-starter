package fr.farmvivi.panelautostarter.velocity.ping;

import com.velocitypowered.api.util.Favicon;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;

public class VelocityFavicon implements CommonFavicon {
    private final Favicon favicon;

    public VelocityFavicon(Favicon favicon) {
        this.favicon = favicon;
    }

    public Favicon getFavicon() {
        return favicon;
    }

    @Override
    public String getEncoded() {
        return favicon.getBase64Url();
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
