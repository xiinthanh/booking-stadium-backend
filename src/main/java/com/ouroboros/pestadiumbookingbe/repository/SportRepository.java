package com.ouroboros.pestadiumbookingbe.repository;

import com.ouroboros.pestadiumbookingbe.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SportRepository extends JpaRepository<Sport, UUID> {
}

