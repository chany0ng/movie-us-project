package com.teamproject.teamthree.controller;

import com.teamproject.teamthree.dto.UserReqDTO;
import com.teamproject.teamthree.dto.UserReqFormDTO;
import com.teamproject.teamthree.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class UserRestController {
    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/index")
    public ResponseEntity<Map<String, String>> index() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, String> response = new HashMap<>();
        response.put("userName", userName);
        return ResponseEntity.ok(response); // userName을 JSON 응답으로 반환
    }

    // 로그인 요청 처리
    @GetMapping("/login")
    public ResponseEntity<String> login() {
        return ResponseEntity.ok("로그인 페이지"); // 로그인 페이지 접근 가능 메시지
    }

    @PostMapping("/login")
    public String login(@RequestParam String useremail, @RequestParam String password, Model model) {
        // 로그인 처리 로직
        return "redirect:/movies/index"; // 로그인 후 리다이렉트할 페이지
    }


    // 로그인 폼
    @GetMapping("/loginform")
    public ResponseEntity<Map<String, String>> loginForm(@RequestParam(value = "error", required = false) String error,
                                                         @RequestParam(value = "signupSuccess", required = false) String signupSuccess) {
        Map<String, String> response = new HashMap<>();

        if (error != null) {
            response.put("loginError", "아이디 또는 비밀번호가 잘못되었습니다.");
        }

        // 회원가입 성공 메시지 확인
        if (signupSuccess != null) {
            response.put("signupSuccess", signupSuccess);
        }

        response.put("userName", SecurityContextHolder.getContext().getAuthentication().getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/signform")
    public ResponseEntity<UserReqFormDTO> signUpForm() {
        return ResponseEntity.ok(new UserReqFormDTO()); // 빈 UserReqFormDTO를 반환
    }

    @PostMapping("/signup")
    public ResponseEntity<String> addUser(@Valid @RequestBody UserReqDTO userReqDTO) {
        userService.addUser(userReqDTO);
        return ResponseEntity.ok("회원가입이 완료되었습니다. 로그인 해주세요."); // 성공 메시지 반환
    }

    @GetMapping("/mypage")
    public ResponseEntity<Map<String, String>> myPage(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("userName", authentication.getName());
        return ResponseEntity.ok(response);
    }
}

