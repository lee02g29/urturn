package com.ssafy.urturn.solving.socket;

import com.ssafy.urturn.global.exception.RestApiException;
import com.ssafy.urturn.global.exception.errorcode.CustomErrorCode;
import com.ssafy.urturn.history.HistoryResult;
import com.ssafy.urturn.history.service.HistoryService;
import com.ssafy.urturn.member.service.MemberService;
import com.ssafy.urturn.problem.dto.ProblemTestcaseDto;
import com.ssafy.urturn.problem.service.ProblemService;
import com.ssafy.urturn.solving.cache.CacheDatas;
import com.ssafy.urturn.solving.dto.*;
import com.ssafy.urturn.solving.service.RoomService;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RoomService roomService;
    private final CacheDatas cachedatas;
    private final HistoryService historyService;
    private final MemberService memberService;
    private final ProblemService problemService;
    // 사용자가 데이터를 app/hello 경로로 데이터 날림.
    // 클라이언트는 /topic/greetings 주제를 구독하고 서버에서 이 주제로 메시지가 발행되면 이를 수신.
    private final int ROUND_LIMIT = 20;


    @MessageMapping("/createRoom")
    public void createRoom(@Payload MemberIdDto memberIddto) {
//        cachedatas.clearAllCache();
        log.info("방 생성 로직 멤버ID Dto = {}",memberIddto);
        Long userId=memberIddto.getMemberId();
        RoomInfoResponse roomInfoResponse = roomService.createRoom(userId);
        UserInfoResponse userInfoResponse = roomService.getUserInfo(userId,null);
//        // response에 포함된 방 정보를 이용하여 방을 생성한 사용자에게만 응답을 보냄
        log.info("roomInfoResponse : {}", roomInfoResponse);
        log.info("userInfoResponse : {}", userInfoResponse);
        simpMessagingTemplate.convertAndSendToUser(userId.toString(), "/roomInfo", roomInfoResponse);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored){
        }

        simpMessagingTemplate.convertAndSendToUser(userId.toString(),"/userInfo",userInfoResponse);
    }


    @MessageMapping("/enterRoom")
    public void enterRoom(@Payload RoomIdDto roomIddto) {
        log.info("방 입장 RoomID Dto = {}",roomIddto);

        String roomId=roomIddto.getRoomId();
        Long pairId=cachedatas.cacheroomInfoDto(roomId).getPairId();
        Long managerId=cachedatas.cacheroomInfoDto(roomId).getManagerId();
        UserInfoResponse userInfoResponse=roomService.getUserInfo(pairId,managerId);

        RoomInfoResponse roomInfoResponse=new RoomInfoResponse(roomId,null, false);

        simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/roomInfo", roomInfoResponse);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored){
        }

        simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/userInfo",userInfoResponse);
        UserInfoResponse userInfoResponse2=roomService.getUserInfo(managerId,pairId);
        simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/userInfo",userInfoResponse2);

        log.info("roomInfoResponse : {}", roomInfoResponse);
        log.info("userInfoResponse : {}", userInfoResponse);
        log.info("userInfoResponse : {}", userInfoResponse2);
    }

    @MessageMapping("/leaveRoom")
    public void leaveRoom(@Payload LeaveRoomDto leaveRoomDto) {

        Long pairId=cachedatas.cacheroomInfoDto(leaveRoomDto.getRoomId()).getPairId();
        Long managerId=cachedatas.cacheroomInfoDto(leaveRoomDto.getRoomId()).getManagerId();

        roomService.leaveRoom(leaveRoomDto);

        if(leaveRoomDto.isHost()){
            simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/disconnectWebSocket",true);
            simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/disconnectWebSocket",true);
            return;
        }
        simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/disconnectWebSocket",true);

    }

    @MessageMapping("/sendOVSession")
    public void sendOVSSession(@Payload SessionIdDto sessionIdDto){
        System.out.println(sessionIdDto.toString());
        Long pairId=cachedatas.cacheroomInfoDto(sessionIdDto.getRoomId()).getPairId();
        log.info("receiveOVSession : {}", sessionIdDto.getSessionId());
        simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/receiveOVSession",sessionIdDto.getSessionId());
    }

    @MessageMapping("/selectLevel")
    public void readContext(@Payload SelectLevelRequest SelectLevelRequest){
        log.info("난이도 선택 및 시작 로직 SelectLevel Dto = {}",SelectLevelRequest);
        // 두 유저ID 추출
        Long pairId=cachedatas.cacheroomInfoDto(SelectLevelRequest.getRoomId()).getPairId();
        Long managerId=cachedatas.cacheroomInfoDto(SelectLevelRequest.getRoomId()).getManagerId();

        List<ProblemTestcaseDto> problemTestcases =roomService.getproblem(SelectLevelRequest.getRoomId(), SelectLevelRequest.getLevel());

        log.info("problemTestcases : {}", problemTestcases);

        simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/questionInfo",problemTestcases);
        simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/questionInfo",problemTestcases);

    }

    @MessageMapping("/readyToSolve")
    public synchronized void readyToSolve(@Payload ReadyInfoRequest readyInfoRequest) {
        log.info("문제 풀이 시작 로직 readyInfo Dto = {}",readyInfoRequest);

        Long pairId=cachedatas.cacheroomInfoDto(readyInfoRequest.getRoomId()).getPairId();
        Long managerId=cachedatas.cacheroomInfoDto(readyInfoRequest.getRoomId()).getManagerId();

        if(roomService.setReadyInRoomInfo(readyInfoRequest)){
            /*
             여기 History Table에 데이터 삽입 로직 구현 (삽입 데이터 : 아이디. 방장, 팀원, 첫번째문제, 두번째문제.)
             서비스 단 호출해서 구현 시 RoomService에 메서드 만들어서 호출해 주세용 -> history service에 일단 구현했습니당
             */
            // 여기 방장, 팀원, 문제1, 문제2 데이터 저장되어 있음.
            RoomInfoDto roomInfoDto=cachedatas.cacheroomInfoDto(readyInfoRequest.getRoomId());

            // 구현 완료
            historyService.createHistory(roomInfoDto);
            cachedatas.cacheroomInfoDto(readyInfoRequest.getRoomId(), roomInfoDto);

            simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/startToSolve",true);
            simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/startToSolve",true);
        }
    }

    @MessageMapping("/switchCode")
    public synchronized void switchCode(@Payload SwitchCodeRequest switchCodeRequest){
        log.info("코드 스위치 로직 SwitchCode Dto = {}",switchCodeRequest);
        // 코드 스냅샷 저장.
        roomService.updateCodeCache(switchCodeRequest.getRoomId(),switchCodeRequest.getProblemId().toString(),
                new UserCodeDto(switchCodeRequest.getRound(),switchCodeRequest.getCode(), switchCodeRequest.getLanguage()));

        Long pairId=cachedatas.cacheroomInfoDto(switchCodeRequest.getRoomId()).getPairId();
        Long managerId=cachedatas.cacheroomInfoDto(switchCodeRequest.getRoomId()).getManagerId();

        // 라운드가 최대라운드인 경우
        if(switchCodeRequest.getRound()==ROUND_LIMIT){
            // DB에 기록 저장
            roomService.updateHistory(switchCodeRequest.getRoomId(), HistoryResult.FAILURE, ROUND_LIMIT);
            // 회고창으로 전환
            showRetroCode(switchCodeRequest.getRoomId(),pairId,managerId);
            return;
        }

        // 페어프로그래밍 모드, 드라이버에게만 메시지 받음.
        if(switchCodeRequest.isPair()){
            // 역할 전환 메시지.
            log.info("switch code request NEXT round : {}", switchCodeRequest.getRound()+1);
            simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/switchRole",switchCodeRequest.getRound()+1);
            simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/switchRole",switchCodeRequest.getRound()+1);
            return;
        }

        // 스위칭 모드
        // 준비완료 업데이트
        roomService.updateReadyStatus(switchCodeRequest.getRoomId(), cachedatas.cacheroomInfoDto(switchCodeRequest.getRoomId()), switchCodeRequest.isHost());

        // 둘다 준비상태 확인
        if(roomService.areBothpairsReady(switchCodeRequest.getRoomId(), cachedatas.cacheroomInfoDto(switchCodeRequest.getRoomId()))){
            if(switchCodeRequest.isHost()){
                simpMessagingTemplate.convertAndSendToUser(managerId.toString(),"/switchCode",roomService.getPairsCode(switchCodeRequest));
                simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/switchCode",new SwitchCodeResponse(switchCodeRequest.getCode(),switchCodeRequest.getRound()+1, switchCodeRequest.getLanguage()));
            }else{
                simpMessagingTemplate.convertAndSendToUser(pairId.toString(),"/switchCode",roomService.getPairsCode(switchCodeRequest));
                simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/switchCode",new SwitchCodeResponse(switchCodeRequest.getCode(),switchCodeRequest.getRound()+1, switchCodeRequest.getLanguage()));
            }
        }

    }

    @MessageMapping("/submitCode")
    public void submitCode(@Payload SubmitRequest submitRequest) {
        RoomInfoDto roomInfoDto = cachedatas.cacheroomInfoDto(submitRequest.getRoomId());
        log.info("채점 로직 Dto = {}", submitRequest);

        Long pairId = roomInfoDto.getPairId();
        log.info("pairId = {}", pairId);

        Long managerId = roomInfoDto.getManagerId();
        log.info("managerID = {}", managerId);

        duplicateSubmissionValidation(submitRequest, roomInfoDto);

        try {
            SubmitResponse submitResponse = roomService.submitCode(submitRequest);

            // 오답인 경우
            if (!submitResponse.isResult()) {
                if (submitRequest.isHost()) {
                    simpMessagingTemplate.convertAndSendToUser(managerId.toString(),
                        "/submit/result", submitResponse);
                } else {
                    simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/submit/result",
                        submitResponse);
                }
                log.info("submitResponse : {}", submitResponse);
                return;
            }

            // 정답인 경우.
            // 멤버당 푼 문제에 대한 기록 저장
            problemService.saveMemberProblem(managerId, pairId, submitRequest.getProblemId());
            // 호스트 인 경우.
            if (submitRequest.isHost()) {

                // 사용자에게 정답 응답 및 메시지 전송.
                simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/submit/result", submitResponse);

                // 페어프로그래밍 모드 인 경우.
                if (submitRequest.isPair()) {
                    // DB 저장 로직.
                    roomService.updateHistory(submitRequest.getRoomId(), HistoryResult.SUCCESS,
                        submitRequest.getRound());

                    showRetroCode(submitRequest.getRoomId(), pairId, managerId);
                    return;
                }

                // 일단은 역할을 data를 String(""Navigator")으로 넘기지만, Enum, dto형태든 원하는 형태로 수정 가능.
                simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/role", "Navigator");
                simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/role", "Driver");

            } else {
                simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/submit/result", submitResponse);

                if (submitRequest.isPair()) {
                    // DB 저장 로직.
                    roomService.updateHistory(submitRequest.getRoomId(), HistoryResult.SUCCESS,
                        submitRequest.getRound());
                    showRetroCode(submitRequest.getRoomId(), pairId, managerId);
                    return;
                }

                simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/role", "Navigator");
                simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/role", "Driver");
            }
        } finally {
            resetValidation(submitRequest, roomInfoDto);
        }
    }

    private void duplicateSubmissionValidation(SubmitRequest submitRequest, RoomInfoDto roomInfoDto) {
        if (submitRequest.isHost()) {
            if (roomInfoDto.isManagerIsSubmitting())
                throw new RestApiException(CustomErrorCode.REQUEST_LOCKED);
            roomInfoDto.setManagerIsReady(true);
        } else {
            if (roomInfoDto.isPairIsSubmitting())
                throw new RestApiException(CustomErrorCode.REQUEST_LOCKED);
            roomInfoDto.setPairIsReady(true);
        }
        cachedatas.cacheroomInfoDto(submitRequest.getRoomId(), roomInfoDto);
    }

    private void resetValidation(SubmitRequest submitRequest, RoomInfoDto roomInfoDto) {
        if (submitRequest.isHost()) {
            roomInfoDto.setManagerIsReady(false);
        } else {
            roomInfoDto.setPairIsReady(false);
        }
        cachedatas.cacheroomInfoDto(submitRequest.getRoomId(), roomInfoDto);
    }


    private void showRetroCode(String roomId, Long pairId, Long managerId) {
        // 회고창에서 보여줄 데이터
        Map<Long, RetroCodeResponse> map = roomService.makeRetroCodeResponse(roomId);
        simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/showRetroCode", map);
        simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/showRetroCode", map);
    }

    @MessageMapping("/submitRetro")
    public void submitRetro(@Payload RetroCreateRequest req){
            RoomInfoDto roomInfo=cachedatas.cacheroomInfoDto(req.getRoomId());

            // history에 retro update
            roomService.updateRetro(req, roomInfo.getHistoryId());

            // 웹소켓 끊기 요청
            // 추가로 각 멤버를 확인해서 github repository가 null이 아니면 github access token refresh 요청
            Long managerId=cachedatas.cacheroomInfoDto(req.getRoomId()).getManagerId();
            simpMessagingTemplate.convertAndSendToUser(managerId.toString(), "/finishSocket/githubUpload", memberService.hasGithubRepository(managerId));

            Long pairId=cachedatas.cacheroomInfoDto(req.getRoomId()).getPairId();
            simpMessagingTemplate.convertAndSendToUser(pairId.toString(), "/finishSocket/githubUpload", memberService.hasGithubRepository(pairId));

        //  캐시 삭제.


    }

}
/*
convertAndSendToUser(String user, String destination, Object payload)
user : 메시지를 받을 사용자의 식별자. 고유ID
destination : 메시지를 보낼 목적지(경로) -> 클라이언트가 메시지를 수신하기 위해 구독하는 경로.
payload 전송할 메시지의 내용

목적지 /queue/roomCreated 는  /user/{userId}/queue/roomCreated로 변환 됨.
이때 /user라는 접두사는 Spring 메시지 브로커가 메시지를 해당 사용자의 세션에 자동으로 라우팅하는데 사용 표준 접두사
{userId} 사용자를 식별하는 고유 키 인증 과정에서 얻어지며, Principal 객체의 이름 또는 특정 필드로부터 추출.

자동 처리: 사용자가 직접 /user/{userId}/queue/... 형태의 경로를 구독할 필요는 없습니다.
클라이언트는 보다 일반적인 경로(예: /queue/roomCreated)를 구독하고,
Spring 웹소켓 인프라가 자동으로 /user/{userId}/queue/roomCreated 형태로 변환하여 해당 사용자의 메시지만을 받도록 합니다.

사용자 ID 전달: 일반적으로 userId는 서버가 사용자의 인증 정보에서 자동으로 추출합니다.

response는 서버에서 내부 로직 처리 후 클라이언트에게 보낼 객체.
 */