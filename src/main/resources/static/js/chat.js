// js/chat.js

let stompClient = null;
let currentSubscription = null; // 현재 구독 중인 STOMP 구독 객체
let selectedRoomId = null;    // 현재 선택된 채팅방 ID
let selectedRoomName = null;  // 현재 선택된 채팅방 이름
// let activeRoomId = null; // 이 변수가 필요하면 이전 답변을 참고하여 추가해주세요. 현재 코드에서는 selectedRoomId가 입장된 방을 의미합니다.
const myUserId = 'user-' + Math.random().toString(36).substring(2, 7); // 고유한 임시 사용자 ID 생성

// DOM 요소 가져오기
const connectButton = document.getElementById('connectButton');
const disconnectButton = document.getElementById('disconnectButton');
const sendButton = document.getElementById('sendButton');
const messageInput = document.getElementById('messageInput');
const messagesList = document.getElementById('messages');
const connectionStatusSpan = document.getElementById('connectionStatus');
const chatWindow = document.getElementById('chatWindow');
const chatRoomListUl = document.getElementById('chatRoomList');
const newRoomNameInput = document.getElementById('newRoomNameInput');
const createRoomButton = document.getElementById('createRoomButton');
const currentRoomNameSpan = document.getElementById('currentRoomName');
const currentRoomIdSpan = document.getElementById('currentRoomId');

// 사용자별 색상 매핑 (간단한 예시)
const userColors = {};
function getUserColor(userId) {
    if (!userColors[userId]) {
        const colors = ['#e6244f', '#007bff', '#28a745', '#ffc107', '#17a2b8', '#6610f2', '#fd7e14', '#9c27b0', '#009688'];
        userColors[userId] = colors[Object.keys(userColors).length % colors.length];
    }
    return userColors[userId];
}

// UI 상태 업데이트 함수
function setConnected(connected) {
    connectButton.disabled = connected;
    disconnectButton.disabled = !connected;

    connectionStatusSpan.textContent = `연결 상태: ${connected ? '연결됨' : '연결되지 않음'}`;
    connectionStatusSpan.className = `connection-status ${connected ? 'connected' : 'disconnected'}`;

    // STOMP 연결 상태와 채팅방 선택 상태에 따라 메시지 전송/입력 활성화
    messageInput.disabled = !(connected && selectedRoomId);
    sendButton.disabled = !(connected && selectedRoomId);
    console.log("[setConnected]: " + currentRoomNameSpan.textContent + ' ' + currentRoomIdSpan.textContent);
    // 채팅방 생성 버튼은 항상 활성화
    createRoomButton.disabled = false;
    newRoomNameInput.disabled = false;

    if (!connected) {
        messagesList.innerHTML = ''; // 연결 끊기면 메시지 지움
        selectedRoomId = null;
        selectedRoomName = null;
        // updateCurrentRoomInfo(); // 여기서 직접 호출하지 않습니다. disconnect 시 호출
        chatRoomListUl.innerHTML = ''; // 채팅방 목록도 초기화
    }
}

