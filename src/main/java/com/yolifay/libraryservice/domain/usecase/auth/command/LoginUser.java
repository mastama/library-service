package com.yolifay.libraryservice.domain.usecase.auth.command;

public record LoginUser (String usernameOrEmail, String password) {
}
