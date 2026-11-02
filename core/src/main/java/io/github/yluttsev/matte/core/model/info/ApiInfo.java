package io.github.yluttsev.matte.core.model.info;

public final class ApiInfo {

    private String title;
    private String summary;
    private String description;
    private String version;
    private String termsOfService;
    private Contact contact;
    private License license;

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public Contact getContact() {
        return contact;
    }

    public License getLicense() {
        return license;
    }
}
