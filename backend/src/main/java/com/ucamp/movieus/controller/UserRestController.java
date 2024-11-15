package com.ucamp.movieus.controller;

import com.ucamp.movieus.dto.UserLoginDto;
import com.ucamp.movieus.dto.UserMyPageDTO;
import com.ucamp.movieus.dto.UserReqDTO;
import com.ucamp.movieus.entity.UserEntity;
import com.ucamp.movieus.security.JwtTokenProvider;
import com.ucamp.movieus.service.MailService;
import com.ucamp.movieus.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
//@CrossOrigin(origins = "http://localhost:3000")
public class UserRestController {
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final MailService mailService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/index")
    public ResponseEntity<Map<String, String>> index() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, String> response = new HashMap<>();
        response.put("userName", userName);
        return ResponseEntity.ok(response); // userName을 JSON 응답으로 반환
    }

    // JWT를 사용한 로그인 요청 처리
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserLoginDto userLoginDto) {
        System.out.println(userLoginDto.getUserPw());

        // 사용자 인증 후 JWT 토큰 생성
        String jwtToken = userService.authenticateUser(userLoginDto);

        // JWT에서 사용자 정보 추출

        String email = jwtTokenProvider.getEmailFromJWT(jwtToken); // JWT에서 이메일 추출
        String name = jwtTokenProvider.getNameFromJWT(jwtToken); // JWT에서 이름 추출
        Integer userNum = jwtTokenProvider.getUserNumFromJWT(jwtToken); // JWT에서 넘버 추출

        Map<String, Object> response = new HashMap<>();

        response.put("token", jwtToken);
        response.put("email", email); // 이메일 추가
        response.put("name", name); // 이름 추가
        response.put("userNum", userNum);
        response.put("message", "로그인 성공");
        return ResponseEntity.ok(response); // JWT 토큰과 메시지 반환
    }


    // 카카오 소셜 로그인 처리
    @PostMapping("/social-login")
    public ResponseEntity<Map<String, Object>> handleSocialLogin(@RequestBody Map<String, String> loginRequest) {
        String kakaoEmail = loginRequest.get("kakaoEmail");
        String userName = loginRequest.get("userName");

        // 소셜 로그인 처리
        UserEntity user = userService.handleSocialLogin(kakaoEmail, userName);

        // JWT 토큰 생성
        String jwtToken = jwtTokenProvider.createToken(user.getUserEmail(), user.getUserName(), user.getUserNum());

        // 응답 JSON 구성
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwtToken);
        response.put("email", user.getUserEmail());
        response.put("name", user.getUserName());
        response.put("userNum", user.getUserNum());
        response.put("message", "소셜 로그인 성공");

        return ResponseEntity.ok(response); // JWT 토큰과 사용자 정보 반환
    }


    // 회원가입 요청 처리
    @PostMapping("/signup")
    public ResponseEntity<String> addUser(@Valid @RequestBody UserReqDTO userReqDTO) {
        System.out.println(userReqDTO);
        userService.addUser(userReqDTO);
        return ResponseEntity.ok("회원가입이 완료되었습니다. 로그인 해주세요."); // 성공 메시지 반환
    }


    // 마이페이지 - 인증된 사용자 정보 확인
    @GetMapping("/mypage")
    public ResponseEntity<Map<String, String>> myPage(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("userName", authentication.getName());
        return ResponseEntity.ok(response);
    }

    // 이메일 중복 확인
    @GetMapping("/check-email/{email}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkEmailDuplication(@PathVariable String email) {
        Map<String, Boolean> response = new HashMap<>();
        System.out.println("Email 요청: " + email);
        boolean isDuplicated = userService.isEmailDuplicate(email);
        response.put("isDuplicated", isDuplicated);

//        String resetUrl = "http://localhost:3000/change-pw";
        String resetUrl = "http://localhost:3000/change-pw/" + email; // 비밀번호 재설정 URL
        if(isDuplicated){
            try {
                mailService.sendPasswordResetMail(email, resetUrl);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); //404
    }



    // 비밀번호 재설정 처리
    @PostMapping("/passwordReset")
    public ResponseEntity<String> passwordReset(@RequestBody UserLoginDto userLoginDto) {
        System.out.println("Resetting password for email: " + userLoginDto.getUserEmail());
        userService.passwordReset(userLoginDto.getUserEmail(), userLoginDto.getUserPw());
        return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
    }





        //재하 추가 코드 (회원정보 조회) - userNum 버전
        @GetMapping("/mypage/user/{userNum}")
        public ResponseEntity<UserMyPageDTO> myPageByNum(@PathVariable Integer userNum) {
            UserMyPageDTO userInfo = userService.getUserMyPageByNum(userNum);
            return ResponseEntity.ok(userInfo);
        }

        //재하 추가 코드 (회원정보 수정) - userNum 버전.
        @PutMapping("/mypage/user/{userNum}")
        public ResponseEntity<UserMyPageDTO> updateUserInfoByNum(
            @PathVariable Integer userNum,
            @RequestBody UserMyPageDTO updateDto
        ) {
            UserMyPageDTO updatedUser = userService.updateUserInfoByNum(userNum, updateDto);
            return ResponseEntity.ok(updatedUser);
        }

    //userNum 반환 url
    @GetMapping("/getUserNum")
    public ResponseEntity<Map<String, Object>> getUserNum(@RequestParam String userName) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer userNum = userService.getUserNumByUsername(userName);
            if (userNum != null) {
                response.put("userNum", userNum);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("message", "Error retrieving user number");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}