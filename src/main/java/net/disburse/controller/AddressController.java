package net.disburse.controller;

import lombok.Getter;
import net.disburse.model.Address;
import net.disburse.model.Transaction;
import net.disburse.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class AddressController {
    private final AddressService addressService;

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
    public ResponseEntity<List<Map<String, Object>>> getTransactions(@RequestBody AddressReq address) {
        List<Map<String, Object>> list = addressService.getTransactionsFromDb(address.getAddress());

        return ResponseEntity.ok(list);
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
