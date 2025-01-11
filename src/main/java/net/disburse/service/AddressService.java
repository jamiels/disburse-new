package net.disburse.service;

import net.disburse.model.Address;
import net.disburse.model.Balance;
import net.disburse.model.Stablecoin;
import net.disburse.model.Transaction;
import net.disburse.repository.AddressRepository;
import net.disburse.repository.BalanceRepository;
import net.disburse.repository.StablecoinRepository;
import net.disburse.repository.TransactionRepository;
import net.disburse.util.EtherScan;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

@Service
public class AddressService {
    private final StablecoinRepository stablecoinRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository, StablecoinRepository stablecoinRepository, BalanceRepository balanceRepository, TransactionRepository transactionRepository) {
        this.addressRepository = addressRepository;
        this.stablecoinRepository = stablecoinRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    public boolean addNewAddress(String address, String nickname) {
        try {
            boolean checkWallet = EtherScan.checkWalletAddress(address);

            if (!checkWallet) {
                return false;
            }

            Address addr = new Address();
            addr.setAddress(address);
            addr.setNickname(nickname);
            addr.setCreatedAt(LocalDateTime.now());
            addr.setUpdatedAt(LocalDateTime.now());

            addressRepository.save(addr);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());

            return false;
        }
    }

    public boolean checkExistingAddress(String address) {
        try {
            Address addr = addressRepository.findByAddress(address);

            if (addr != null) {
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());

            return false;
        }
    }
    
    public Map<String, Map<String, String>> savePortfolio(long addressId) {
        Map<String, Map<String, String>> portfolio = new HashMap<>();
        
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        
        // Fetch the address details from the database
        Address addrFromRepo = addressRepository.findById(addressId).orElseThrow(
                () -> new IllegalArgumentException("Address not found for ID: " + addressId));
        
        String address = addrFromRepo.getAddress();
        String nickname = addrFromRepo.getNickname();
        Map<String, String> balances = new HashMap<>();
        
        // Fetch stablecoins dynamically from the database
        List<Stablecoin> stablecoinList = stablecoinRepository.findAll();
        
        for (Stablecoin stablecoin : stablecoinList) {
            try {
                // Etherscan API URL for fetching token balance
                String url = String.format(
                        "https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
                        stablecoin.getName(), address, EtherScan.getApiKey()
                );
                
                // Send HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode jsonResponse = mapper.readTree(response.body());
                
                // Extract and save balance
                String balance = jsonResponse.get("result").asText();
                balances.put(stablecoin.getName(), balance);
                
                BigDecimal bigBalance = new BigDecimal(balance);
                
                // Create or update the Balance entity
                Balance bal = new Balance();
                bal.setAddress(addrFromRepo);
                bal.setBalance(bigBalance);
                bal.setStablecoin(stablecoin);
                bal.setLastUpdated(LocalDateTime.now());
                
                balanceRepository.save(bal);
            } catch (Exception e) {
                balances.put(stablecoin.getName(), "Error");
                e.printStackTrace();
            }
        }
        
        portfolio.put(nickname, balances);
        
        return portfolio;
    }
    
    public Map<String, Map<String, String>> updatePortfolio() {
        Map<String, Map<String, String>> portfolio = new HashMap<>();
        
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        
        // Fetch all addresses from the database
        List<Address> allAddresses = addressRepository.findAll();
        
        // Fetch all stablecoins dynamically from the database
        List<Stablecoin> stablecoinList = stablecoinRepository.findAll();
        
        for (Address addrFromRepo : allAddresses) {
            String address = addrFromRepo.getAddress();
            String nickname = addrFromRepo.getNickname();
            Map<String, String> balances = new HashMap<>();
            
            for (Stablecoin stablecoin : stablecoinList) {
                try {
                    // Etherscan API URL for fetching token balance
                    String url = String.format(
                            "https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
                            stablecoin.getName(), address, EtherScan.getApiKey()
                    );
                    
                    // Send HTTP request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode jsonResponse = mapper.readTree(response.body());
                    
                    // Extract and save balance
                    String balance = jsonResponse.get("result").asText();
                    balances.put(stablecoin.getName(), balance);
                    
                    BigDecimal bigBalance = new BigDecimal(balance);
                    
                    // Check if a balance entry already exists
                    Balance existingBalance = balanceRepository.findByAddressAndStablecoin(addrFromRepo, stablecoin);
                    
                    if (existingBalance != null) {
                        // Update existing balance
                        existingBalance.setBalance(bigBalance);
                        existingBalance.setLastUpdated(LocalDateTime.now());
                        balanceRepository.save(existingBalance);
                    } else {
                        // Create new balance entry
                        Balance newBalance = new Balance();
                        newBalance.setAddress(addrFromRepo);
                        newBalance.setBalance(bigBalance);
                        newBalance.setStablecoin(stablecoin);
                        newBalance.setLastUpdated(LocalDateTime.now());
                        balanceRepository.save(newBalance);
                    }
                } catch (Exception e) {
                    balances.put(stablecoin.getName(), "Error");
                    e.printStackTrace();
                }
            }
            
            portfolio.put(nickname, balances);
        }
        
        return portfolio;
    }
    
    public List<Transaction> getLastTransactions(long addressId) {
        Address addrFromRepo = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found for ID: " + addressId));
        
        String address = addrFromRepo.getAddress();
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            String url = String.format(
                    "https://api.etherscan.io/api?module=account&action=txlist&address=%s&page=1&offset=20&sort=desc&apikey=%s",
                    address, EtherScan.getApiKey()
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonResponse = mapper.readTree(response.body());
            
            if ("1".equals(jsonResponse.get("status").asText())) {
                for (JsonNode txNode : jsonResponse.get("result")) {
                    String contractAddress = txNode.get("to").asText();
                    Stablecoin stablecoin = stablecoinRepository.findByName(contractAddress);
                    
                    // If the stablecoin doesn't exist, skip this transaction
                    if (stablecoin == null) continue;
                    
                    Transaction tx = new Transaction();
                    tx.setTxHash(txNode.get("hash").asText());
                    tx.setAmount(new BigDecimal(txNode.get("value").asText()));
                    tx.setAddress(addrFromRepo);
                    tx.setStablecoin(stablecoin); // Set the stablecoin
                    tx.setTimestamp(Instant.ofEpochSecond(txNode.get("timeStamp").asLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                    tx.setCreatedAt(Instant.ofEpochSecond(txNode.get("timeStamp").asLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                    
                    transactions.add(tx);
                    
                    // Save transaction to the database
                    transactionRepository.save(tx);
                }
            } else {
                throw new RuntimeException("Failed to fetch transactions: " + jsonResponse.get("message").asText());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while fetching transactions", e);
        }
        
        return transactions;
    }
    
    public List<Map<String, Object>> getTransactionsFromDb(long addressId, String stablecoinName) {
        Address addrFromRepo = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found for ID: " + addressId));
        
        Stablecoin stablecoin = stablecoinRepository.findByName(stablecoinName);
        if (stablecoin == null) {
            throw new IllegalArgumentException("Stablecoin not found: " + stablecoinName);
        }
        
        List<Transaction> transactions = transactionRepository.findByAddressAndStablecoin(addrFromRepo, stablecoin);
        
        List<Map<String, Object>> transactionDetails = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("transactionId", transaction.getId());
            transactionMap.put("hash", transaction.getTxHash());
            transactionMap.put("timestamp", transaction.getTimestamp());
            transactionMap.put("value", transaction.getAmount());
            transactionMap.put("stablecoin", stablecoin.getName());
            transactionDetails.add(transactionMap);
        }
        
        return transactionDetails;
    }
    
    public List<Map<String, Object>> getAllTransactionsFromDb(long addressId) {
        Address addrFromRepo = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found for ID: " + addressId));
        
        List<Transaction> transactions = transactionRepository.findByAddress(addrFromRepo);
        
        List<Map<String, Object>> transactionDetails = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("transactionId", transaction.getId());
            transactionMap.put("hash", transaction.getTxHash());
            transactionMap.put("timestamp", transaction.getTimestamp());
            transactionMap.put("value", transaction.getAmount());
            
            // Handle the case where Stablecoin is null
            Stablecoin stablecoin = transaction.getStablecoin();
            transactionMap.put("stablecoin", stablecoin != null ? stablecoin.getName() : "Unknown");
            
            transactionDetails.add(transactionMap);
        }
        
        return transactionDetails;
    }
    
    
    public Address getAddressByAddress(String address) {
        return addressRepository.findByAddress(address);
    }
    
    public List<Map<String, Object>> getAllBalances() {
        // Use a custom query in the BalanceRepository to fetch required data
        List<Object[]> results = balanceRepository.findAllBalancesWithStablecoins();
        
        // Transform query results into structured portfolio data
        Map<Long, Map<String, Object>> portfolioMap = new HashMap<>();
        
        for (Object[] row : results) {
            Long addressId = (Long) row[0];
            String address = (String) row[1];
            String nickname = (String) row[2];
            String stablecoinName = (String) row[3];
            BigDecimal balance = (BigDecimal) row[4];
            
            // Create or update the address portfolio
            portfolioMap.computeIfAbsent(addressId, id -> {
                Map<String, Object> portfolio = new HashMap<>();
                portfolio.put("id", addressId);
                portfolio.put("address", address);
                portfolio.put("nickname", nickname);
                portfolio.put("balances", new HashMap<String, BigDecimal>());
                return portfolio;
            });
            
            // Add balance to the existing portfolio entry
            Map<String, BigDecimal> balances = (Map<String, BigDecimal>) portfolioMap.get(addressId).get("balances");
            balances.put(stablecoinName, balance);
        }
        
        return new ArrayList<>(portfolioMap.values());
    }
    public List<Stablecoin> getAllStablecoins() {
        return stablecoinRepository.findAll();
    }
    
}
