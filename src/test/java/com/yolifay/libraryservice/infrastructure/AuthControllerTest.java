package com.yolifay.libraryservice.infrastructure;

import com.yolifay.libraryservice.application.dto.auth.*;
import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.*;
import com.yolifay.libraryservice.domain.usecase.auth.handler.*;
import com.yolifay.libraryservice.infrastructure.web.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NOTE: Sesuaikan semua import paket di atas agar cocok dengan strukturmu.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    // --- Mocks untuk semua dependency ---
    private final RegisterUserHandler registerHandler = mock(RegisterUserHandler.class);
    private final LoginUserHandler loginHandler = mock(LoginUserHandler.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final TokenStore accessWhitelist = mock(TokenStore.class);
    private final RefreshAccessTokenHandler refreshHandler = mock(RefreshAccessTokenHandler.class);
    private final RefreshTokenStore refreshStore = mock(RefreshTokenStore.class);
    private final RequestOtpHandler requestOtpHandler = mock(RequestOtpHandler.class);
    private final LoginWithOtpHandler loginWithOtpHandler = mock(LoginWithOtpHandler.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);

    private AuthController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AuthController(
                registerHandler, loginHandler, tokenIssuer, accessWhitelist,
                refreshHandler, refreshStore, requestOtpHandler, loginWithOtpHandler, users);

        // set nilai @Value melalui reflection
        setField(controller, "otpTtl", Duration.ofMinutes(7));
        setField(controller, "refreshDays", 14L);
    }

    // Helper untuk menyetel field private
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /* ======================= REGISTER ======================= */

    @Test
    void register_success_withExplicitRole() {
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.fullName()).thenReturn("A");
        when(req.username()).thenReturn("alice");
        when(req.email()).thenReturn("a@x.test");
        when(req.password()).thenReturn("p");
        when(req.role()).thenReturn("editor"); // lower-case → should parse to Role.EDITOR

        when(registerHandler.executeRegisterUser(any(RegisterUser.class))).thenReturn(123L);

        Long id = controller.register(req);

        assertEquals(123L, id);
        verify(registerHandler).executeRegisterUser(argThat(r ->
                r.username().equals("alice") &&
                        r.email().equals("a@x.test") &&
                        r.role() == Role.EDITOR
        ));
    }

    @Test
    void register_success_withBlankRole_defaultsToViewer() {
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.fullName()).thenReturn("B");
        when(req.username()).thenReturn("bob");
        when(req.email()).thenReturn("b@x.test");
        when(req.password()).thenReturn("p");
        when(req.role()).thenReturn("  "); // blank → default Role.VIEWER

        when(registerHandler.executeRegisterUser(any(RegisterUser.class))).thenReturn(11L);

        Long id = controller.register(req);

        assertEquals(11L, id);
        verify(registerHandler).executeRegisterUser(argThat(r -> r.role() == Role.VIEWER));
    }

    @Test
    void register_invalidRole_throws() {
        RegisterRequest req = mock(RegisterRequest.class);
        lenient().when(req.fullName()).thenReturn("C");
        lenient().when(req.username()).thenReturn("c");
        lenient().when(req.email()).thenReturn("c@x.test");
        lenient().when(req.password()).thenReturn("p");
        lenient().when(req.role()).thenReturn("not_a_role");

        assertThrows(IllegalArgumentException.class, () -> controller.register(req));
        verifyNoInteractions(registerHandler);
    }

    /* ======================= LOGIN ======================= */

    @Test
    void login_success() {
        LoginRequest lr = mock(LoginRequest.class);
        when(lr.usernameOrEmail()).thenReturn("alice");
        when(lr.password()).thenReturn("secret");

        TokenPairResponse tpr = mock(TokenPairResponse.class);
        when(loginHandler.execute(any(LoginUser.class))).thenReturn(tpr);

        TokenPairResponse out = controller.login(lr);

        assertSame(tpr, out);
        verify(loginHandler).execute(any(LoginUser.class));
    }

    @Test
    void login_handlerThrows_isPropagated() {
        LoginRequest lr = mock(LoginRequest.class);
        when(lr.usernameOrEmail()).thenReturn("x");
        when(lr.password()).thenReturn("y");

        when(loginHandler.execute(any(LoginUser.class)))
                .thenThrow(new RuntimeException("login failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.login(lr));
        assertEquals("login failed", ex.getMessage());
    }

    /* ======================= REFRESH ======================= */

    @Test
    void refresh_success_buildsResponseWithComputedExpiry() throws Exception {
        RefreshRequest req = mock(RefreshRequest.class);
        when(req.refreshToken()).thenReturn("rft-1");

        // Mock pair + nested accessToken
        var token = new TokenIssuer.Token("acc-123", "jti-xyz",
                Instant.now(), Instant.now().plusSeconds(3600));

        // Asumsikan ada tipe 'TokenPair' (DTO) dgn method accessToken() & refreshToken().
        // Sesuaikan fully qualified name jika berbeda di proyekmu.
        RefreshAccessTokenHandler.TokenPair pair = mock(RefreshAccessTokenHandler.TokenPair.class);
        when(pair.accessToken()).thenReturn(token);
        when(pair.refreshToken()).thenReturn("rft-1");

        when(refreshHandler.execute(any(RefreshAccessToken.class))).thenReturn(pair);

        Instant before = Instant.now();
        TokenPairResponse resp = controller.refresh(req);
        Instant after = Instant.now();

        assertEquals("acc-123", resp.accessToken());
        assertEquals(token.issuedAt(), resp.accessIssuedAt());
        assertEquals(token.expiresAt(), resp.accessExpiresAt());
        assertEquals("rft-1", resp.refreshToken());
        assertTrue(!resp.refreshIssuedAt().isBefore(before.plus(Duration.ofDays(14))) &&
                !resp.refreshIssuedAt().isAfter(after.plus(Duration.ofDays(14)).plusSeconds(5)));

        verify(refreshHandler).execute(any(RefreshAccessToken.class));
    }

    @Test
    void refresh_handlerThrows_isPropagated() {
        RefreshRequest req = mock(RefreshRequest.class);
        when(req.refreshToken()).thenReturn("oops");
        when(refreshHandler.execute(any(RefreshAccessToken.class)))
                .thenThrow(new IllegalStateException("bad refresh"));

        assertThrows(IllegalStateException.class, () -> controller.refresh(req));
    }

    /* ======================= LOGOUT ======================= */

    @Test
    void logout_missingOrNonBearerHeader_returns401() {
        // header null
        ResponseEntity<LogoutResponse> r1 = controller.logout(null, null);
        assertEquals(HttpStatus.UNAUTHORIZED, r1.getStatusCode());

        // header tidak diawali "Bearer "
        ResponseEntity<LogoutResponse> r2 =
                controller.logout("Token xxx", new RefreshRequest("rf"));
        assertEquals(HttpStatus.UNAUTHORIZED, r2.getStatusCode());

        verifyNoInteractions(tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void logout_success_onlyAccessRevoked_whenNoRefreshProvided() {
        TokenIssuer.DecodedToken dec =
                new TokenIssuer.DecodedToken(1L, "u", "e", "USER", "jti-1");
        when(tokenIssuer.verify("AT")).thenReturn(dec);

        ResponseEntity<LogoutResponse> resp =
                controller.logout("Bearer AT", null); // req null → refreshRevoked=false

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().accessRevoked());
        assertFalse(resp.getBody().refreshRevoked());

        verify(accessWhitelist).revoke("jti-1");
        verifyNoInteractions(refreshStore);
    }

    @Test
    void logout_success_bothAccessAndRefreshRevoked() {
        TokenIssuer.DecodedToken dec =
                new TokenIssuer.DecodedToken(2L, "u", "e", "USER", "jti-2");
        when(tokenIssuer.verify("AT2")).thenReturn(dec);

        RefreshRequest req = mock(RefreshRequest.class);
        when(req.refreshToken()).thenReturn("RF-OK");

        ResponseEntity<LogoutResponse> resp =
                controller.logout("Bearer AT2", req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().accessRevoked());
        assertTrue(resp.getBody().refreshRevoked());

        verify(accessWhitelist).revoke("jti-2");
        verify(refreshStore).revoke("RF-OK");
    }

    /* ======================= REQUEST OTP ======================= */

    @Test
    void requestOtp_success_userFound_longLocalPart_masksFirst2Chars() {
        RequestOtpRequest r = mock(RequestOtpRequest.class);
        when(r.usernameOrEmail()).thenReturn("alice");

        User u = mock(User.class);
        when(u.getEmail()).thenReturn("abcdef@example.test"); // >2 char → ambil 2 huruf
        when(users.findByUsernameOrEmail("alice")).thenReturn(Optional.of(u));

        ResponseEntity<OtpResponse> resp = controller.requestOtp(r);

        assertEquals(202, resp.getStatusCodeValue()); // 202 Accepted
        assertEquals("email", "email");
        assertEquals("ab*******@example.test", resp.getBody().to());
        assertEquals(7 * 60, resp.getBody().ttlSeconds()); // otpTtl 7 menit
        verify(requestOtpHandler).execute(any(OtpRequest.class));
    }

    @Test
    void requestOtp_success_userFound_shortLocalPart_masks1or2Chars() {
        RequestOtpRequest r = mock(RequestOtpRequest.class);
        when(r.usernameOrEmail()).thenReturn("bob");

        User u = mock(User.class);
        when(u.getEmail()).thenReturn("a@example.test"); // ≤2 char → ambil 1 (max(1,len))
        when(users.findByUsernameOrEmail("bob")).thenReturn(Optional.of(u));

        ResponseEntity<OtpResponse> resp = controller.requestOtp(r);

        assertEquals(202, resp.getStatusCodeValue());
        assertEquals("a*******@example.test", resp.getBody().to());
    }

    @Test
    void requestOtp_success_userNotFound_orEmailInvalid_returnsDash() {
        RequestOtpRequest r = mock(RequestOtpRequest.class);
        when(r.usernameOrEmail()).thenReturn("ghost");

        // Optional.empty() → orElse("-") → maskEmail("-") ⇒ "-"
        when(users.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

        ResponseEntity<OtpResponse> resp = controller.requestOtp(r);

        assertEquals(202, resp.getStatusCodeValue());
        assertEquals("-", resp.getBody().to());
        verify(requestOtpHandler).execute(any(OtpRequest.class));
    }

    /* ======================= LOGIN OTP ======================= */

    @Test
    void loginOtp_success_withXffAndUaHeaders() {
        LoginWithOtpRequest r = mock(LoginWithOtpRequest.class);
        when(r.usernameOrEmail()).thenReturn("alice");
        when(r.password()).thenReturn("p");
        when(r.otp()).thenReturn("123456");

        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(http.getHeader("User-Agent")).thenReturn("UA");

        TokenPairResponse tpr = mock(TokenPairResponse.class);
        when(loginWithOtpHandler.execute(any(LoginWithOtp.class), eq("1.2.3.4"), eq("UA")))
                .thenReturn(tpr);

        TokenPairResponse out = controller.loginOtp(r, http);

        assertSame(tpr, out);
    }

    @Test
    void loginOtp_success_withoutXff_usesRemoteAddr_andUaDashWhenMissing() {
        LoginWithOtpRequest r = mock(LoginWithOtpRequest.class);
        when(r.usernameOrEmail()).thenReturn("bob");
        when(r.password()).thenReturn("p");
        when(r.otp()).thenReturn("654321");

        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getHeader("X-Forwarded-For")).thenReturn(null);
        when(http.getHeader("User-Agent")).thenReturn(null);
        when(http.getRemoteAddr()).thenReturn("9.9.9.9");

        TokenPairResponse tpr = mock(TokenPairResponse.class);
        when(loginWithOtpHandler.execute(any(LoginWithOtp.class), eq("9.9.9.9"), eq("-")))
                .thenReturn(tpr);

        TokenPairResponse out = controller.loginOtp(r, http);

        assertSame(tpr, out);
    }
}

