package com.example.clip;

// 서버 연결 확인용 테스트 컨트롤러
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // GET /test — 서버 기동 여부 확인용 헬스체크
    @GetMapping("/test")
    public String test() {
        return "스프링 부트 서버가 정상적으로 연결되었습니다!";
    }
}