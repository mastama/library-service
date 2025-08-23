package com.yolifay.libraryservice.domain.usecase.auth.command;

public record RegisterUser(String fullName, String username, String email, String password) {
}
