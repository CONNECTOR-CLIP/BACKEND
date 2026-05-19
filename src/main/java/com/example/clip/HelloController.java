package com.example.clip;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/test")
    public String test() {
        return "스프링 부트 서버가 정상적으로 연결되었습니다!";
    }
}