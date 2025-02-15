package org.example.model;

public class User {
    private long id;
    private boolean isPolicyAccepted;
    private String link;

    public User(long id, boolean isPolicyAccepted, String link) {
        this.id = id;
        this.isPolicyAccepted = isPolicyAccepted;
        this.link = link;
    }

    // Геттеры и сеттеры
    public long getId() {
        return id;
    }

    public boolean isPolicyAccepted() {
        return isPolicyAccepted;
    }

    public String getLink() {
        return link;
    }
}
