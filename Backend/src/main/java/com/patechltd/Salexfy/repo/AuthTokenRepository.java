package com.patechltd.Salexfy.repo;

import com.patechltd.Salexfy.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    Optional<AuthToken> findByToken(String token);

    void deleteByExpiresAtLessThan(long now);
}
