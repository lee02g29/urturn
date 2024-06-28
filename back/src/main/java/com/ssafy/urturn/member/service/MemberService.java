package com.ssafy.urturn.member.service;

import com.ssafy.urturn.global.exception.RestApiException;
import com.ssafy.urturn.global.exception.errorcode.CustomErrorCode;
import com.ssafy.urturn.global.util.MemberUtil;
import com.ssafy.urturn.member.dto.MemberDetailResponse;
import com.ssafy.urturn.member.entity.Member;
import com.ssafy.urturn.member.repository.MemberRepository;
import com.ssafy.urturn.solving.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMemberById(Long id){
        return memberRepository.findById(id).orElseThrow(() ->  new RestApiException(
            CustomErrorCode.NO_MEMBER));
    }

    public MemberDetailResponse getCurrentMember() {
        Long currentMemberId = MemberUtil.getMemberId();
        Member currentMember = memberRepository.findById(currentMemberId).orElseThrow(() ->  new RestApiException(
            CustomErrorCode.NO_MEMBER));
        return MemberDetailResponse.makeResponse(currentMember);
    }

    @Transactional
    public void updateGithubRepository(String repository) {
        Long currentMemberId = MemberUtil.getMemberId();
        Member currentMember = memberRepository.findById(currentMemberId).orElseThrow(() ->  new RestApiException(
            CustomErrorCode.NO_MEMBER));
        currentMember.updateGithubRepository(repository);
    }

//    public userInfoResponse getMemberInfo(Long myUserId, Long pairId) {
//        userInfoResponse userInfo = new userInfoResponse();
//        if(myUserId!=null) {
//            Member member = memberRepository.findById(myUserId).orElseThrow(() -> new IllegalArgumentException("Invalid member ID"));
//            userInfo.setMyUserProfileUrl(member.getProfileImage());
//            userInfo.setMyUserNickName(member.getNickname());
//        }
//
//        if(pairId!=null) {
//            Member member = memberRepository.findById(pairId).orElseThrow(() -> new IllegalArgumentException("Invalid member ID"));
//            userInfo.setpairProfileUrl(member.getProfileImage());
//            userInfo.setpairNickName(member.getNickname());
//        }
//
//        return userInfo;
//    }

    public UserInfoResponse getMemberInfo(Long myUserId, Long pairId) {
        UserInfoResponse userInfo = new UserInfoResponse();

        // 사용자 정보 설정
        setUserInfo(userInfo, myUserId, true);
        setUserInfo(userInfo, pairId, false);

        return userInfo;
    }

    public boolean hasGithubRepository(Long memberId){
        return memberRepository.findById(memberId).orElseThrow(() ->  new RestApiException(
            CustomErrorCode.NO_MEMBER)).getRepository() != null;
    }

    // 사용자 정보를 설정하는 보조 메서드
    private void setUserInfo(UserInfoResponse userInfo, Long userId, boolean isMyUser) {
        if (userId != null) {
            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("부적절한 유저 아이디 : " + userId));
            if (isMyUser) {
                userInfo.setMyUserProfileUrl(member.getProfileImage());
                userInfo.setMyUserNickName(member.getNickname());
            } else {
                userInfo.setPairProfileUrl(member.getProfileImage());
                userInfo.setPairNickName(member.getNickname());
            }
        }
    }


}
