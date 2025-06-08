package org.example.Common.entities;


import jakarta.persistence.*;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String password;

    @Enumerated(EnumType.STRING)
    private StatusUser statusUser;

    @OneToOne
    @JoinColumn(name = "client_client_id", referencedColumnName = "clientId")
    private Client client;

    public enum StatusUser {
        ACTIVE, BLOCKED, NULL
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public StatusUser getStatusUser() {
        return statusUser;
    }

    public void setStatusUser(StatusUser statusUser) {
        this.statusUser = statusUser;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
