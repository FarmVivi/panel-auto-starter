package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;

public class ProxyPingEvent extends Event {
    /**
     * The host asking for a ping response.
     */
    private final String host;

    /**
     * The data to respond with.
     */
    private CommonServerPing response;

    public ProxyPingEvent(String host, CommonServerPing response) {
        this.host = host;
        this.response = response;
    }

    public String getHost() {
        return host;
    }

    public CommonServerPing getResponse() {
        return response;
    }

    public void setResponse(CommonServerPing response) {
        this.response = response;
    }
}
