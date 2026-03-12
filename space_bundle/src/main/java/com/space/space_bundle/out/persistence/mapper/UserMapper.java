package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.out.persistence.entity.UserDocument;
import lombok.experimental.UtilityClass;

import java.util.stream.Collectors;

@UtilityClass
public class UserMapper {

        public UserDocument toDocument(User user) {
                if (user == null)
                        return null;

                return UserDocument.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .password(user.getPassword())
                                .phoneNumber(user.getPhoneNumber())
                                .roles(
                                                user.getRoles().stream()
                                                                .map(Enum::name)
                                                                .collect(Collectors.toSet()))
                                .enabled(user.isEnabled())
                                .accountNonLocked(user.isAccountNonLocked())
                                .failedLoginAttempts(user.getFailedLoginAttempts())
                                .lastFailedLogin(user.getLastFailedLogin())
                                .lastSuccessfulLogin(user.getLastSuccessfulLogin())
                                .passwordLastChanged(user.getPasswordLastChanged())
                                .passwordResetToken(user.getPasswordResetToken())
                                .passwordResetTokenExpiry(user.getPasswordResetTokenExpiry())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .build();
        }

        public User toDomain(UserDocument document) {
                if (document == null)
                        return null;

                return User.builder()
                                .id(document.getId())
                                .username(document.getUsername())
                                .email(document.getEmail())
                                .password(document.getPassword())
                                .phoneNumber(document.getPhoneNumber())
                                .roles(
                                                document.getRoles().stream()
                                                                .map(User.Role::valueOf)
                                                                .collect(Collectors.toSet()))
                                .enabled(document.isEnabled())
                                .accountNonLocked(document.isAccountNonLocked())
                                .failedLoginAttempts(document.getFailedLoginAttempts())
                                .lastFailedLogin(document.getLastFailedLogin())
                                .lastSuccessfulLogin(document.getLastSuccessfulLogin())
                                .passwordLastChanged(document.getPasswordLastChanged())
                                .passwordResetToken(document.getPasswordResetToken())
                                .passwordResetTokenExpiry(document.getPasswordResetTokenExpiry())
                                .createdAt(document.getCreatedAt())
                                .updatedAt(document.getUpdatedAt())
                                .build();
        }
}
