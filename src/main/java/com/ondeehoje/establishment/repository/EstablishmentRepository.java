package com.ondeehoje.establishment.repository;

import com.ondeehoje.establishment.model.Establishment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    List<Establishment> findAllByOwner_IdOrderByCreatedAtDescIdAsc(UUID ownerId);

    Optional<Establishment> findByIdAndOwner_Id(UUID establishmentId, UUID ownerId);
}
