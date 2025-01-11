package net.disburse.controller;

import lombok.Getter;
import net.disburse.model.Address;
import net.disburse.model.Stablecoin;
import net.disburse.model.Transaction;
import net.disburse.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/address")
public class AddressController {
    private final AddressService addressService;
    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping("/")
    public ResponseEntity<Boolean> saveNewAddress(@RequestBody AddressRequest addressRequest) {
        boolean checkExisting = addressService.checkExistingAddress(addressRequest.getAddress());

        if (checkExisting) {
            return ResponseEntity.badRequest().body(false);
        }

        boolean saveAddress = addressService.addNewAddress(addressRequest.getAddress(), addressRequest.getNickname());

        Address address = addressService.getAddressByAddress(addressRequest.getAddress());

        Map<String, Map<String, String>> saveBalance = addressService.savePortfolio(address.getId());

        return ResponseEntity.ok(saveAddress);
    }

    @GetMapping("/")
    public ResponseEntity<List<Map<String, Object>>> getAllBalances() {
        List<Map<String, Object>> allAddresses = addressService.getAllBalances();

        return ResponseEntity.ok(allAddresses);
    }

    @PutMapping("/portfolio/")
    public ResponseEntity<Map<String, Map<String, String>>> updatePortfolio() {
        Map<String, Map<String, String>> updateBalance = addressService.updatePortfolio();

        return ResponseEntity.ok(updateBalance);
    }

    @PostMapping("/portfolio/transactions/refresh")
    public ResponseEntity<List<Transaction>> refreshTransactions(@RequestBody AddressReq address) {
        try {
            List<Transaction> list = addressService.getLastTransactions(address.getAddress());

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);
        }
    }
    
    @PostMapping("/portfolio/transactions/")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @RequestBody AddressReq addressReq,
            @RequestParam(required = false) String stablecoinName) {
        try {
            logger.info("Received request for transactions. Address ID: {}, StablecoinName: {}",
                    addressReq.getAddress(), stablecoinName);
            
            List<Map<String, Object>> transactions;
            if (stablecoinName == null || stablecoinName.isEmpty()) {
                logger.info("Fetching all transactions for Address ID: {}", addressReq.getAddress());
                transactions = addressService.getAllTransactionsFromDb(addressReq.getAddress());
            } else {
                logger.info("Fetching transactions for Address ID: {} and StablecoinName: {}",
                        addressReq.getAddress(), stablecoinName);
                transactions = addressService.getTransactionsFromDb(addressReq.getAddress(), stablecoinName);
            }
            
            logger.info("Fetched {} transactions successfully.", transactions.size());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error while fetching transactions for Address ID: {}, StablecoinName: {}. Error: {}",
                    addressReq.getAddress(), stablecoinName, e.getMessage(), e);
            return ResponseEntity.status(400).body(null);
        }
    }
    
    @GetMapping("/stablecoins")
    public ResponseEntity<List<Map<String, Object>>> getAllStablecoins() {
        try {
            logger.info("Received request to fetch all stablecoins.");
            
            List<Stablecoin> stablecoins = addressService.getAllStablecoins();
            List<Map<String, Object>> stablecoinList = new ArrayList<>();
            
            for (Stablecoin stablecoin : stablecoins) {
                logger.info("Processing stablecoin: {}", stablecoin.getName());
                
                Map<String, Object> stablecoinMap = new HashMap<>();
                stablecoinMap.put("id", stablecoin.getId());
                stablecoinMap.put("name", stablecoin.getName());
                stablecoinMap.put("fullName", stablecoin.getFullName());
                stablecoinList.add(stablecoinMap);
            }
            
            logger.info("Fetched {} stablecoins successfully.", stablecoinList.size());
            return ResponseEntity.ok(stablecoinList);
        } catch (Exception e) {
            logger.error("Error while fetching stablecoins. Error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }
    
}

@Getter
class AddressRequest {
    private String address;
    private String nickname;
}

@Getter
class AddressReq {
    private long address;
}
