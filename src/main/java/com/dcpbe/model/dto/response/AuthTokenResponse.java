package com.dcpbe.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class AuthTokenResponse {
     String accessToken;
     String refreshToken;
     Long expiresIn;
     Long refreshExpiresIn;
     String tokenType;
}
