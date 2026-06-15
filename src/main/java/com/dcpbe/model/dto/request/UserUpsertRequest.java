package com.dcpbe.model.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UserUpsertRequest {
    private String username;
    private String fullName;
    private String email;
    private String position;
    private List<String> roles;
}
