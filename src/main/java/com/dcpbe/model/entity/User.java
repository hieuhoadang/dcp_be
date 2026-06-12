package com.dcpbe.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_name", unique = true, nullable = false, length = 100)
    private String username;
    @Column(name = "full_name", nullable = false)
    private String fullname;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String position;
    @Column(name = "roles")
    private String roles;

    public User(String username, String fullname, String position, String roles) {
        this.username = username;
        this.fullname = fullname;
        this.position = position;
        this.roles = roles;
    }

}
