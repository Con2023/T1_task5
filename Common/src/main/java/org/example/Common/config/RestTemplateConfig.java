package org.example.Common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

//Это класс из Spring Framework, который:
//Выполняет HTTP-запросы (GET, POST, PUT, DELETE и др.)
//Обрабатывает ответы
//Сериализует/десериализует JSON/XML в объекты Java
//Работает через простое API в стиле Spring


// попробовать WebClient

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
