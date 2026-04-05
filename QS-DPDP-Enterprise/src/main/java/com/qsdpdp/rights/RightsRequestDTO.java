package com.qsdpdp.rights;

/**
 * Rights request DTO
 */
public class RightsRequestDTO {

    private String dataPrincipalId;
    private RightType requestType;
    private String description;
    private RequestPriority priority;
    private String actorId;

    public RightsRequestDTO() {
        this.priority = RequestPriority.NORMAL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RightsRequestDTO dto = new RightsRequestDTO();

        public Builder dataPrincipalId(String id) {
            dto.dataPrincipalId = id;
            return this;
        }

        public Builder requestType(RightType type) {
            dto.requestType = type;
            return this;
        }

        public Builder description(String desc) {
            dto.description = desc;
            return this;
        }

        public Builder priority(RequestPriority p) {
            dto.priority = p;
            return this;
        }

        public Builder actorId(String id) {
            dto.actorId = id;
            return this;
        }

        public RightsRequestDTO build() {
            return dto;
        }
    }

    public String getDataPrincipalId() {
        return dataPrincipalId;
    }

    public void setDataPrincipalId(String dataPrincipalId) {
        this.dataPrincipalId = dataPrincipalId;
    }

    public RightType getRequestType() {
        return requestType;
    }

    public void setRequestType(RightType requestType) {
        this.requestType = requestType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
}
