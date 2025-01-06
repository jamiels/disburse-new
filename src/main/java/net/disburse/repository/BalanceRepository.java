package net.disburse.repository;

import net.disburse.model.Address;
import net.disburse.model.Balance;
import net.disburse.model.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Balance findByAddressAndStablecoin(Address address, Stablecoin stablecoin);
}
