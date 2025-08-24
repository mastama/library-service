package com.yolifay.libraryservice.infrastructure.web;

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
import com.yolifay.libraryservice.infrastructure.audit.Audited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {

    private final RegisterUserHandler register;
    private final UpdateUserHandler update;
    private final SetUserRoleHandler setRole;
    private final GetUserHandler get;
    private final ListUsersHandler list;
    private final DeleteUserHandler delete;

    // CREATE user (SUPER_ADMIN)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @Audited(action = "CREATE_USER")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest r){
        log.info("[USER] CREATE req username={} email={} role={}", r.username(), r.email(), r.role());
        Role role = (r.role()==null ? Role.VIEWER : r.role());
        Long id = register.executeRegisterUser(new RegisterUser(
                r.fullName(), r.username(), r.email(), r.password(), role));
        User u = get.executeGetUser(new GetUser(id));
        return ResponseEntity.created(URI.create("/api/v1/users/" + id)).body(toResponse(u));
    }

    // UPDATE profile fields (fullName/email/password)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    @Audited(action = "UPDATE_USER")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r){
        log.info("[USER] UPDATE req id={} email={} (pw? {})", id, r.email(), r.password()!=null);
        User u = update.executeUpdateUser(new UpdateUser(id, r.fullName(), r.email(), r.password()));
        return ResponseEntity.ok(toResponse(u));
    }

    // UPDATE role
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/role")
    @Audited(action = "SET_USER_ROLE")
    public ResponseEntity<UserResponse> setRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest r){
        log.info("[USER] SET ROLE req id={} role={}", id, r.role());
        User u = setRole.executeSetUserRole(new SetUserRole(id, r.role()));
        return ResponseEntity.ok(toResponse(u));
    }

    // GET by id
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id){
        User u = get.executeGetUser(new GetUser(id));
        return ResponseEntity.ok(toResponse(u));
    }

    // LIST
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> list(@RequestParam(defaultValue="0") int page,
                                                   @RequestParam(defaultValue="20") int size){
        var items = list.executeListUser(new ListUsers(page, size)).stream().map(UsersController::toResponse).toList();
        return ResponseEntity.ok(items);
    }

    // DELETE
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @Audited(action = "DELETE_USER")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        delete.executeDeleteUser(new DeleteUser(id));
        return ResponseEntity.noContent().build();
    }

    private static UserResponse toResponse(User u){
        return new UserResponse(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}

