package org.example.Common.repositories;

import org.example.Common.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByClient_ClientId(Long clientId);
}
