package com.resume.screening.security;

import com.resume.screening.entity.User;
import com.resume.screening.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username or email cannot be empty");
        }

        String trimmed = usernameOrEmail.trim();
        User user;

        if (isEmail(trimmed)) {
            user = userRepository.findByEmailIgnoreCase(trimmed)
                    .or(() -> userRepository.findByEmail(trimmed))
                    .or(() -> userRepository.findByUsernameOrEmail(trimmed))
                    .or(() -> userRepository.findByUsernameIgnoreCase(trimmed))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + trimmed));
        } else {
            user = userRepository.findByUsernameIgnoreCase(trimmed)
                    .or(() -> userRepository.findByUsername(trimmed))
                    .or(() -> userRepository.findByUsernameOrEmail(trimmed))
                    .or(() -> userRepository.findByEmailIgnoreCase(trimmed))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + trimmed));
        }

        return new CustomUserDetails(user);
    }

    private boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@") && identifier.indexOf('@') > 0 && identifier.indexOf('@') < identifier.length() - 1;
    }
}
