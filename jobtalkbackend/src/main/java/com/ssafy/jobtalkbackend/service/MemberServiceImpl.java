package com.ssafy.jobtalkbackend.service;

import com.ssafy.jobtalkbackend.domain.*;
import com.ssafy.jobtalkbackend.dto.request.LoginRequest;
import com.ssafy.jobtalkbackend.dto.request.SignUpRequest;
import com.ssafy.jobtalkbackend.dto.response.TokenResponse;
import com.ssafy.jobtalkbackend.exception.enterprise.EnterpriseExceptionEnum;
import com.ssafy.jobtalkbackend.exception.enterprise.EnterpriseRuntimeException;
import com.ssafy.jobtalkbackend.jwt.JwtTokenProvider;
import com.ssafy.jobtalkbackend.repository.*;
import com.ssafy.jobtalkbackend.exception.member.MemberExceptionEnum;
import com.ssafy.jobtalkbackend.exception.member.MemberRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final NewsLikeRepository newsLikeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final NewsRepository newsRepository;
    private final PassReviewRepository passReviewRepository;
    private final PassReviewLikeRepository passReviewLikeRepository;

    @Override
    public Member searchMember(String email) {
        Member member = memberRepository.findByEmail(String.valueOf(email))
                .orElseThrow(()-> new MemberRuntimeException(MemberExceptionEnum.MEMBER_NOT_EXIST_EXCEPTION));
        return member;
    }

    @Transactional
    @Override
    public Boolean signUp(SignUpRequest request){
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_EXIST_EMAIL_EXCEPTION);
        }

        if (memberRepository.findByNickname(request.getNickname()).isPresent()) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_EXIST_NICKNAME_EXCEPTION);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.ROLE_USER)
                .build();

        memberRepository.save(member);

        return true;
    }

    @Override
    @Transactional
    public ResponseEntity<TokenResponse> login(LoginRequest request, boolean kakaoLogin) {

        Member member = searchMember(request.getEmail());

        if (kakaoLogin == false && member.getOauthId() != null) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_NEED_KAKAO_LOGIN);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_PASSWORD_EXCEPTION);
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        TokenResponse tokenResponse = jwtTokenProvider.createToken(authentication);

        return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
    }

    @Override
    public Boolean checkEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_EXIST_EMAIL_EXCEPTION);
        }
        return true;
    }

    @Override
    public Boolean checkNickname(String nickname) {
        if (memberRepository.findByNickname(nickname).isPresent()) {
            throw new MemberRuntimeException(MemberExceptionEnum.MEMBER_EXIST_NICKNAME_EXCEPTION);
        }
        return true;
    }

    @Transactional
    @Override
    public String modifyNickname(String nickname, User user) {
        Member member = searchMember(user.getUsername());
        checkNickname(nickname);
        member.modifyNickname(nickname);
        return nickname;
    }

    @Transactional
    @Override
    public Boolean scrapNews(Long newsId, User user) {
        Member member = searchMember(user.getUsername());
        News news = newsRepository.findById(newsId)
                .orElseThrow(()-> new EnterpriseRuntimeException(EnterpriseExceptionEnum.ENTERPRISE_NEWS_NOT_EXIST_EXCEPTION));

        NewsLike newsLike = NewsLike
                .builder()
                .member(member)
                .news(news)
                .build();

        newsLikeRepository.save(newsLike);
        return true;
    }

    @Transactional
    @Override
    public Boolean scrapPassReview(Long passReviewId, User user) {
        Member member = searchMember(user.getUsername());
        PassReview passReview = passReviewRepository.findById(passReviewId)
                .orElseThrow(()-> new EnterpriseRuntimeException(EnterpriseExceptionEnum.ENTERPRISE_PASSREVIEW_NOT_EXIST_EXCEPTION));

        PassReviewLike passReviewLike = PassReviewLike
                .builder()
                .member(member)
                .passReview(passReview)
                .build();
        passReviewLikeRepository.save(passReviewLike);
        return true;
    }


}
