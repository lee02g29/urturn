import {
	MenuItem,
	Menu,
	Card,
	CardContent,
	CardHeader,
	CardGroup,
	Header,
	Image,
	Divider,
	Form,
	Icon,
	FormField,
	Segment,
	Button,
	Radio, Popup,
} from 'semantic-ui-react';
import logo from '../../assets/images/logo.svg';
import './WaitingPage.css';
import { FormEvent, useEffect, useRef, useState } from 'react';
import { useRoomStore } from '../../stores/room';
import { useRtcStore } from '../../stores/rtc.ts';
//import { useRtcStore } from "../../stores/rtc.ts";

interface ModalProps {
	changeModal: () => void;
	// 모달을 닫는 함수
}

// const langOptions = [
// 	{ key: 'C++', text: 'C++', value: 'C++' },
// 	{ key: 'Java', text: 'Java', value: 'Java' },
// 	{ key: 'Python', text: 'Python', value: 'Python' },
// 	{ key: 'JavaScript', text: 'JavaScript', value: 'JavaScript' },
// ];

export const WaitingPage = ({ changeModal }: ModalProps) => {
	const roomStore = useRoomStore();
	const rtcStore = useRtcStore();
	const myVideoRef = useRef<HTMLVideoElement | null>(null);
	const otherVideoRef = useRef<HTMLVideoElement | null>(null);

	// 스피커 볼륨, 마이크 볼륨
	const [difficulty, setDifficulty] = useState('LEVEL1');
	// 난이도
	//const rtcStore = useRtcStore();
	const difficulties = [
		{ label: 'LEVEL 1', value: 'LEVEL1', color: '#A9D9DC' },
		{ label: 'LEVEL 2', value: 'LEVEL2', color: '#AAD79F' },
		{ label: 'LEVEL 3', value: 'LEVEL3', color: '#dce46e' },
		{ label: 'LEVEL 4', value: 'LEVEL4', color: '#E5ACAC' },
		{ label: 'LEVEL 5', value: 'LEVEL5', color: '#9B9B9B' },
	];
	// 난이도 목록
	const [isMuted, setIsMuted] = useState(false);
	const [isMicMuted, setIsMicMuted] = useState(false);
	useEffect(() => {
		if (rtcStore.streamManager && myVideoRef.current) {
			rtcStore.streamManager.addVideoElement(myVideoRef.current);
		}
	}, [rtcStore.streamManager]);

	useEffect(() => {
		if (rtcStore.subscriber && otherVideoRef.current) {
			rtcStore.subscriber.addVideoElement(otherVideoRef.current);
		}
	}, [rtcStore.subscriber]);

	const handleDifficulty = (_e: FormEvent<HTMLInputElement>, data: any) => {
		setDifficulty(data.value);
	}; // 난이도 설정 조정 함수

	const selectDifficulty = () => {
		if (roomStore.client === undefined || roomStore.client === null) {
			alert('문제가 발생했습니다. 재접속 해주세요.');
			return;
		}
		if (roomStore.roomInfo === undefined || roomStore.roomInfo === null) {
			alert('문제가 발생했습니다. 재접속 해주세요.');
			return;
		}

		roomStore.client.publish({
			destination: '/app/selectLevel',
			body: JSON.stringify({
				roomId: roomStore.roomInfo.roomId,
				level: difficulty,
			}),
		});
	};
	// const toggleMute = () => {
	// 	if (myVideoRef.current && otherVideoRef.current) {
	// 		myVideoRef.current.muted = !myVideoRef.current.muted;
	// 		otherVideoRef.current.muted = !otherVideoRef.current.muted;
	// 	}
	// };
	const toggleMute = () => {
		if (myVideoRef.current && otherVideoRef.current) {
			const newMuteStatus = !myVideoRef.current.muted;
			myVideoRef.current.muted = newMuteStatus;
			otherVideoRef.current.muted = newMuteStatus; // Assuming you want to mute both videos together
			setIsMuted(newMuteStatus); // This will trigger a re-render
		}
	};

	const toggleMicrophone = () => {
		const publisher = rtcStore.getPublisher(); // Assuming you have a method to get the Publisher
		if (publisher) {
			const newMicStatus = !isMicMuted;
			publisher.publishAudio(!newMicStatus); // Enable or disable audio based on new mute status
			setIsMicMuted(newMicStatus);
		}
	};

	return (
		<>
			<div className='WaitingRoomBackground'>
				<div className='WaitingRoom'>
					<div className='WaitingContent'>
						<div className='Header HeaderSection'>
							{/* 상위 정보 메뉴 */}
							<Menu pointing secondary size='large' style={{ marginBottom: '2vh' }}>
								<MenuItem className='Header'>
									<img alt='URTurn' src={logo} style={{ width: '100px' }} />
								</MenuItem>
								<MenuItem name='Waiting Room'>
									<Header as='h3' textAlign='center'>
										Waiting Room
									</Header>
								</MenuItem>
								<MenuItem name='Entry Code'>
									<Header className='EntryCode' as='h3' textAlign='center'>
										입장 코드 : {roomStore.roomInfo?.entryCode}
									</Header>
								</MenuItem>
								{/* <MenuItem>
									<Header className='EntryCode' as='h3' textAlign='center'>
										선택 언어  :{' '}
										<Dropdown
											search
											defaultValue={langOptions[langOptions.length - 1].value}
											searchInput={{ type: 'string' }}
											options={langOptions}
										/>
									</Header>
								</MenuItem> */}
								<MenuItem name='close' position='right' onClick={changeModal}>
									<Icon className='Icon' name='close' size='large' />
								</MenuItem>
							</Menu>
						</div>
						<div className='AllSection'>
							<div className='RightSection'>
								{/* 프로필 영역 */}
								<CardGroup className='FitContent'>
									{/* 방장 */}
									<Card className='Card-without-border'>
										<CardContent
											style={{
												display: 'flex',
												flexDirection: 'column',
												alignItems: 'center',
												justifyContent: 'center',
											}}
										>
											<Image
												src={
													roomStore.userInfo?.myUserProfileUrl
														? roomStore.userInfo!.myUserProfileUrl
														: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png'
												}
												style={{ width: '10vw', height: 'auto', minWidth: '150px' }}
												rounded
											/>
											<Divider hidden />
											<CardHeader textAlign='center'>
												{roomStore.userInfo?.myUserNickName
													? roomStore.userInfo.myUserNickName
													: '내 정보 로딩 중'}
											</CardHeader>
										</CardContent>
									</Card>
									{/* 입장 파트너 */}
									<Card className='Card-without-border'>
										<CardContent
											style={{
												display: 'flex',
												flexDirection: 'column',
												alignItems: 'center',
												justifyContent: 'center',
											}}
										>
											<Image
												src={
													roomStore.userInfo?.relativeUserProfileUrl
														? roomStore.userInfo.relativeUserProfileUrl
														: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png'
												}
												style={{ width: '10vw', height: 'auto', minWidth: '150px' }}
												rounded
											/>
											<Divider hidden />
											<CardHeader textAlign='center'>
												{roomStore.userInfo?.relativeUserNickName
													? roomStore.userInfo.relativeUserNickName
													: '미접속'}
											</CardHeader>
										</CardContent>
									</Card>
								</CardGroup>
								{/* 볼륨 조정 영역 */}
								<div
									style={{
										width: '32vw',
										display: 'flex',
										alignItems: 'center',
										justifyContent: 'space-around',
									}}
								>
									<div>
										{/* 마이크 아이콘 */}
										<Button className='VolumeButton' onClick={toggleMicrophone}>
											<Icon							
												className='Icon'
												name={isMicMuted ? 'microphone slash' : 'microphone'}
												size='huge'
											/>
										</Button>
									</div>
									<div >
										{/* 스피커 아이콘 */}
										<Button className='VolumeButton' onClick={toggleMute}>
											<Icon
												className='Icon'
												name={isMuted ? 'volume off' : 'volume up'}
												size='huge'
											/>
										</Button>
									</div>
								</div>
							</div>
							<div className='LeftSection'>
								<div>
									<Form>
										{/* 난이도 선택 헤더 */}
										<Header as='h2' textAlign='center'>
											난이도 선택
										</Header>
										{/* 난이도 버튼 그룹 */}
										{difficulties.map((item) => (
											<FormField key={item.value}>
												<Segment
													size='small'
													style={{
														backgroundColor:
															difficulty === item.value ? item.color : 'transparent',
														border: difficulty === item.value ? 'none' : undefined,
														borderRadius: '10px',
													}}
												>
													<Radio
														label={item.label}
														name='difficulties group'
														value={item.value}
														checked={difficulty === item.value}
														onChange={handleDifficulty}
														className={difficulty === item.value ? 'RadioChecked' : ''}
													/>
												</Segment>
											</FormField>
										))}
									</Form>
								</div>
								{/* 시작 버튼 */}
								<div className='StartButton'>
									{roomStore.roomInfo?.host ? (
										<Button
											onClick={selectDifficulty}
											className='StartButtonStyle'
											style={{width: '11.5vw', height: '8vh', fontSize: '1.3rem'}}
										>
											시작하기
										</Button>
									) : (
										<Popup
											content='방장만 시작할 수 있습니다'
											trigger={
												<div>
													<Button
														disabled
														className='StartButtonStyle'
														style={{width: '11.5vw', height: '8vh', fontSize: '1.3rem'}}
													>
														시작하기
													</Button>
												</div>
											}
										/>
									)}
								</div>
							</div>
						</div>
					</div>
				</div>
				<video autoPlay={true} controls={true} ref={myVideoRef} style={{display: 'none'}}/>
				<video autoPlay={true} controls={true} ref={otherVideoRef} style={{display: 'none'}}/>
			</div>
		</>
	);
};
