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

    private static final Map<String, String> stablecoins = Map.of(
            "USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            "USDC", "0xA0b86991C6218b36c1d19D4a2e9Eb0cE3606EB48",
            "BUSD", "0x4fabb145d64652a948d72533023f6e7a623c7c53",
            "DAI", "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            "USDD", "0x0A89bF8f1B2a3e6fEA92bDDfD2F5EB064f7fEfFf",
            "PayPal USD", "0xB547F85E6e2A7dF2cb074d19725eF00bD4Cb2423",
            "Pax", "0x8e870D67F660D95D5be530380D0eC0bd388289E1",
            "Gemini Dollar", "0x056Fd409E1d7A124BD7017459dfea2F387b6d5Cd"
    );

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

        Address addrFromRepo = addressRepository.findById(addressId).get();

        String address = addrFromRepo.getAddress();
        String nickname = addrFromRepo.getNickname();
        Map<String, String> balances = new HashMap<>();

        for (Map.Entry<String, String> coin : stablecoins.entrySet()) {
            try {
                String url = String.format(
                        "https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
                        coin.getValue(), address, EtherScan.getApiKey()
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode jsonResponse = mapper.readTree(response.body());

                String balance = jsonResponse.get("result").asText();
                balances.put(coin.getKey(), balance);

                BigDecimal bigBalance = new BigDecimal(balance);

                Stablecoin stablecoin = stablecoinRepository.findByName(coin.getKey());

                Balance bal = new Balance();
                bal.setAddress(addrFromRepo);
                bal.setBalance(bigBalance);
                bal.setStablecoin(stablecoin);
                bal.setLastUpdated(LocalDateTime.now());

                balanceRepository.save(bal);
            } catch (Exception e) {
                balances.put(coin.getKey(), "Error");
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

        List<Address> allAddresses = addressRepository.findAll();

        for (Address addrFromRepo : allAddresses) {
            String address = addrFromRepo.getAddress();
            String nickname = addrFromRepo.getNickname();
            Map<String, String> balances = new HashMap<>();

            for (Map.Entry<String, String> coin : stablecoins.entrySet()) {
                try {
                    String url = String.format(
                            "https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
                            coin.getValue(), address, EtherScan.getApiKey()
                    );

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode jsonResponse = mapper.readTree(response.body());

                    String balance = jsonResponse.get("result").asText();
                    balances.put(coin.getKey(), balance);

                    BigDecimal bigBalance = new BigDecimal(balance);

                    Stablecoin stablecoin = stablecoinRepository.findByName(coin.getKey());

                    Balance existingBalance = balanceRepository.findByAddressAndStablecoin(addrFromRepo, stablecoin);

                    if (existingBalance != null) {
                        existingBalance.setBalance(bigBalance);
                        existingBalance.setLastUpdated(LocalDateTime.now());
                        balanceRepository.save(existingBalance);
                    } else {
                        Balance newBalance = new Balance();
                        newBalance.setAddress(addrFromRepo);
                        newBalance.setBalance(bigBalance);
                        newBalance.setStablecoin(stablecoin);
                        newBalance.setLastUpdated(LocalDateTime.now());
                        balanceRepository.save(newBalance);
                    }
                } catch (Exception e) {
                    balances.put(coin.getKey(), "Error");
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
                    Transaction tx = new Transaction();
                    tx.setTxHash(txNode.get("hash").asText());
                    tx.setAmount(new BigDecimal(txNode.get("value").asText()));
                    tx.setAddress(addrFromRepo);
                    tx.setTimestamp(Instant.ofEpochSecond(txNode.get("timeStamp").asLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                    tx.setCreatedAt(Instant.ofEpochSecond(txNode.get("timeStamp").asLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());

                    transactions.add(tx);
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

    public List<Map<String, Object>> getTransactionsFromDb(long address) {
        Address addrFromRepo = addressRepository.findById(address).get();

        if (addrFromRepo == null) {
            throw new IllegalArgumentException("Address not found: " + address);
        }

        List<Transaction> transactions = transactionRepository.findByAddress(addrFromRepo);

        List<Map<String, Object>> transactionDetails = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("transactionId", transaction.getId());
            transactionMap.put("hash", transaction.getTxHash());
            transactionMap.put("timestamp", transaction.getTimestamp());
            transactionMap.put("value", transaction.getAmount());

            Stablecoin stablecoin = transaction.getStablecoin();
            if (stablecoin != null) {
                Map<String, String> stablecoinData = new HashMap<>();
                stablecoinData.put("name", stablecoin.getName());
                stablecoinData.put("fullName", stablecoin.getFullName());
                transactionMap.put("stablecoin", stablecoinData);
            } else {
                transactionMap.put("stablecoin", null);
            }

            transactionDetails.add(transactionMap);
        }

        return transactionDetails;
    }

    public Address getAddressByAddress(String address) {
        return addressRepository.findByAddress(address);
    }

    public List<Map<String, Object>> getAllBalances() {
        List<Address> addresses = addressRepository.findAll();
        List<Balance> balances = balanceRepository.findAll();

        List<Map<String, Object>> portfolio = new ArrayList<>();
        for (Address address : addresses) {
            Map<String, Object> addressPortfolio = new HashMap<>();
            addressPortfolio.put("id", address.getId());
            addressPortfolio.put("address", address.getAddress());
            addressPortfolio.put("nickname", address.getNickname());

            Map<String, BigDecimal> stablecoinBalances = balances.stream()
                    .filter(balance -> balance.getAddress().getId().equals(address.getId()))
                    .sorted(Comparator.comparing(Balance::getLastUpdated).reversed()) // Sort by most recent
                    .collect(Collectors.toMap(
                            balance -> balance.getStablecoin().getName(),
                            Balance::getBalance,
                            (balance1, balance2) -> balance1 // Keep the most recent based on the sort order
                    ));

            addressPortfolio.put("balances", stablecoinBalances);
            portfolio.add(addressPortfolio);
        }

        return portfolio;
    }
}
