package net.disburse.repository;

import net.disburse.model.Address;
import net.disburse.model.Balance;
import net.disburse.model.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Balance findByAddressAndStablecoin(Address address, Stablecoin stablecoin);
    
    @Query("SELECT a.id, a.address, a.nickname, s.name, b.balance " +
            "FROM Balance b " +
            "JOIN b.address a " +
            "JOIN b.stablecoin s")
    List<Object[]> findAllBalancesWithStablecoins();
}
