package com.peatroxd.mtprototest.proxy.ranking;

public record ClientSegment(String country, String os, String deviceType) {

    public static final ClientSegment UNKNOWN = new ClientSegment("UNKNOWN", "UNKNOWN", "UNKNOWN");

    public boolean matchesCountryAndOs(String country, String os) {
        return this.country.equalsIgnoreCase(country) && this.os.equalsIgnoreCase(os);
    }
}
