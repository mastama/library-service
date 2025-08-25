package com.yolifay.libraryservice.domain.usecase.auth;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.OtpSender;
import com.yolifay.libraryservice.domain.service.OtpStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.OtpRequest;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RequestOtpHandler;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestOtpHandlerTest {

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final OtpSender sender = mock(OtpSender.class);
    private final OtpStore store = mock(OtpStore.class);
    private final RateLimitGuard rl = mock(RateLimitGuard.class);
    private final HttpServletRequest httpReq = mock(HttpServletRequest.class);

    private RequestOtpHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RequestOtpHandler(users, sender, store, rl, httpReq);
        // set @Value fields deterministik
        setField("length", 6);
        setField("ttl", Duration.ofSeconds(120));
        setField("cooldown", Duration.ofSeconds(30));
    }

    private void setField(String name, Object val) throws Exception {
        Field f = RequestOtpHandler.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(handler, val);
    }

    private static User user(long id, String email, String username) {
        return User.builder()
                .id(id)
                .fullName("X")
                .username(username)
                .email(email)
                .passwordHash("H")
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .role(Role.VIEWER)
                .build();
    }

    // ============== POSITIVE: generate + simpan + kirim ==============
    @Test
    void execute_success_generateStoresAndSendsOtp_withLowercasedLookup_andLengthRespected() {
        String input = "Alice@Example.TEST"; // akan di-lowercase saat lookup
        OtpRequest req = new OtpRequest(input);

        User u = user(7L, "alice@example.test", "alice");
        when(users.findByUsernameOrEmail("alice@example.test")).thenReturn(Optional.of(u));

        // belum ada OTP aktif
        when(store.get("login:7")).thenReturn(null);

        // act
        handler.execute(req);

        // verify rate-limit dipanggil dengan identity asli (tidak di-lowercase di RL)
        verify(rl).check(eq("request-otp"), same(httpReq), isNull(), eq(input));

        // lookup user lower-case
        verify(users).findByUsernameOrEmail("alice@example.test");

        // put ke store dengan code panjang = length
        ArgumentCaptor<String> codeCap = ArgumentCaptor.forClass(String.class);
        verify(store).put(eq("login:7"), codeCap.capture(), eq(Duration.ofSeconds(120)));
        String code = codeCap.getValue();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit), "OTP harus angka semua");

        // mengirim email ke alamat user
        verify(sender).sendOtp("alice@example.test", code);

//        verifyNoMoreInteractions(sender, store, users, rl);
    }

    // ============== NEGATIVE: rate-limit throw ==============
    @Test
    void execute_rateLimited_throws_andStopsFlow() {
        OtpRequest req = new OtpRequest("some@mail");
        doThrow(new RuntimeException("throttled"))
                .when(rl).check(eq("request-otp"), same(httpReq), isNull(), eq("some@mail"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.execute(req));
        assertEquals("throttled", ex.getMessage());

        verifyNoInteractions(users, store, sender);
    }

    // ============== NEGATIVE: user not found ==============
    @Test
    void execute_userNotFound_throwsIllegalArgument() {
        OtpRequest req = new OtpRequest("NoUser@Mail");
        when(users.findByUsernameOrEmail("nouser@mail")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(req));
        assertEquals("User not found", ex.getMessage());

        verify(users).findByUsernameOrEmail("nouser@mail");
        verifyNoInteractions(store, sender);
    }

    // ============== COOL-DOWN: OTP masih aktif -> return tanpa generate/kirim ==============
    @Test
    void execute_cooldownActive_returnsEarly_withoutGeneratingOrSending() {
        OtpRequest req = new OtpRequest("u@mail");
        User u = user(11L, "u@mail", "u");
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.of(u));

        // masih ada OTP aktif
        when(store.get("login:11")).thenReturn("123456");

        handler.execute(req);

        verify(store).get("login:11");
        verify(store, never()).put(anyString(), anyString(), any());
        verify(sender, never()).sendOtp(anyString(), anyString());
    }
}
