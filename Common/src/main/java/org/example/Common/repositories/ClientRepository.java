package org.example.Common.repositories;

import org.example.Common.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{

    Client findClientByClientId(Long clientId);

}
