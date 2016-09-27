package com.fexco.test;

/**
 * Lists the catalogs from the external provider that are proxy-ed by this one.
 */
enum AddressCatalog {
    eirCode("ie"), premise("uk");

    private final String prefix;

    AddressCatalog(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
