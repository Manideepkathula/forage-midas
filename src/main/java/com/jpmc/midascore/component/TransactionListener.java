package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {

    @Autowired
    private TransactionValidator transactionValidator;

    @Autowired
    private UserRepository userRepository;

    private RestTemplate restTemplate = new RestTemplate();

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        if (transactionValidator.isValid(transaction)) {
            UserRecord sender = userRepository.findById(transaction.getSenderId());
            UserRecord recipient = userRepository.findById(transaction.getRecipientId());

            Incentive incentive = restTemplate.postForObject(
                "http://localhost:8080/incentive",
                transaction,
                Incentive.class
            );

            float incentiveAmount = incentive != null ? incentive.getAmount() : 0;

            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

            userRepository.save(sender);
            userRepository.save(recipient);

            UserRecord wilbur = userRepository.findByName("wilbur");
            if (wilbur != null) {
                System.out.println("Wilbur balance: " + wilbur.getBalance());
            }
        }
    }
}
