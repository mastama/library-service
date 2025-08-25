package com.yolifay.libraryservice.infrastructure;

import com.yolifay.libraryservice.application.dto.user.CreateUserRequest;
import com.yolifay.libraryservice.application.dto.user.UpdateUserRequest;
import com.yolifay.libraryservice.application.dto.user.UpdateUserRoleRequest;
import com.yolifay.libraryservice.application.dto.user.UserResponse;
import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RegisterUserHandler;
import com.yolifay.libraryservice.domain.usecase.user.command.*;
import com.yolifay.libraryservice.domain.usecase.user.handler.*;
import com.yolifay.libraryservice.infrastructure.web.UsersController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    private final RegisterUserHandler register = mock(RegisterUserHandler.class);
    private final UpdateUserHandler update = mock(UpdateUserHandler.class);
    private final SetUserRoleHandler setRole = mock(SetUserRoleHandler.class);
    private final GetUserHandler get = mock(GetUserHandler.class);
    private final ListUsersHandler list = mock(ListUsersHandler.class);
    private final DeleteUserHandler delete = mock(DeleteUserHandler.class);

    private UsersController controller;

    @BeforeEach
    void setUp() {
        controller = new UsersController(register, update, setRole, get, list, delete);
    }

    // ===== Helpers untuk kompatibel record/POJO =====
    private static Object call(Object obj, String... methods) {
        for (String m : methods) {
            try {
                Method md = obj.getClass().getMethod(m);
                return md.invoke(obj);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                fail("Accessor invocation failed: " + e.getMessage());
            }
        }
        fail("No accessor found on " + obj.getClass());
        return null;
    }
    private static Long respId(Object resp) { return (Long) call(resp, "id", "getId"); }
    private static String respFullName(Object resp) { return (String) call(resp, "fullName", "getFullName"); }
    private static String respUsername(Object resp) { return (String) call(resp, "username", "getUsername"); }
    private static String respEmail(Object resp) { return (String) call(resp, "email", "getEmail"); }
    private static Role respRole(Object resp) { return (Role) call(resp, "role", "getRole"); }
    private static Instant respCreatedAt(Object resp) { return (Instant) call(resp, "createdAt", "getCreatedAt"); }

    private User mockedUser(long id, String fullName, String username, String email, Role role, Instant createdAt) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getFullName()).thenReturn(fullName);
        when(u.getUsername()).thenReturn(username);
        when(u.getEmail()).thenReturn(email);
        when(u.getRole()).thenReturn(role);
        when(u.getCreatedAt()).thenReturn(createdAt);
        return u;
    }

    // ========== CREATE ==========

    @Test
    void create_success_withExplicitRole() {
        CreateUserRequest r = mock(CreateUserRequest.class);
        when(r.fullName()).thenReturn("Alice");
        when(r.username()).thenReturn("alice");
        when(r.email()).thenReturn("a@x.test");
        when(r.password()).thenReturn("pwd");
        when(r.role()).thenReturn(Role.EDITOR); // cabang role != null

        when(register.executeRegisterUser(any(RegisterUser.class))).thenReturn(100L);

        Instant now = Instant.now();
        User u = mockedUser(100L, "Alice", "alice", "a@x.test", Role.EDITOR, now);
        when(get.executeGetUser(any(GetUser.class))).thenReturn(u);

        ResponseEntity<UserResponse> resp = controller.create(r);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(URI.create("/api/v1/users/100"), resp.getHeaders().getLocation());
        UserResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(100L, respId(body));
        assertEquals("Alice", respFullName(body));
        assertEquals("alice", respUsername(body));
        assertEquals("a@x.test", respEmail(body));
        assertEquals(Role.EDITOR, respRole(body));
        assertEquals(now, respCreatedAt(body));

        verify(register).executeRegisterUser(argThat(cmd ->
                cmd.fullName().equals("Alice")
                        && cmd.username().equals("alice")
                        && cmd.email().equals("a@x.test")
                        && cmd.password().equals("pwd")
                        && cmd.role() == Role.EDITOR));
        verify(get).executeGetUser(argThat(q -> q.id().equals(100L)));
    }

    @Test
    void create_success_withNullRole_defaultsToViewer() {
        CreateUserRequest r = mock(CreateUserRequest.class);
        when(r.fullName()).thenReturn("Bob B");
        when(r.username()).thenReturn("bobby");
        when(r.email()).thenReturn("b@x.test");
        when(r.password()).thenReturn("pwd");
        when(r.role()).thenReturn(null); // cabang default Role.VIEWER

        when(register.executeRegisterUser(any(RegisterUser.class))).thenReturn(7L);

        Instant now = Instant.now();
        User u = mockedUser(7L, "Bob B", "bobby", "b@x.test", Role.VIEWER, now);
        when(get.executeGetUser(any(GetUser.class))).thenReturn(u);

        ResponseEntity<UserResponse> resp = controller.create(r);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(Role.VIEWER, respRole(resp.getBody()));

        verify(register).executeRegisterUser(argThat(cmd -> cmd.role() == Role.VIEWER));
        verify(get).executeGetUser(argThat(q -> q.id().equals(7L)));
    }

    @Test
    void create_whenRegisterThrows_isPropagated() {
        CreateUserRequest r = mock(CreateUserRequest.class);
        when(r.fullName()).thenReturn("X");
        when(r.username()).thenReturn("x");
        when(r.email()).thenReturn("x@x.test");
        when(r.password()).thenReturn("pwd");
        when(r.role()).thenReturn(Role.VIEWER);

        when(register.executeRegisterUser(any(RegisterUser.class)))
                .thenThrow(new IllegalStateException("reg-fail"));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> controller.create(r));
        assertEquals("reg-fail", ex.getMessage());
        verifyNoInteractions(get);
    }

    // ========== UPDATE ==========

    @Test
    void update_success_returnsOkMapped() {
        UpdateUserRequest r = mock(UpdateUserRequest.class);
        when(r.fullName()).thenReturn("New Name");
        when(r.email()).thenReturn("n@x.test");
        when(r.password()).thenReturn("newpwd");

        Instant now = Instant.now();
        User u = mockedUser(77L, "New Name", "user77", "n@x.test", Role.EDITOR, now);
        when(update.executeUpdateUser(any(UpdateUser.class))).thenReturn(u);

        ResponseEntity<UserResponse> resp = controller.update(77L, r);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(77L, respId(resp.getBody()));
        assertEquals("New Name", respFullName(resp.getBody()));
        assertEquals("user77", respUsername(resp.getBody()));
        assertEquals("n@x.test", respEmail(resp.getBody()));
        assertEquals(Role.EDITOR, respRole(resp.getBody()));
        assertEquals(now, respCreatedAt(resp.getBody()));

        verify(update).executeUpdateUser(argThat(cmd ->
                cmd.id().equals(77L) &&
                        cmd.fullName().equals("New Name") &&
                        cmd.email().equals("n@x.test") &&
                        cmd.newPassword().equals("newpwd")));
    }

    @Test
    void update_whenHandlerThrows_isPropagated() {
        UpdateUserRequest r = mock(UpdateUserRequest.class);
        when(r.fullName()).thenReturn("N");
        when(r.email()).thenReturn("e@x.test");
        when(r.password()).thenReturn(null);

        when(update.executeUpdateUser(any(UpdateUser.class)))
                .thenThrow(new RuntimeException("upd-fail"));

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> controller.update(5L, r));
        assertEquals("upd-fail", ex.getMessage());
    }

    // ========== SET ROLE ==========

    @Test
    void setRole_success_returnsOkMapped() {
        UpdateUserRoleRequest r = mock(UpdateUserRoleRequest.class);
        when(r.role()).thenReturn(Role.SUPER_ADMIN);

        Instant now = Instant.now();
        User u = mockedUser(9L, "S Name", "sname", "s@x.test", Role.SUPER_ADMIN, now);
        when(setRole.executeSetUserRole(any(SetUserRole.class))).thenReturn(u);

        ResponseEntity<UserResponse> resp = controller.setRole(9L, r);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(Role.SUPER_ADMIN, respRole(resp.getBody()));
        verify(setRole).executeSetUserRole(argThat(cmd ->
                cmd.id().equals(9L) && cmd.role() == Role.SUPER_ADMIN));
    }

    @Test
    void setRole_whenHandlerThrows_isPropagated() {
        UpdateUserRoleRequest r = mock(UpdateUserRoleRequest.class);
        when(r.role()).thenReturn(Role.EDITOR);

        when(setRole.executeSetUserRole(any(SetUserRole.class)))
                .thenThrow(new IllegalArgumentException("role-fail"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> controller.setRole(1L, r));
        assertEquals("role-fail", ex.getMessage());
    }

    // ========== GET ==========

    @Test
    void get_success_returnsOkMapped() {
        Instant now = Instant.now();
        User u = mockedUser(3L, "F", "user3", "f@x.test", Role.VIEWER, now);
        when(get.executeGetUser(any(GetUser.class))).thenReturn(u);

        ResponseEntity<UserResponse> resp = controller.get(3L);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(3L, respId(resp.getBody()));
        assertEquals("user3", respUsername(resp.getBody()));
        verify(get).executeGetUser(argThat(q -> q.id().equals(3L)));
    }

    @Test
    void get_whenHandlerThrows_isPropagated() {
        when(get.executeGetUser(any(GetUser.class))).thenThrow(new RuntimeException("get-fail"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.get(99L));
        assertEquals("get-fail", ex.getMessage());
    }

    // ========== LIST ==========

    @Test
    void list_success_nonEmpty_mapsAll() {
        Instant now = Instant.now();
        User u1 = mockedUser(1L, "A", "u1", "a@x.test", Role.VIEWER, now);
        User u2 = mockedUser(2L, "B", "u2", "b@x.test", Role.EDITOR, now);

        when(list.executeListUser(any(ListUsers.class))).thenReturn(List.of(u1, u2));

        ResponseEntity<List<UserResponse>> resp = controller.list(0, 20);

        assertEquals(200, resp.getStatusCodeValue());
        List<UserResponse> body = resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(1L, respId(body.get(0)));
        assertEquals(2L, respId(body.get(1)));
        assertEquals(Role.VIEWER, respRole(body.get(0)));
        assertEquals(Role.EDITOR, respRole(body.get(1)));

        verify(list).executeListUser(argThat(q -> q.page() == 0 && q.size() == 20));
    }

    @Test
    void list_success_empty_returnsEmptyList() {
        when(list.executeListUser(any(ListUsers.class))).thenReturn(List.of());

        ResponseEntity<List<UserResponse>> resp = controller.list(3, 5);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());

        verify(list).executeListUser(argThat(q -> q.page() == 3 && q.size() == 5));
    }

    @Test
    void list_whenHandlerThrows_isPropagated() {
        when(list.executeListUser(any(ListUsers.class))).thenThrow(new RuntimeException("list-fail"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.list(1, 1));
        assertEquals("list-fail", ex.getMessage());
    }

    // ========== DELETE ==========

    @Test
    void delete_success_returns204() {
        doNothing().when(delete).executeDeleteUser(any(DeleteUser.class));

        ResponseEntity<Void> resp = controller.delete(55L);

        assertEquals(204, resp.getStatusCodeValue());
        assertFalse(resp.hasBody());
        verify(delete).executeDeleteUser(argThat(cmd -> cmd.id().equals(55L)));
    }

    @Test
    void delete_whenHandlerThrows_isPropagated() {
        doThrow(new IllegalStateException("del-fail"))
                .when(delete).executeDeleteUser(any(DeleteUser.class));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> controller.delete(8L));
        assertEquals("del-fail", ex.getMessage());
    }
}
