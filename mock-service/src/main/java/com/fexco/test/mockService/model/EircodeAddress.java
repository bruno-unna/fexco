package com.fexco.test.mockService.model;

/**
 * Represents an address (mocked one). Randomly generated.
 */
public class EircodeAddress {
    private String addressline1;
    private String addressline2;
    private String summaryline;
    private String organisation;
    private String street;
    private String posttown;
    private String county;
    private String postcode;

    public String getAddressline1() {
        return addressline1;
    }

    public EircodeAddress setAddressline1(String addressline1) {
        this.addressline1 = addressline1;
        return this;
    }

    public String getAddressline2() {
        return addressline2;
    }

    public EircodeAddress setAddressline2(String addressline2) {
        this.addressline2 = addressline2;
        return this;
    }

    public String getSummaryline() {
        return summaryline;
    }

    public EircodeAddress setSummaryline(String summaryline) {
        this.summaryline = summaryline;
        return this;
    }

    public String getOrganisation() {
        return organisation;
    }

    public EircodeAddress setOrganisation(String organisation) {
        this.organisation = organisation;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public EircodeAddress setStreet(String street) {
        this.street = street;
        return this;
    }

    public String getPosttown() {
        return posttown;
    }

    public EircodeAddress setPosttown(String posttown) {
        this.posttown = posttown;
        return this;
    }

    public String getCounty() {
        return county;
    }

    public EircodeAddress setCounty(String county) {
        this.county = county;
        return this;
    }

    public String getPostcode() {
        return postcode;
    }

    public EircodeAddress setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }
}
