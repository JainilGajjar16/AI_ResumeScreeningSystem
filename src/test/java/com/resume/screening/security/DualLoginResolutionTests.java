package com.resume.screening.security;

import com.resume.screening.entity.Resume;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.UserResolutionService;
import com.resume.screening.service.impl.UserResolutionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DualLoginResolutionTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    private UserResolutionService userResolutionService;
    private CustomUserDetailsService customUserDetailsService;

    private User sampleStudentUser;
    private StudentProfile sampleProfile;

    @BeforeEach
    void setUp() {
        userResolutionService = new UserResolutionServiceImpl(userRepository, studentProfileRepository, recruiterRepository);
        customUserDetailsService = new CustomUserDetailsService(userRepository);

        sampleStudentUser = User.builder()
                .id(42L)
                .username("Jainil")
                .email("jainilgajjar@gmail.com")
                .firstName("Jainil")
                .lastName("Gajjar")
                .password("encoded_password")
                .role("STUDENT")
                .build();

        sampleProfile = StudentProfile.builder()
                .id(100L)
                .user(sampleStudentUser)
                .college("Engineering College")
                .branch("Computer Science")
                .build();
    }

    @Test
    @DisplayName("Case 1: Resolve authenticated user when login identifier is username")
    void testResolveUserByUsername() {
        when(userRepository.findByUsernameIgnoreCase("Jainil")).thenReturn(Optional.of(sampleStudentUser));

        User user = userResolutionService.resolveUser("Jainil");

        assertNotNull(user);
        assertEquals(42L, user.getId());
        assertEquals("Jainil", user.getUsername());
        assertEquals("jainilgajjar@gmail.com", user.getEmail());
    }

    @Test
    @DisplayName("Case 2: Resolve authenticated user when login identifier is email")
    void testResolveUserByEmail() {
        when(userRepository.findByEmailIgnoreCase("jainilgajjar@gmail.com")).thenReturn(Optional.of(sampleStudentUser));

        User user = userResolutionService.resolveUser("jainilgajjar@gmail.com");

        assertNotNull(user);
        assertEquals(42L, user.getId());
        assertEquals("Jainil", user.getUsername());
        assertEquals("jainilgajjar@gmail.com", user.getEmail());
    }

    @Test
    @DisplayName("Both username and email resolve to the EXACT SAME User and StudentProfile")
    void testBothLoginMethodsResolveSameAccount() {
        when(userRepository.findByUsernameIgnoreCase("Jainil")).thenReturn(Optional.of(sampleStudentUser));
        when(userRepository.findByEmailIgnoreCase("jainilgajjar@gmail.com")).thenReturn(Optional.of(sampleStudentUser));
        when(studentProfileRepository.findByUserId(42L)).thenReturn(Optional.of(sampleProfile));

        User fromUsername = userResolutionService.resolveUser("Jainil");
        User fromEmail = userResolutionService.resolveUser("jainilgajjar@gmail.com");

        assertEquals(fromUsername.getId(), fromEmail.getId());
        assertEquals(fromUsername.getUsername(), fromEmail.getUsername());
        assertEquals(fromUsername.getEmail(), fromEmail.getEmail());

        StudentProfile profileFromUsername = userResolutionService.resolveStudentProfile(fromUsername);
        StudentProfile profileFromEmail = userResolutionService.resolveStudentProfile(fromEmail);

        assertEquals(profileFromUsername.getId(), profileFromEmail.getId());
        assertEquals(profileFromUsername.getUser().getId(), profileFromEmail.getUser().getId());
    }

    @Test
    @DisplayName("Spring Security Authentication#getName() resolves correctly for both username and email tokens")
    void testResolveAuthenticatedUserWithAuthenticationTokens() {
        // Token 1: Name is username
        Authentication usernameAuth = new UsernamePasswordAuthenticationToken("Jainil", "credentials");
        when(userRepository.findByUsernameIgnoreCase("Jainil")).thenReturn(Optional.of(sampleStudentUser));

        User user1 = userResolutionService.resolveAuthenticatedUser(usernameAuth);
        assertNotNull(user1);
        assertEquals(42L, user1.getId());

        // Token 2: Name is email
        Authentication emailAuth = new UsernamePasswordAuthenticationToken("jainilgajjar@gmail.com", "credentials");
        when(userRepository.findByEmailIgnoreCase("jainilgajjar@gmail.com")).thenReturn(Optional.of(sampleStudentUser));

        User user2 = userResolutionService.resolveAuthenticatedUser(emailAuth);
        assertNotNull(user2);
        assertEquals(42L, user2.getId());

        // Token 3: Principal is CustomUserDetails
        CustomUserDetails cud = new CustomUserDetails(sampleStudentUser);
        Authentication customUserDetailsAuth = new UsernamePasswordAuthenticationToken(cud, "credentials", cud.getAuthorities());
        when(userRepository.findById(42L)).thenReturn(Optional.of(sampleStudentUser));

        User user3 = userResolutionService.resolveAuthenticatedUser(customUserDetailsAuth);
        assertNotNull(user3);
        assertEquals(42L, user3.getId());
    }

    @Test
    @DisplayName("CustomUserDetailsService loads UserDetails successfully with both username and email")
    void testCustomUserDetailsServiceBothLoginMethods() {
        when(userRepository.findByUsernameIgnoreCase("Jainil")).thenReturn(Optional.of(sampleStudentUser));
        when(userRepository.findByEmailIgnoreCase("jainilgajjar@gmail.com")).thenReturn(Optional.of(sampleStudentUser));

        UserDetails detailsByUsername = customUserDetailsService.loadUserByUsername("Jainil");
        UserDetails detailsByEmail = customUserDetailsService.loadUserByUsername("jainilgajjar@gmail.com");

        assertNotNull(detailsByUsername);
        assertNotNull(detailsByEmail);
        assertEquals("Jainil", detailsByUsername.getUsername());
        assertEquals("Jainil", detailsByEmail.getUsername());
    }

    @Test
    @DisplayName("Case-insensitivity test for mixed case username and email")
    void testCaseInsensitiveResolution() {
        when(userRepository.findByUsernameIgnoreCase("jainil")).thenReturn(Optional.of(sampleStudentUser));
        when(userRepository.findByEmailIgnoreCase("JainilGajjar@GMAIL.COM")).thenReturn(Optional.of(sampleStudentUser));

        User userLower = userResolutionService.resolveUser("jainil");
        User userUpperEmail = userResolutionService.resolveUser("JainilGajjar@GMAIL.COM");

        assertNotNull(userLower);
        assertNotNull(userUpperEmail);
        assertEquals(42L, userLower.getId());
        assertEquals(42L, userUpperEmail.getId());
    }

    @Test
    @DisplayName("Throws informative exception when user is not found")
    void testUserNotFoundThrowsException() {
        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameOrEmail("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userResolutionService.resolveUser("nonexistent"));
        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("nonexistent"));
    }

    @Test
    @DisplayName("Email detection correctly identifies email formats")
    void testIsEmailDetection() {
        assertTrue(userResolutionService.isEmail("jainilgajjar@gmail.com"));
        assertTrue(userResolutionService.isEmail("user.name+tag@example.co.uk"));
        assertFalse(userResolutionService.isEmail("Jainil"));
        assertFalse(userResolutionService.isEmail("jainil G"));
        assertFalse(userResolutionService.isEmail(null));
        assertFalse(userResolutionService.isEmail(""));
        assertFalse(userResolutionService.isEmail("@"));
    }
}
