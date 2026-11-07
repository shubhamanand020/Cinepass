package com.cinepass.user.service;

import com.cinepass.user.entity.Role;
import com.cinepass.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceSecurityTest {

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;
    private BCryptPasswordEncoder passwordEncoder;
    private final String hardcodedSecret = "cinepass-review1-jwt-secret-key-2026";

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(hardcodedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        jwtService = new JwtService(jwtEncoder);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Admin token must contain ROLE_ADMIN in scope and role claims")
    void testAdminTokenClaims() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@cinepass.com");
        admin.setName("Cinema Administrator");
        admin.setRole(Role.ROLE_ADMIN);

        String token = jwtService.createToken(admin);
        assertNotNull(token);

        Jwt decoded = jwtDecoder.decode(token);
        assertEquals("1", decoded.getSubject());
        assertEquals("admin@cinepass.com", decoded.getClaimAsString("email"));
        assertEquals("ROLE_ADMIN", decoded.getClaimAsString("role"));
        assertEquals(List.of("ROLE_ADMIN"), decoded.getClaimAsStringList("roles"));
        assertEquals("ROLE_ADMIN", decoded.getClaimAsString("scope"));
    }

    @Test
    @DisplayName("User token must contain ROLE_USER and never grant admin scope")
    void testUserTokenClaims() {
        User customer = new User();
        customer.setId(2L);
        customer.setEmail("customer@cinepass.com");
        customer.setName("Regular Customer");
        customer.setRole(Role.ROLE_USER);

        String token = jwtService.createToken(customer);
        assertNotNull(token);

        Jwt decoded = jwtDecoder.decode(token);
        assertEquals("2", decoded.getSubject());
        assertEquals("customer@cinepass.com", decoded.getClaimAsString("email"));
        assertEquals("ROLE_USER", decoded.getClaimAsString("role"));
        assertNotEquals("ROLE_ADMIN", decoded.getClaimAsString("role"));
    }

    @Test
    @DisplayName("Tampered JWT signature must fail decoding")
    void testTamperedTokenFails() {
        User user = new User();
        user.setId(3L);
        user.setEmail("user@example.com");
        user.setRole(Role.ROLE_USER);

        String validToken = jwtService.createToken(user);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "ABCDE";

        assertThrows(Exception.class, () -> {
            jwtDecoder.decode(tamperedToken);
        }, "Tampered signature should throw JwtException upon decode");
    }

    @Test
    @DisplayName("BCrypt password encoder correctly verifies match and rejects bad password")
    void testBcryptVerification() {
        String validHash = "$2a$10$qt/wLO5bst2sCQ3W08dgfO1mFvrJ2qTKT/UUHM.91AZ/zUeXKNnF.";
        assertTrue(passwordEncoder.matches("Password@123", validHash));
        String rawPassword = "SecurePassword@2026";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("WrongPassword", encoded));
    }
}
