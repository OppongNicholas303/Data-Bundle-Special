package com.space.space_bundle.security;

import com.space.space_bundle.entity.User;
import com.space.space_bundle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user);
    }

    public static class CustomUserDetails implements UserDetails {

        private final User user;

        public CustomUserDetails(User user) { this.user = user; }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return user.getRoles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        @Override public String getPassword() { return user.getPassword(); }
        @Override public String getUsername() { return user.getUsername(); }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return user.isAccountNonLocked(); }
        @Override public boolean isCredentialsNonExpired() { return !user.isPasswordExpired(); }
        @Override public boolean isEnabled() { return user.isEnabled(); }

        public String getUserId() { return user.getId(); }
        public String getEmail() { return user.getEmail(); }
        public User getUser() { return user; }
    }
}
