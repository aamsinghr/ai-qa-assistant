package com.aamsinghr.ai_qa_assistant.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

@Configuration
public class BedrockConfig {

    @Value("${aws.bedrock.region}")
    private String region;

    @Value("${aws.bedrock.timeout:30}")
    private int timeoutSeconds;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(timeoutSeconds))
                .apiCallAttemptTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(overrideConfig)
                .build();
    }
}
