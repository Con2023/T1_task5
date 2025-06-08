package org.example.Common.DTO;

public class ClientStatusResponse {
    private Long clientId;
    private Long accountId;
    private String status;

    public ClientStatusResponse(Long clientId, Long accountId, String status) {
        this.clientId = clientId;
        this.accountId = accountId;
        this.status = status;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
