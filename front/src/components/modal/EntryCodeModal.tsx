import { useState } from 'react';
import { Menu, MenuItem, Header, Icon, Input, Button } from 'semantic-ui-react';
import { useAxios } from '../../utils/useAxios';
import './EntryCodeModal.css';
import { useRoomStore } from '../../stores/room';

interface ModalProps {
	changeModal: () => void;
	successConnect: () => void;
}

export const EntryCodeModal = ({ changeModal, successConnect }: ModalProps) => {
	const [entryCode, setEntryCode] = useState('');
	const axios = useAxios();
	const roomStore = useRoomStore();

	const checkCode = () => {
		axios.get(`/room/enter/${entryCode}`)
		.then((res: { data: string }) => {
			const roomId = res.data;
			roomStore.getClient()?.publish({
					destination: '/app/enterRoom',
					body: JSON.stringify({
						roomId : roomId,
					}),
				});
			successConnect();
		})
		.catch(() => {
			alert("존재하지 않는 방입니다.");
		})
	}

	return (
		<>
			<div className='EntryCodeModalBackground'>
				<div className='EntryCodeModal'>
					<div className='EntryContent'>
						<div className='Title TitleSection'>
							{/* 상위 정보 메뉴 */}
							<Menu secondary size='large' className='TitleAlign Title'>
								<MenuItem name='Entry Code'>
									<Header as='h3' textAlign='center'>
										입장 코드 입력
									</Header>
								</MenuItem>
								<MenuItem name='close' position='right' onClick={changeModal}>
									<Icon className='Icon' name='close' size='large' />
								</MenuItem>
							</Menu>
						</div>
            {/* 입장 코드 입력 input폼 */}
						<div className='CodeInputForm'>
							<Input onChange={(e) => setEntryCode(e.target.value)} placeholder='입장 코드 입력' />
						</div>
            {/* 입장 버튼 */}
						<div className='InputButton'>
							<Button className='EntryCodeButton' onClick={checkCode} style={{  }}>입장하기</Button>
						</div>
					</div>
				</div>
			</div>
		</>
	);
};