// 메시지를 화면에 표시하는 함수
function showMessage(messageData, isMine = false, type = 'talk') {
    const li = document.createElement('li');
    li.className = 'message-item';

    let bubbleContent;
    let senderName = messageData.senderId; // 클라이언트 전송 시 'senderId'
    // 서버에서 받은 메시지의 내용은 'content' 필드를 사용
    const messageContent = messageData.content; // <--- 이 부분 변경 (messageData.message 대신 messageData.content)
    const timestamp = new Date(messageData.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    if (type === 'talk') {
        li.classList.add(isMine ? 'my-message' : 'other-message');
        const senderColor = getUserColor(senderName);

        bubbleContent = `
            ${isMine ? '' : `<span class="sender-name" style="color: ${senderColor};">${senderName}</span>`}
            ${messageContent} `;
        li.innerHTML = `
            <div class="message-bubble">${bubbleContent}</div>
            <div class="message-meta">${timestamp}</div>
        `;
    } else if (type === 'system') {
        li.classList.add('system-message');
        // 시스템 메시지의 내용은 `messageData.message` 또는 `messageData.content`일 수 있으므로 유연하게 처리
        bubbleContent = messageData.content || messageData.message; // <--- 이 부분 변경 (content 우선, message도 fallback)
        li.innerHTML = `<div class="message-bubble">${bubbleContent}</div>`;
    } else if (type === 'error') {
        li.classList.add('system-message');
        li.style.color = 'red';
        // 에러 메시지는 보통 String으로 넘어오므로 그대로 사용
        bubbleContent = `오류: ${messageData}`;
        li.innerHTML = `<div class="message-bubble">${bubbleContent}</div>`;
    }

    messagesList.appendChild(li);
    // 메시지 추가 시 스크롤 자동 이동
    chatWindow.scrollTop = chatWindow.scrollHeight;
}

// 현재 채팅방 정보 UI 업데이트
function updateCurrentRoomInfo() {
    currentRoomNameSpan.textContent = selectedRoomName || '선택 안 됨';
    currentRoomIdSpan.textContent = selectedRoomId || '';
    // setConnected(stompClient && stompClient.connected); // <-- 이 라인이 무한 루프의 원인! 제거합니다.

    const connected = stompClient && stompClient.connected;
    messageInput.disabled = !(connected && selectedRoomId);
    sendButton.disabled = !(connected && selectedRoomId);
    console.log("[updateCurrentRoomInfo]: " + currentRoomNameSpan.textContent + ' ' + currentRoomIdSpan.textContent);
}


// WebSocket 연결 함수
connectButton.addEventListener('click', () => {
    if (stompClient && stompClient.connected) {
        showMessage({ content: '이미 WebSocket에 연결되어 있습니다.', type: 'info' }, false, 'system'); // <--- content로 변경
        return;
    }

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.heartbeat.outgoing = 10000;
    stompClient.heartbeat.incoming = 10000;

    stompClient.connect({}, (frame) => {
        console.log('Connected: ' + frame);
        setConnected(true); // 연결 상태 업데이트
        updateCurrentRoomInfo(); // 초기 방 정보 UI 업데이트
        showMessage({ content: `서버에 WebSocket 연결되었습니다. 사용자 ID: ${myUserId}` }, false, 'system'); // <--- content로 변경

        // 개인 메시지 큐 구독 (선택 사항)
        stompClient.subscribe('/user/queue/messages', (message) => {
            const parsedMessage = JSON.parse(message.body);
            showMessage(parsedMessage, false, 'system'); // 개인 알림은 시스템 메시지로 표시 (parsedMessage 내 content 또는 message 사용)
        });

        // 연결 후 채팅방 목록을 즉시 로드
        loadChatRooms();

    }, (error) => {
        console.error('Connection error: ' + error);
        showMessage('WebSocket 연결 오류: ' + error, false, 'error');
        setConnected(false);
        updateCurrentRoomInfo(); // 연결 실패 시 방 정보 초기화 UI 업데이트
    });
});

// WebSocket 연결 끊기 함수
disconnectButton.addEventListener('click', () => {
    if (stompClient !== null && stompClient.connected) {
        if (currentSubscription) {
            currentSubscription.unsubscribe();
            currentSubscription = null;
        }
        stompClient.disconnect(() => {
            console.log("Disconnected");
            setConnected(false); // 연결 상태 업데이트
            updateCurrentRoomInfo(); // 방 정보 초기화 UI 업데이트
            showMessage({ content: "WebSocket 연결이 끊어졌습니다." }, false, 'system'); // <--- content로 변경
        });
    } else {
        showMessage({ content: "이미 WebSocket에 연결되지 않았습니다." }, false, 'system'); // <--- content로 변경
    }
});

// 메시지 전송 함수
sendButton.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', (event) => {
    if (event.key === 'Enter') {
        sendMessage();
    }
});

