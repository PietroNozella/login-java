package com.example.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoInitConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        String[] colecoes = {
            "usuarios",
            "password_reset_tokens",
            "auditoria"
        };
        for (String colecao : colecoes) {
            if (!mongoTemplate.collectionExists(colecao)) {
                mongoTemplate.createCollection(colecao);
            }
        }
    }
}
