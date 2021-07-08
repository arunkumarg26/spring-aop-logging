package com.example.AopExample.config;

import com.example.AopExample.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;

@Configuration
public class LoggingConfig {
    @Value("${spring.profiles.active}")
    private String activeProfile;
    @Value("${pii.masking.profiles}")
    private Set<String> dataMaskingProfiles;
    @Value("${pii.masking.properties}")
    private Set<String> propertiesToFilter;


    @Bean
    public ObjectWriter aspectObjectWriter() {
        if (dataMaskingProfiles.contains(activeProfile)) {
            FilterProvider filter = new SimpleFilterProvider()
                    .addFilter("filter properties by field", SimpleBeanPropertyFilter.serializeAllExcept(propertiesToFilter))
                    .addFilter("filter properties by class", SimpleBeanPropertyFilter.filterOutAllExcept());
            return new ObjectMapper()
                    .addMixIn(Users.class, FieldMixIn.class)
                    .writer(filter);
        }
        //Return full writer if we aren't on PROD or UAT
        return new ObjectMapper().writer();
    }
}
