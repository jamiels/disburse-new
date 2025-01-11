package net.disburse.repository;

import net.disburse.model.Address;
import net.disburse.model.Stablecoin;
import net.disburse.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAddress(Address address);
    
    @Query("SELECT t FROM Transaction t WHERE t.address = :address AND t.stablecoin = :stablecoin")
    List<Transaction> findByAddressAndStablecoin(Address address, Stablecoin stablecoin);
}
