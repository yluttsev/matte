package io.github.yluttsev.matte.core.model.info;

import io.github.yluttsev.matte.core.construction.CopyableModel;

public final class ApiInfo implements CopyableModel<ApiInfo> {

    private String title;
    private String summary;
    private String description;
    private String version;
    private String termsOfService;
    private Contact contact;
    private License license;

    public ApiInfo() {
    }

    private ApiInfo(ApiInfo source) {
        title = source.title;
        summary = source.summary;
        description = source.description;
        version = source.version;
        termsOfService = source.termsOfService;
        contact = source.contact;
        license = source.license;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ApiInfo copy() {
        return new ApiInfo(this);
    }

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

    public static final class Builder {

        private final ApiInfo draft = new ApiInfo();

        private Builder() {
        }

        public Builder title(String title) {
            draft.title = title;
            return this;
        }

        public Builder summary(String summary) {
            draft.summary = summary;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder version(String version) {
            draft.version = version;
            return this;
        }

        public Builder termsOfService(String termsOfService) {
            draft.termsOfService = termsOfService;
            return this;
        }

        public Builder contact(Contact contact) {
            draft.contact = contact;
            return this;
        }

        public Builder license(License license) {
            draft.license = license;
            return this;
        }

        public ApiInfo build() {
            return draft.copy();
        }
    }
}
