
import { useNavigate } from "react-router-dom";
import { Client } from "@stomp/stompjs";

import { useAuthStore } from "../stores/useAuthStore";
import { useRoomStore } from "../stores/room";
import { loadMarkdownFromCDN } from "../utils/solve/loadMarkdownFromCDN";
import { questionInfo, reviewInfo } from "../types/roomTypes";
import {useOpenVidu} from "./openVidu.ts";
import {useRtcStore} from "../stores/rtc.ts";
import { convertLangToUpper, convertUppserToLang } from "../utils/solve/convertProgramLang.ts";

const url = import.meta.env.VITE_API_WEBSOCKET_URL
const MAX_TIME = 40;
export const useWebSocket = () => {
    const navi = useNavigate();
    const authStore = useAuthStore();
    const roomStore = useRoomStore();
    const rtcStore = useRtcStore();
    const openVidu = useOpenVidu();
    
    const connect = () => {
        const userId = authStore.memberId;
        const token = authStore.accessToken;

        const setTimer= (maxSec: number) => {
            
            roomStore.setSec(maxSec);
        
            const timer = setInterval(() => {
                if(roomStore.getSec() > 0){
                    roomStore.setSec(roomStore.getSec() - 1);
                }
                else if (roomStore.getSec() <= 0) {
                    if(roomStore.getPairProgramingMode()===false || roomStore.getPairProgramingRole() === 'Driver'){
                        roomStore.getClient()?.publish({
                            destination: '/app/switchCode',
                            body: JSON.stringify({ 
                                code: roomStore.getCode(),
                                roomId: roomStore.getRoomInfo()?.roomId,
                                round: roomStore.getRound(),
                                algoQuestionId: roomStore.getQuestionInfos()?.[roomStore.getQuestionIdx()]?.algoQuestionId,
                                isHost: roomStore.getRoomInfo()?.host,
                                isPair: roomStore.getPairProgramingMode(),
                                language: convertLangToUpper(roomStore.getLang()),
                            })
                        })
                    }
                        clearInterval(timer);
                }
            }, 1000)
        }
        
        const client = new Client({
            brokerURL: url + '/ws',
            
            connectHeaders: {
                Authorization: 'Bearer ' + token,
            },
            
            debug: function () {
            },
            reconnectDelay: 5000, //자동 재 연결
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = function () {
            client.subscribe('/user/' + userId + '/roomInfo', (msg) => {
                const roomInfo = JSON.parse(msg.body);
                roomStore.setRoomInfo(roomInfo);
            });
            client.subscribe('/user/' + userId + '/userInfo', (msg) => {
                const userInfo = JSON.parse(msg.body);
                if(roomStore.getRoomInfo()?.host && userInfo.relativeUserNickName===null){
                    openVidu.masterCreate();
                }
                if(roomStore.getRoomInfo()?.host && userInfo.relativeUserNickName){
                    client.publish({
                        destination: '/app/sendOVSession',
                        body: JSON.stringify({
                            roomId: roomStore.getRoomInfo()?.roomId,
                            sessionId: rtcStore.getSessionId(),
                        })
                    })
                }
                roomStore.setUserInfo(userInfo);
            });
            // const publishTimer = setInterval(() => {
            //     if(subscribeRoomInfo.id && subscribeUserInfo.id){
            //         clearInterval(publishTimer);
            //         if(roomId===null){
            //             client.publish({
            //                 destination: '/app/createRoom',
            //                 body: JSON.stringify({
            //                 memberId : userId,
            //                 }),
            //             });
            //         }
            //         else{
            //             client.publish({
            //                 destination: '/app/enterRoom',
            //                 body: JSON.stringify({
            //                     roomId : roomId,
            //                 }),
            //             });
            //         }
            //     }
            // }, 1000)

            client.subscribe('/user/' + userId + '/receiveOVSession', (msg) => {
                const sessionId = msg.body;
                rtcStore.setSessionId(sessionId);
                openVidu.partnerJoin(sessionId);
            })
            client.subscribe('/user/' + userId + '/questionInfo', (msg) => {
                const questionInfos: questionInfo[] = JSON.parse(msg.body);
                questionInfos.forEach(
                    async (questionInfo: questionInfo) => {
                        const content = await loadMarkdownFromCDN(questionInfo.algoQuestionUrl);
                        questionInfo.algoQuestionContent = content;
                    }
                )
                roomStore.setQuestionInfos(questionInfos);
                navi('/trans/check');
            });
            client.subscribe('/user/' + userId + '/startToSolve', () => {
                const idx = roomStore.getRoomInfo()?.host ? 0 : 1;
                roomStore.setQuestionIdx(idx);
                setTimer(MAX_TIME);
                navi('/trans/solve');
            });
            client.subscribe('/user/' + userId + '/switchCode', (msg) => {
                const data = JSON.parse(msg.body);
                roomStore.setCode(data.code);
                roomStore.setRound(data.round);
                roomStore.setLang(convertUppserToLang(data.language));

                const idx = roomStore.getQuestionIdx() === 0 ? 1 : 0;
                roomStore.setQuestionIdx(idx);
                setTimer(MAX_TIME);
                navi('/trans/solveSwitch');
            });
            client.subscribe('/user/' + userId + '/switchRole', (msg) => {
                const round = Number(msg.body);
                roomStore.setRound(round);
                if(roomStore.getPairProgramingRole() === 'Driver'){
                    roomStore.setPairProgramingRole('Navigator');
                }
                else if(roomStore.getPairProgramingRole() === 'Navigator'){
                    roomStore.setPairProgramingRole('Driver');
                }
                setTimer(MAX_TIME);
                navi('/trans/pairSolveSwitch');
            })
            client.subscribe('/user/' + userId + '/submit/result', (msg) => {
                roomStore.setCurrentlySubmitting(false);
                const data = JSON.parse(msg.body);
                const result = data.result;

                let consoleMsg = '';

                if(result===true){
                    consoleMsg += '정답입니다!\n\n\n';
                }
                else{
                    consoleMsg += '오답입니다\n\n\n';
                }

                const testcaseResults = data.testcaseResults;
                for(let i = 0; i < testcaseResults.length; i++){
                    const t = testcaseResults[i];
                    consoleMsg += (i+1) + '번 테스트케이스 : ' +  t.status + '\n';
                    if(t.stderr!==null){
                        consoleMsg += t.stderr + '\n';
                    }
                    consoleMsg += '\n';
                }
                if(roomStore.getPairProgramingMode()===false || roomStore.getPairProgramingRole() === 'Driver'){
                    rtcStore.getOpenVidu()?.session.signal({
                        data: consoleMsg,
                        to: [],
                        type: 'console'
                    })
                }
                roomStore.setConsole(consoleMsg);
            })
            client.subscribe('/user/' + userId + '/role', (msg) => {
                roomStore.setCurrentlySubmitting(false);
                const role = msg.body;
                roomStore.setPairProgramingMode(true);
                roomStore.setPairProgramingRole(role);
                if(role==='Navigator'){
                    const idx = roomStore.getQuestionIdx() === 0 ? 1 : 0;
                    roomStore.setQuestionIdx(idx);
                }
                if(role==='Driver'){
                    roomStore.setConsole('상대가 문제를 풀었습니다! 잠시 후 페어 프로그래밍으로 전환됩니다.');
                }
                setTimeout(() => {
                    navi('/trans/pairsolve');
                }, 2000)
            })
            client.subscribe('/user/' + userId + '/showRetroCode', (msg) => {

                const data = JSON.parse(msg.body);
       
                roomStore?.getQuestionInfos()?.forEach((questionInfo: questionInfo) => {
                    const idx = questionInfo.algoQuestionId;
                    const tmp : reviewInfo[] = [];

                    data[idx].codes.forEach((userCode: any) => {
                        tmp.push({
                            title: String(userCode.round) + " 라운드",
                            content: userCode.code,
                            language: convertUppserToLang(userCode.language)
                        })
                    })
                    tmp.push({
                        title: "정답",
                        content: data[idx].code,
                        language: convertUppserToLang(data[idx].language)
                    })
                    roomStore.getReviewInfos().push(tmp);
                });
                roomStore.setPairProgramingRole('Navigator');
                roomStore.setSec(0);

                navi('/trans/review');
            })
            client.subscribe('/user/' + userId + '/finishSocket/githubUpload', (msg) => {
                roomStore.client?.publish({
					destination: '/app/leaveRoom',
					body: JSON.stringify({
						roomId: roomStore.roomInfo?.roomId,
						isHost: roomStore.roomInfo?.host
					}),
				})
                client.deactivate();
                const githubPush: boolean = JSON.parse(msg.body);
                if(githubPush===true){
                    location.href = `https://github.com/login/oauth/authorize?client_id=a82095fde8aa68bb396d&scope=repo&redirect_uri=https://urturn.site/auth/github/upload`;
                }
                else{
                    navi('/myPage');
                }
            })


        };
        
        // client.onStompError = function (frame) {
        // };

        client.activate()
        
        roomStore.setClient(client);
    }

    return {
        connect
    }
}