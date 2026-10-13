package com.docmind.response;

import com.docmind.dto.UserDto;

public record LoginResponse(String accessToken, UserDto userDto) {

}
