package com.example.AopExample.controller;

import com.example.AopExample.entity.Users;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AopController {

    @GetMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
    public Users sayHello(@RequestBody Users users){
        return users;
    }
}
