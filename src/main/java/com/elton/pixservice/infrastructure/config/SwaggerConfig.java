package com.elton.pixservice.infrastructure.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swagger/OpenAPI configuration using SpringFox.
 *
 * Provides automatic API documentation for all REST endpoints.
 *
 * Access the documentation at:
 * - Swagger UI: http://localhost:8080/swagger-ui/
 * - OpenAPI JSON: http://localhost:8080/v2/api-docs
 */
@Configuration
public class SwaggerConfig {

    /**
     * Bean post processor to fix SpringFox compatibility with Spring Boot 2.6+
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.elton.pixservice.infrastructure.web.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Pix Service API")
                .description("API REST para gerenciamento de carteira digital com suporte a transferências Pix\n\n" +
                        "Esta API implementa:\n" +
                        "- Gestão de carteiras digitais\n" +
                        "- Registro e validação de chaves Pix (CPF, EMAIL, PHONE, EVP)\n" +
                        "- Transferências Pix com idempotência\n" +
                        "- Processamento de webhooks (confirmação/rejeição)\n" +
                        "- Controle de concorrência com pessimistic locking\n" +
                        "- Ledger imutável para auditoria\n\n" +
                        "Arquitetura: Clean Architecture (Domain, Use Case, Infrastructure)")
                .version("1.0.0")
                .contact(new Contact(
                        "Elton Pix Service",
                        "https://github.com/elton/pix-service",
                        "elton@example.com"))
                .license("MIT License")
                .licenseUrl("https://opensource.org/licenses/MIT")
                .build();
    }
}
