package com.space.space_bundle.out.persistence.entity;

import com.space.space_bundle.core.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MongoDB document for User - adapter to core entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String phoneNumber;
    private Set<String> roles;
    private boolean enabled;
    private boolean accountNonLocked;
    private int failedLoginAttempts;
    private LocalDateTime lastFailedLogin;
    private LocalDateTime lastSuccessfulLogin;
    private LocalDateTime passwordLastChanged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

//    public static UserDocument fromDomain(User user) {
//        return UserDocument.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .password(user.getPassword())
//                .phoneNumber(user.getPhoneNumber())
//                .roles(user.getRoles().stream()
//                        .map(Enum::name)
//                        .collect(Collectors.toSet()))
//                .enabled(user.isEnabled())
//                .accountNonLocked(user.isAccountNonLocked())
//                .failedLoginAttempts(user.getFailedLoginAttempts())
//                .lastFailedLogin(user.getLastFailedLogin())
//                .lastSuccessfulLogin(user.getLastSuccessfulLogin())
//                .passwordLastChanged(user.getPasswordLastChanged())
//                .createdAt(user.getCreatedAt())
//                .updatedAt(user.getUpdatedAt())
//                .build();
//    }
//
//    public User toDomain() {
//        return User.builder()
//                .id(this.id)
//                .username(this.username)
//                .email(this.email)
//                .password(this.password)
//                .phoneNumber(this.phoneNumber)
//                .roles(this.roles.stream()
//                        .map(User.Role::valueOf)
//                        .collect(Collectors.toSet()))
//                .enabled(this.enabled)
//                .accountNonLocked(this.accountNonLocked)
//                .failedLoginAttempts(this.failedLoginAttempts)
//                .lastFailedLogin(this.lastFailedLogin)
//                .lastSuccessfulLogin(this.lastSuccessfulLogin)
//                .passwordLastChanged(this.passwordLastChanged)
//                .createdAt(this.createdAt)
//                .updatedAt(this.updatedAt)
//                .build();
//    }
}
