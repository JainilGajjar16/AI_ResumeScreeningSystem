package com.resume.screening.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "New password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}
