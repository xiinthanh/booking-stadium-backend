package com.ouroboros.pestadiumbookingbe.repository;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SportHallRepository extends JpaRepository<SportHall, UUID> {
}
