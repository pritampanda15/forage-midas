package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IncentiveService incentiveService;

    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository, IncentiveService incentiveService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.incentiveService = incentiveService;
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        long senderId = transaction.getSenderId();
        long recipientId = transaction.getRecipientId();
        float amount = transaction.getAmount();

        logger.info("Processing transaction: sender={}, recipient={}, amount={}", senderId, recipientId, amount);

        // Validate sender exists
        UserRecord sender = userRepository.findById(senderId);
        if (sender == null) {
            logger.warn("Transaction rejected: Invalid senderId {}", senderId);
            return;
        }

        // Validate recipient exists
        UserRecord recipient = userRepository.findById(recipientId);
        if (recipient == null) {
            logger.warn("Transaction rejected: Invalid recipientId {}", recipientId);
            return;
        }

        // Validate sender has sufficient balance
        if (sender.getBalance() < amount) {
            logger.warn("Transaction rejected: Insufficient balance for sender {}. Balance: {}, Required: {}",
                    senderId, sender.getBalance(), amount);
            return;
        }

        // Get incentive from API
        float incentive = incentiveService.getIncentive(transaction);

        // Process transaction: adjust balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentive);

        // Save updated user records
        userRepository.save(sender);
        userRepository.save(recipient);

        // Save transaction record with incentive
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount, incentive);
        transactionRepository.save(transactionRecord);

        logger.info("Transaction processed successfully: {} -> {} amount {} + incentive {}",
                sender.getName(), recipient.getName(), amount, incentive);

        // Log balance updates for debugging
        if (sender.getName().equals("wilbur") || recipient.getName().equals("wilbur")) {
            UserRecord wilbur = sender.getName().equals("wilbur") ? sender : recipient;
            logger.info("WILBUR BALANCE UPDATE: {}", wilbur.getBalance());
        }
    }
}
