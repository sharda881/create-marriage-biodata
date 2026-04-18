package com.biodatamaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application class for Marriage Bio-Data Maker.
 *
 * This is a production-ready, monolithic application that combines:
 * - Spring Boot 3.x backend
 * - Thymeleaf + Tailwind CSS frontend
 * - Spring Security with OAuth2 and Form Login
 * - JPA with H2 (dev) / PostgreSQL (prod)
 * - OpenHTMLtoPDF for PDF generation
 */
@SpringBootApplication
public class BiodataMakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiodataMakerApplication.class, args);
    }
}
