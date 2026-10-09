package com.mb.callassistant.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RmUserRepository extends JpaRepository<RmUser, String> {

    Optional<RmUser> findByUsername(String username);
}
