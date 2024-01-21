package com.traders.tradersback.controller;



import com.traders.tradersback.dto.AuthResponse;
import com.traders.tradersback.dto.MemberLoginDto;
import com.traders.tradersback.dto.PasswordResetDto;
import com.traders.tradersback.dto.PasswordResetRequestDTO;
import com.traders.tradersback.model.Member;
import com.traders.tradersback.service.EmailService;
import com.traders.tradersback.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Clock;


@RestController
@RequestMapping("/api/members")
public class MemberController {
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);
    @Autowired
    private MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            Member registeredMember = memberService.register(member);
            return ResponseEntity.ok(registeredMember);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginDto memberLoginDto, HttpServletResponse response) {
        try {
            AuthResponse authResponse = memberService.login(memberLoginDto.getMemberId(), memberLoginDto.getMemberPassword());
            String token = authResponse.getAccessToken(); // getToken 대신 getAccessToken 사용
            // JWT 토큰을 쿠키에 저장
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setHttpOnly(true); // HttpOnly 설정
            cookie.setSecure(true); // HTTPS 환경에서만 쿠키 전송
            cookie.setPath("/"); // 쿠키의 경로 설정
            response.addCookie(cookie);

            return ResponseEntity.ok(authResponse);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // JWT 토큰 무효화 로직 (예: 토큰 블랙리스트 처리)
        // 쿠키 삭제
        Cookie cookie = new Cookie("jwtToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키를 즉시 만료시킴
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            AuthResponse response = memberService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findMemberId(@RequestParam String memberEmail) {
        try {
            String memberId = memberService.findMemberIdByEmail(memberEmail);
            if (memberId != null) {
                return ResponseEntity.ok(memberId);
            } else {
                return ResponseEntity.badRequest().body("No member found with the given email.");
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDTO requestDto) {
        try {
            emailService.sendPasswordResetEmail(requestDto.getEmail());
            return ResponseEntity.ok("비밀번호 재설정 이메일을 발송했습니다.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호 재설정 이메일 발송 중 오류가 발생했습니다: " + ex.getMessage());
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto resetDto) {
        try {
            logger.info("Reset Password request received for token: {}", resetDto.getToken());
            memberService.resetPassword(resetDto.getToken(), resetDto.getNewPassword());
            logger.info("Password reset successful for token: {}", resetDto.getNewPassword());
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception ex) {
            logger.error("Error during password reset for token: {}", resetDto.getToken(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호 변경 중 오류가 발생했습니다: " + ex.getMessage());
        }
    }

}
