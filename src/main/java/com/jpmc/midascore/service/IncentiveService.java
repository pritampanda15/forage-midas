package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IncentiveService {
    private static final Logger logger = LoggerFactory.getLogger(IncentiveService.class);

    private final RestTemplate restTemplate;

    @Value("${incentive.api.url:http://localhost:8080/incentive}")
    private String incentiveApiUrl;

    public IncentiveService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public float getIncentive(Transaction transaction) {
        try {
            logger.info("Calling incentive API for transaction: {}", transaction);
            Incentive incentive = restTemplate.postForObject(incentiveApiUrl, transaction, Incentive.class);

            if (incentive != null) {
                logger.info("Received incentive: {}", incentive.getAmount());
                return incentive.getAmount();
            } else {
                logger.warn("No incentive received from API");
                return 0.0f;
            }
        } catch (Exception e) {
            logger.error("Error calling incentive API: {}", e.getMessage());
            return 0.0f;
        }
    }
}
