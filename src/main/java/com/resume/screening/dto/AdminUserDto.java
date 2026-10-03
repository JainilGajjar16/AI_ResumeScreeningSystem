package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private LocalDateTime createdAt;
    private String status;
}