function sendMessage() {
    const messageText = messageInput.value.trim();

    if (!stompClient || !stompClient.connected) {
        showMessage({ content: '먼저 WebSocket에 연결해주세요.' }, false, 'error'); // <--- content로 변경
        return;
    }
    if (!selectedRoomId) {
        showMessage({ content: '먼저 참여할 채팅방을 선택해주세요.' }, false, 'error'); // <--- content로 변경
        return;
    }
    if (!messageText) {
        showMessage({ content: '메시지를 입력해주세요.' }, false, 'error'); // <--- content로 변경
        return;
    }

    const chatMessage = {
        senderId: myUserId,
        roomId: selectedRoomId, // 선택된 방 ID 사용
        content: messageText, // <--- 이 부분 변경 (message 대신 content)
        type: 'TALK',
        timestamp: Date.now()
    };

    // 내 메시지는 먼저 화면에 표시 (로컬 에코)
    showMessage(chatMessage, true, 'talk');

    stompClient.send(`/app/chatrooms/${selectedRoomId}`, {}, JSON.stringify(chatMessage));
    messageInput.value = ''; // 입력 필드 초기화
}

// 채팅방 목록 로드 함수
async function loadChatRooms() {
    try {
        const response = await fetch('/api/chatrooms'); // 서버의 REST API 호출
        const rooms = await response.json();

        chatRoomListUl.innerHTML = ''; // 기존 목록 초기화
        rooms.forEach(room => {
            const li = document.createElement('li');
            li.setAttribute('data-room-id', room.id);
            li.innerHTML = `
                <span>${room.name}</span>
                <span class="room-id">ID: ${room.id}</span>
            `;
            li.addEventListener('click', () => selectChatRoom(room.id, room.name));
            chatRoomListUl.appendChild(li);
        });
        showMessage({ content: '채팅방 목록을 불러왔습니다.', type: 'info' }, false, 'system'); // <--- content로 변경

        // 만약 이전에 선택된 방이 있다면 다시 선택 상태로 만듦
        if (selectedRoomId) {
            const previouslySelectedLi = document.querySelector(`[data-room-id="${selectedRoomId}"]`);
            if (previouslySelectedLi) {
                previouslySelectedLi.classList.add('selected');
            }
        }

    } catch (error) {
        console.error('Failed to load chat rooms:', error);
        showMessage('채팅방 목록을 불러오는데 실패했습니다.', false, 'error');
    }
}

