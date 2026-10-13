package com.docmind.dto;

import com.docmind.utils.Role;

public record UserDto(Long id, String username, String email, Role role) {

}
