package com.example.clip.auth.service;

import com.example.clip.auth.domain.User;
import com.example.clip.auth.domain.UserRole;
import com.example.clip.auth.dto.SignUpRequestDto;
import com.example.clip.auth.repository.UserRepository;
import com.example.clip.gap.repository.GapResultRepository;
import com.example.clip.paper.repository.GapBookmarkRepository;
import com.example.clip.paper.repository.PaperBookmarkRepository;
import com.example.clip.search.domain.Roadmap;
import com.example.clip.search.repository.RoadmapRepository;
import com.example.clip.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SearchHistoryRepository searchHistoryRepository;
    private final RoadmapRepository roadmapRepository;
    private final GapResultRepository gapResultRepository;
    private final PaperBookmarkRepository paperBookmarkRepository;
    private final GapBookmarkRepository gapBookmarkRepository;

    @Transactional
    public void registerUser(SignUpRequestDto signUpRequestDto) {
        if (userRepository.existsByUserId(signUpRequestDto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (userRepository.existsByNickname(signUpRequestDto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        if (userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User(
                signUpRequestDto.getUserId(),
                signUpRequestDto.getNickname(),
                passwordEncoder.encode(signUpRequestDto.getPassword()),
                signUpRequestDto.getEmail(),
                UserRole.ROLE_USER
        );

        userRepository.save(user);
    }

    // 회원 탈퇴 — 유저의 연관 데이터를 먼저 삭제한 뒤 유저 삭제.
    // FK 제약(SearchHistory.user, Roadmap.user)을 피하려면 자식 데이터부터 지워야 함.
    @Transactional
    public void deleteAccount(User user) {
        String userId = user.getUserId();

        // 1. 북마크 (userId 문자열 기준)
        paperBookmarkRepository.deleteByUserId(userId);
        gapBookmarkRepository.deleteByUserId(userId);

        // 2. 검색 기록 (User FK)
        searchHistoryRepository.deleteByUser(user);

        // 3. 유저의 로드맵과 그에 딸린 분석 결과(Insight)
        for (Roadmap roadmap : roadmapRepository.findByUser(user)) {
            gapResultRepository.deleteByRoadmapId(roadmap.getRoadmapId());
        }
        roadmapRepository.deleteByUser(user);

        // 4. 유저
        userRepository.delete(user);
    }
}