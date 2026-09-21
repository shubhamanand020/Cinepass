package com.cinepass.user.service;

import com.cinepass.user.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    public JwtService(JwtEncoder encoder) { this.encoder = encoder; }
    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("cinepass-user-service").subject(user.getId().toString()).claim("email", user.getEmail()).issuedAt(now).expiresAt(now.plusSeconds(3600)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
