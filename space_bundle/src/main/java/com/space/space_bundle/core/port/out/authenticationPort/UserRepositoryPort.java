package com.space.space_bundle.core.port.out.authenticationPort;



import com.space.space_bundle.core.entities.User;

import java.util.Optional;

/**
 * Output port for user persistence operations
 */
public interface UserRepositoryPort {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void updateFailedLoginAttempts(String userId, int attempts);
    void unlockAccount(String userId);
}
