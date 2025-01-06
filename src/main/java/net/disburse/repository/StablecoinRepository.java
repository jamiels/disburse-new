package net.disburse.repository;

import net.disburse.model.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StablecoinRepository extends JpaRepository<Stablecoin, Long> {
    Stablecoin findByName(String name);
}

