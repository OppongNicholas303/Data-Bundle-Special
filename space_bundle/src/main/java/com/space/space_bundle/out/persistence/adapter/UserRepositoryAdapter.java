package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.core.port.out.authenticationPort.UserRepositoryPort;
import com.space.space_bundle.out.persistence.entity.UserDocument;
import com.space.space_bundle.out.persistence.mapper.UserMapper;
import com.space.space_bundle.out.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB adapter for User repository operations
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserDocument document = UserMapper.toDocument(user);
        UserDocument saved = userRepository.save(document);
        return UserMapper.toDomain(saved);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void updateFailedLoginAttempts(String userId, int attempts) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
        });
    }

    @Override
    public void unlockAccount(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        });
    }
}