package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.core.port.out.authenticationPort.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {
    
    private final UserRepositoryPort userRepository;
    
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
