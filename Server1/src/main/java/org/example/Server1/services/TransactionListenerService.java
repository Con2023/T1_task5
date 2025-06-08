package org.example.Server1.services;

import org.example.Common.DTO.ClientStatusResponse;
import org.example.Common.entities.Client;
import org.example.Common.entities.User;
import org.example.Common.repositories.ClientRepository;
import org.example.Common.repositories.UserRepository;
import org.slf4j.Logger;
import org.example.Common.DTO.TransactionAsseptedMessage;
import org.example.Common.DTO.TransactionResultMessage;
import org.example.Common.DTO.TransactionSendMessage;
import org.example.Common.DataSourceErrorLogAnnotation;
import org.example.Common.entities.Account;
import org.example.Common.entities.Transaction;
import org.example.Common.repositories.AccountRepository;
import org.example.Common.repositories.TransactionRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class TransactionListenerService {

    @Value("${transaction.rejection.limits.max-count}")
    private Long rejectionLimit;


    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientStatusHttpService clientStatusHttpService;

    private final KafkaTemplate<String, TransactionSendMessage> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(TransactionListenerService.class);

    public TransactionListenerService(TransactionRepository transactionRepository, AccountRepository accountRepository, UserRepository userRepository, ClientRepository clientRepository, ClientStatusHttpService clientStatusHttpService, @Qualifier("kafkaTemplate") KafkaTemplate<String, TransactionSendMessage> kafkaTemplate) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.clientStatusHttpService = clientStatusHttpService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @DataSourceErrorLogAnnotation
    @KafkaListener(topics = "t1_demo_transactions",  groupId = "common-service-group",containerFactory = "kafkaListenerContainerFactory")
    public void listen(TransactionAsseptedMessage transactionAsseptedMessage) {
        User user = userRepository.findUserByClient_ClientId(transactionAsseptedMessage.getClientId());
         Account account = accountRepository.findByAccountId(transactionAsseptedMessage.getAccountId());

        switch (user.getStatusUser()){
            case ACTIVE -> {
                try {
                if (account.getStatus().equals(Account.AccountStatus.OPEN)) {
                    Transaction transaction = createTransaction(account,transactionAsseptedMessage);
                    TransactionSendMessage transactionSendMessage = new TransactionSendMessage(
                            account.getClient().getClientId(),
                            account.getAccountId(),
                            transaction.getTransactionId(),
                            transactionAsseptedMessage.getAmount(),
                            account.getBalance()
                    );
                    kafkaTemplate.send("t1_demo_transaction_accept", transactionSendMessage);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }}
            case BLOCKED -> {
                Transaction transaction = new Transaction();
                transaction.setStatus(Transaction.TransactionStatus.BLOCKED);
                transaction.setAccount(account);
                transactionRepository.save(transaction);
            }
            case NULL -> {
                ClientStatusResponse clientStatusResponse = clientStatusHttpService.checkClientStatus(transactionAsseptedMessage.getClientId(), transactionAsseptedMessage.getAccountId());
                if (clientStatusResponse.getStatus().equals("ACTIVE")){
                    user.setStatusUser(User.StatusUser.ACTIVE);
                    userRepository.save(user);
                    if (account.getStatus().equals(Account.AccountStatus.OPEN)) { // сделать через else и проверку что статус не
                        Transaction transaction = createTransaction(account,transactionAsseptedMessage);
                        TransactionSendMessage transactionSendMessage = new TransactionSendMessage(
                                account.getClient().getClientId(),
                                account.getAccountId(),
                                transaction.getTransactionId(),
                                transactionAsseptedMessage.getAmount(),
                                account.getBalance()
                        );
                        kafkaTemplate.send("t1_demo_transaction_accept", transactionSendMessage);
                    }
                }
                else{

                    account.setStatus(Account.AccountStatus.BLOCKED);
                    user.setStatusUser(User.StatusUser.BLOCKED);
                    Transaction transaction = new Transaction();
                    transaction.setStatus(Transaction.TransactionStatus.REJECTED);
                    transaction.setAccount(account);

                    accountRepository.save(account);
                    userRepository.save(user);
                    transactionRepository.save(transaction);
                }


            }
        }

    }
    @DataSourceErrorLogAnnotation
    @KafkaListener(topics = "t1_demo_transaction_result",  groupId = "common-service-group",containerFactory = "transactionResultKafkaListenerContainerFactory")
    public void listen(TransactionResultMessage transactionResultMessage) {

        try {
            Transaction transaction = transactionRepository.findByTransactionId(transactionResultMessage.getTransactionId());
            Account account = accountRepository.findByAccountId(transactionResultMessage.getAccountId());


            switch (transactionResultMessage.getStatus()) {
                case ACCEPTED -> {
                    transaction.setStatus(Transaction.TransactionStatus.ACCEPTED);
                    Long newBalance = account.getBalance() - transaction.getAmount();
                    Long newFrozenAmount = account.getFrozenAmount() - transaction.getAmount();

                    account.setBalance(newBalance);
                    account.setFrozenAmount(newFrozenAmount);

                    try {
                        accountRepository.save(account);
                        transactionRepository.save(transaction);
                    }
                    catch (DataAccessException e){
                        logger.error(e.getMessage());
                    }

                }
                case BLOCKED -> {
                    account.setStatus(Account.AccountStatus.BLOCKED);
                    transaction.setStatus(Transaction.TransactionStatus.BLOCKED);

                    Long newFrozenAmount = account.getFrozenAmount() - transaction.getAmount();
                    account.setFrozenAmount(newFrozenAmount);

                    try {
                        accountRepository.save(account);
                        transactionRepository.save(transaction);
                    }
                    catch (DataAccessException e){
                        logger.error(e.getMessage());
                    }
                }
                case REJECTED  -> {
                    transaction.setStatus(Transaction.TransactionStatus.REJECTED);
                    Long newFrozenAmount = account.getFrozenAmount() - transaction.getAmount();
                    account.setBalance(account.getBalance() + transaction.getAmount());
                    account.setFrozenAmount(newFrozenAmount);

                    try {
                        accountRepository.save(account);
                        transactionRepository.save(transaction);
                    }
                    catch (DataAccessException e){
                        logger.error(e.getMessage());
                    }

                    Long limitRejectedTransaction = countTransaction(transaction.getTransactionId(),account.getAccountId());
                    if (limitRejectedTransaction > rejectionLimit) {
                        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
                        account.setStatus(Account.AccountStatus.ARRESTED);
                        try {
                            accountRepository.save(account);
                            transactionRepository.save(transaction);
                        }
                        catch (DataAccessException e){
                            logger.error(e.getMessage());
                        }
                    }
                }
                default -> throw new IllegalArgumentException(
                        "Invalid transaction status: " + transactionResultMessage.getStatus()
                );
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    public Transaction createTransaction(Account account, TransactionAsseptedMessage transactionAsseptedMessage){
        Transaction transaction = new Transaction();
        transaction.setStatus(Transaction.TransactionStatus.REQUESTED);
        transaction.setAccount(account);
        transaction.setAmount(transactionAsseptedMessage.getAmount());

        account.setFrozenAmount(transactionAsseptedMessage.getAmount() + account.getFrozenAmount());
        try {
            transactionRepository.save(transaction);
            accountRepository.save(account);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
        }
        return transaction;
    }
    private Long countTransaction(Long clientId, Long accountId){
        return transactionRepository.countByClientIdAndAccountIdAndStatus(
                clientId, accountId, Transaction.TransactionStatus.REJECTED);
    }
}
