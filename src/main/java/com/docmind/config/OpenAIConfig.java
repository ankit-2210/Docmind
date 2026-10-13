package com.docmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocMind - AI Document Intelligence & RAG backend")
                        .description("Rest API for DocMing: Multi-format document ingestion, vector embedding with PostgresSql pgvector")

                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Substring Technologies")
                                .email("support@substringtechnologies.com")
                                .url("https://substringtechnologies.com")
                        )
                );

    }

}