// 채팅방 선택 함수
async function selectChatRoom(roomId, roomName) { // async 키워드 추가
    if (stompClient && stompClient.connected) {
        // 기존 구독 해지 (있다면)
        if (currentSubscription) {
            const previousRoomId = selectedRoomId;
            currentSubscription.unsubscribe();
            currentSubscription = null;
            // 이전 방에 퇴장 메시지 전송 (현재 주석 처리된 부분)
//            const leaveMessage = {
//                senderId: myUserId,
//                roomId: previousRoomId,
//                content: `${myUserId} 님이 퇴장했습니다.`, // <--- content로 변경
//                type: 'LEAVE',
//                timestamp: Date.now()
//            };
//            stompClient.send(`/app/chatrooms/${previousRoomId}`, {}, JSON.stringify(leaveMessage));
//            showMessage({ content: `채팅방 '${selectedRoomName}' 구독을 해지했습니다.`, type: 'info' }, false, 'system'); // <--- content로 변경
        }

        // 새로운 방 구독
        selectedRoomId = roomId;
        selectedRoomName = roomName;
        messagesList.innerHTML = ''; // 새 방 선택 시 메시지 초기화

        // **1. 이전 메시지 로드 (Redis Stream에서 가져옴)**
        try {
            // startId는 '0-0'부터 시작하거나, 특정 시점부터의 메시지를 위해 타임스탬프 기반 ID를 사용할 수 있습니다.
            const historyUrl = `/api/chatrooms/${roomId}/messages?startId=0-0&count=100`; // 처음부터 100개 메시지 요청
            const response = await fetch(historyUrl);
            const previousMessages = await response.json();
            previousMessages.forEach(msg => {
                const isMine = msg.senderId === myUserId;
                showMessage(msg, isMine, 'talk'); // showMessage 함수가 content를 사용하도록 변경되었으므로 별도 수정 불필요
            });
            showMessage({ content: `채팅방 '${roomName}'의 이전 메시지를 불러왔습니다.`, type: 'info' }, false, 'system'); // <--- content로 변경
        } catch (error) {
            console.error('Failed to load previous messages from stream:', error);
            showMessage('이전 메시지를 불러오는데 실패했습니다.', false, 'error');
        }

        const subscriptionId = `room-sub-${roomId}`;
        currentSubscription = stompClient.subscribe(`/topic/chatrooms/${roomId}`, (message) => {
            const parsedMessage = JSON.parse(message.body);
            // 내가 보낸 일반 메시지는 로컬 에코되었으므로 중복 표시 방지
            // 하지만 ENTER/LEAVE 메시지는 서버에서 다시 받아서 표시 (다른 사람의 입장/퇴장도 표시)
            if (parsedMessage.senderId !== myUserId || parsedMessage.type === 'ENTER' || parsedMessage.type === 'LEAVE') {
                showMessage(parsedMessage, false, 'talk'); // showMessage 함수가 content를 사용하도록 변경되었으므로 별도 수정 불필요
            }
        }, { id: subscriptionId });

        // 새로운 방에 입장 메시지 전송
        const enterMessage = {
            senderId: myUserId,
            roomId: roomId,
            content: `${myUserId} 님이 입장했습니다.`, // <--- 이 부분 변경 (message 대신 content)
            type: 'ENTER',
            timestamp: Date.now()
        };
        stompClient.send(`/app/chatrooms/${roomId}`, {}, JSON.stringify(enterMessage));

        console.log(`Subscribed to /topic/chatrooms/${roomId} with ID: ${subscriptionId}`);
        showMessage({ content: `채팅방 '${roomName}'에 입장했습니다.`, type: 'info' }, false, 'system'); // <--- content로 변경

        // UI에서 선택된 방 표시 업데이트
        document.querySelectorAll('#chatRoomList li').forEach(li => li.classList.remove('selected'));
        document.querySelector(`[data-room-id="${roomId}"]`).classList.add('selected');

        updateCurrentRoomInfo(); // 현재 방 정보 UI 업데이트
    } else {
        showMessage({ content: '먼저 WebSocket에 연결해주세요.', type: 'error' }, false, 'system'); // <--- content로 변경
    }
}

// 새로운 채팅방 생성 함수
createRoomButton.addEventListener('click', async () => {
    const newRoomName = newRoomNameInput.value.trim();
    if (!newRoomName) {
        showMessage({ content: '새 채팅방 이름을 입력해주세요.', type: 'error' }, false, 'system'); // <--- content로 변경
        return;
    }
    if (!stompClient || !stompClient.connected) {
         showMessage({ content: '먼저 WebSocket에 연결해주세요.', type: 'error' }, false, 'system'); // <--- content로 변경
         return;
    }

    try {
        const response = await fetch(`/api/chatrooms?name=${encodeURIComponent(newRoomName)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        const newRoom = await response.json();
        showMessage({ content: `'${newRoom.name}' 채팅방이 생성되었습니다.`, type: 'info' }, false, 'system'); // <--- content로 변경
        newRoomNameInput.value = ''; // 입력 필드 초기화
        loadChatRooms(); // 채팅방 목록 새로고침
        selectChatRoom(newRoom.id, newRoom.name); // 새로 생성된 방으로 자동 입장
    } catch (error) {
        console.error('Failed to create chat room:', error);
        showMessage('채팅방 생성에 실패했습니다.', false, 'error');
    }
});

// 초기 UI 상태 설정
setConnected(false); // 페이지 로드 시 초기 UI 상태 설정
updateCurrentRoomInfo(); // 페이지 로드 시 초기 방 정보 UI 설정