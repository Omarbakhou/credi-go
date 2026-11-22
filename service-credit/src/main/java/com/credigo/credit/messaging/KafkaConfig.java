package com.credigo.credit.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Topics Kafka (créés au démarrage s'ils n'existent pas) et gestion des erreurs de consommation :
 * 3 essais espacés d'1 s, puis le message est publié dans le topic {@code <topic>.DLT} (lettre morte).
 * Un message illisible part directement en .DLT (le rejouer ne servirait à rien).
 */
@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    @Bean
    public NewTopic topicApplicationDeposited() {
        return TopicBuilder.name(Topics.APPLICATION_DEPOSITED).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic topicClientScored() {
        return TopicBuilder.name(Topics.CLIENT_SCORED).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic topicLoanApproved() {
        return TopicBuilder.name(Topics.LOAN_APPROVED).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic topicDisbursementPaid() {
        return TopicBuilder.name(Topics.DISBURSEMENT_PAID).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic topicDisbursementFailed() {
        return TopicBuilder.name(Topics.DISBURSEMENT_FAILED).partitions(PARTITIONS).replicas(1).build();
    }

    /** Topic compacté : Kafka conserve au moins le dernier événement de chaque client (clé = clientId). */
    @Bean
    public NewTopic topicClientEvents() {
        return TopicBuilder.name(Topics.CLIENT_EVENTS).partitions(PARTITIONS).replicas(1).compact().build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer lettreMorte = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        DefaultErrorHandler handler = new DefaultErrorHandler(lettreMorte, new FixedBackOff(1000L, 2L));
        handler.addNotRetryableExceptions(EvenementIllisibleException.class);
        return handler;
    }
}
