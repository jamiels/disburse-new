package net.disburse.repository;

import net.disburse.model.Address;
import net.disburse.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAddress(Address address);
}

