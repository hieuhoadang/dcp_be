package com.dcpbe.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;
    @Column(name = "fullName", nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String position;
    @Column(name = "roles")
    private String roles;

    public User(String username, String fullname, String position, String roles) {
        this.username = username;
        this.fullName = fullname;
        this.position = position;
        this.roles = roles;
    }

}
