class ChatApp {
    constructor() {
        this.token = localStorage.getItem('token');
        this.currentUser = null;
        this.currentChat = null; // {type: 'user'|'group', id: userId|groupId}
        this.ws = null;
        
        this.initElements();
        this.bindEvents();
        this.checkAuth();
    }
    
    initElements() {
        // 认证相关元素
        this.authSection = document.getElementById('auth-section');
        this.chatSection = document.getElementById('chat-section');
        this.loginForm = document.getElementById('login-form');
        this.registerForm = document.getElementById('register-form');
        this.loginTab = document.getElementById('login-tab');
        this.registerTab = document.getElementById('register-tab');
        
        // 用户信息元素
        this.userInfo = document.getElementById('user-info');
        this.usernameSpan = document.getElementById('username');
        this.logoutBtn = document.getElementById('logout-btn');
        
        // 聊天界面元素
        this.onlineUsersList = document.getElementById('online-users-list');
        this.groupsList = document.getElementById('groups-list');
        this.createGroupBtn = document.getElementById('create-group-btn');
        this.chatTitle = document.getElementById('chat-title');
        this.messagesContainer = document.getElementById('messages-container');
        this.messageInput = document.getElementById('message-input');
        this.sendMessageBtn = document.getElementById('send-message-btn');
    }
    
    bindEvents() {
        // 认证表单切换
        this.loginTab.addEventListener('click', () => this.switchToLogin());
        this.registerTab.addEventListener('click', () => this.switchToRegister());
        
        // 表单提交
        this.loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        this.registerForm.addEventListener('submit', (e) => this.handleRegister(e));
        this.logoutBtn.addEventListener('click', () => this.handleLogout());
        
        // 消息发送
        this.sendMessageBtn.addEventListener('click', () => this.sendMessage());
        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });
        
        // 创建群组
        this.createGroupBtn.addEventListener('click', () => this.createGroup());
    }
    
    switchToLogin() {
        this.loginTab.classList.add('active');
        this.registerTab.classList.remove('active');
        this.loginForm.classList.remove('hidden');
        this.registerForm.classList.add('hidden');
    }
    
    switchToRegister() {
        this.registerTab.classList.add('active');
        this.loginTab.classList.remove('active');
        this.registerForm.classList.remove('hidden');
        this.loginForm.classList.add('hidden');
    }
    
    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.token = data.data.accessToken;
                this.currentUser = data.data.user;
                localStorage.setItem('token', this.token);
                this.showChatInterface();
                this.connectWebSocket();
                this.loadOnlineUsers();
                this.loadUserGroups();
            } else {
                alert('登录失败: ' + data.message);
            }
        } catch (error) {
            console.error('登录错误:', error);
            alert('登录过程中发生错误');
        }
    }
    
    async handleRegister(e) {
        e.preventDefault();
        
        const username = document.getElementById('register-username').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        const nickname = document.getElementById('register-nickname').value;
        
        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, email, password, nickname })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.token = data.data.accessToken;
                this.currentUser = data.data.user;
                localStorage.setItem('token', this.token);
                this.showChatInterface();
                this.connectWebSocket();
                this.loadOnlineUsers();
                this.loadUserGroups();
            } else {
                alert('注册失败: ' + data.message);
            }
        } catch (error) {
            console.error('注册错误:', error);
            alert('注册过程中发生错误');
        }
    }
    
    handleLogout() {
        this.token = null;
        this.currentUser = null;
        localStorage.removeItem('token');
        
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        
        this.showAuthInterface();
    }
    
    checkAuth() {
        if (this.token) {
            // 验证token有效性
            this.showChatInterface();
            this.connectWebSocket();
            this.loadOnlineUsers();
            this.loadUserGroups();
        } else {
            this.showAuthInterface();
        }
    }
    
    showAuthInterface() {
        this.authSection.classList.remove('hidden');
        this.chatSection.classList.add('hidden');
        this.userInfo.classList.add('hidden');
    }
    
    showChatInterface() {
        this.authSection.classList.add('hidden');
        this.chatSection.classList.remove('hidden');
        this.userInfo.classList.remove('hidden');
        this.usernameSpan.textContent = this.currentUser.username;
    }
    
    connectWebSocket() {
        const wsUrl = `ws://localhost:8080/ws/chat?token=${this.token}`;
        this.ws = new WebSocket(wsUrl);
        
        this.ws.onopen = () => {
            console.log('WebSocket连接已建立');
        };
        
        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleWebSocketMessage(message);
        };
        
        this.ws.onclose = () => {
            console.log('WebSocket连接已关闭');
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket错误:', error);
        };
    }
    
    handleWebSocketMessage(message) {
        switch (message.type) {
            case 'USER_ONLINE':
                this.addUserToList(message.senderId, message.senderUsername, true);
                this.displaySystemMessage(`${message.senderUsername} 上线了`);
                break;
            case 'USER_OFFLINE':
                this.removeUserFromList(message.senderId);
                this.displaySystemMessage(`${message.senderUsername} 下线了`);
                break;
            case 'PRIVATE_MESSAGE':
                this.displayPrivateMessage(message);
                break;
            case 'GROUP_MESSAGE':
                this.displayGroupMessage(message);
                break;
            case 'GROUP_NOTIFICATION':
                this.displaySystemMessage(message.content);
                break;
            case 'PRIVATE_MESSAGE_ACK':
                // 这是发送私聊消息的确认，我们可以在这里加载历史消息或做其他处理
                break;
            case 'GROUP_MESSAGE_ACK':
                // 这是发送群聊消息的确认
                break;
        }
    }
    
    async loadOnlineUsers() {
        try {
            const response = await fetch('/api/users/online', {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.onlineUsersList.innerHTML = '';
                data.data.forEach(user => {
                    if (user.id !== this.currentUser.id) {
                        const li = document.createElement('li');
                        li.textContent = user.username;
                        li.dataset.userId = user.id;
                        li.addEventListener('click', () => this.startPrivateChat(user));
                        this.onlineUsersList.appendChild(li);
                    }
                });
            }
        } catch (error) {
            console.error('加载在线用户失败:', error);
        }
    }
    
    async loadUserGroups() {
        try {
            const response = await fetch('/api/groups/my', {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.groupsList.innerHTML = '';
                data.data.forEach(group => {
                    const li = document.createElement('li');
                    li.textContent = group.name;
                    li.dataset.groupId = group.id;
                    li.addEventListener('click', () => this.startGroupChat(group));
                    this.groupsList.appendChild(li);
                });
            }
        } catch (error) {
            console.error('加载群组失败:', error);
        }
    }
    
    async startPrivateChat(user) {
        this.currentChat = {
            type: 'user',
            id: user.id,
            name: user.username
        };
        this.chatTitle.textContent = `与 ${user.username} 聊天`;
        this.messagesContainer.innerHTML = '';
        
        // 加载历史消息
        await this.loadPrivateChatHistory(user.id);
    }
    
    async startGroupChat(group) {
        this.currentChat = {
            type: 'group',
            id: group.id,
            name: group.name
        };
        this.chatTitle.textContent = `群组: ${group.name}`;
        this.messagesContainer.innerHTML = '';
        
        // 加载群组历史消息
        await this.loadGroupChatHistory(group.id);
    }
    
    async loadPrivateChatHistory(userId) {
        try {
            const response = await fetch(`/api/messages/private/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                // 按时间顺序排列消息（从旧到新）
                data.data.reverse().forEach(message => {
                    const wsMessage = {
                        type: 'PRIVATE_MESSAGE',
                        senderId: message.senderId,
                        senderUsername: message.senderUsername,
                        content: message.content,
                        timestamp: message.createdAt
                    };
                    this.displayPrivateMessage(wsMessage);
                });
            }
        } catch (error) {
            console.error('加载私聊历史消息失败:', error);
        }
    }
    
    async loadGroupChatHistory(groupId) {
        try {
            const response = await fetch(`/api/messages/group/${groupId}`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                // 按时间顺序排列消息（从旧到新）
                data.data.reverse().forEach(message => {
                    const wsMessage = {
                        type: 'GROUP_MESSAGE',
                        senderId: message.senderId,
                        senderUsername: message.senderUsername,
                        content: message.content,
                        timestamp: message.createdAt
                    };
                    this.displayGroupMessage(wsMessage);
                });
            }
        } catch (error) {
            console.error('加载群聊历史消息失败:', error);
        }
    }
    
    sendMessage() {
        const content = this.messageInput.value.trim();
        if (!content || !this.currentChat) return;
        
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            const message = {
                type: this.currentChat.type === 'user' ? 'PRIVATE_MESSAGE' : 'GROUP_MESSAGE',
                content: content
            };
            
            if (this.currentChat.type === 'user') {
                message.receiverId = this.currentChat.id;
            } else {
                message.groupId = this.currentChat.id;
            }
            
            this.ws.send(JSON.stringify(message));
            this.messageInput.value = '';
        }
    }
    
    displayPrivateMessage(message) {
        const isSent = message.senderId === this.currentUser.id;
        const className = isSent ? 'sent' : 'received';
        const senderName = isSent ? '我' : message.senderUsername;
        
        const messageElement = document.createElement('div');
        messageElement.className = `message ${className}`;
        messageElement.innerHTML = `
            <div class="message-header">
                ${senderName}
                <span class="message-time">${this.formatTime(message.timestamp)}</span>
            </div>
            <div class="message-content">${this.escapeHtml(message.content)}</div>
        `;
        
        this.messagesContainer.appendChild(messageElement);
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }
    
    displayGroupMessage(message) {
        const isSent = message.senderId === this.currentUser.id;
        const senderName = isSent ? '我' : message.senderUsername;
        
        const messageElement = document.createElement('div');
        messageElement.className = `message ${isSent ? 'sent' : 'received'}`;
        messageElement.innerHTML = `
            <div class="message-header">
                ${senderName}
                <span class="message-time">${this.formatTime(message.timestamp)}</span>
            </div>
            <div class="message-content">${this.escapeHtml(message.content)}</div>
        `;
        
        this.messagesContainer.appendChild(messageElement);
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }
    
    displaySystemMessage(content) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message system';
        messageElement.innerHTML = `
            <div class="message-content">${this.escapeHtml(content)}</div>
        `;
        
        this.messagesContainer.appendChild(messageElement);
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }
    
    addUserToList(userId, username, isOnline) {
        // 检查用户是否已经在列表中
        const existingUser = Array.from(this.onlineUsersList.children)
            .find(li => li.dataset.userId == userId);
            
        if (!existingUser && userId != this.currentUser.id) {
            const li = document.createElement('li');
            li.textContent = username;
            li.dataset.userId = userId;
            this.onlineUsersList.appendChild(li);
        }
    }
    
    removeUserFromList(userId) {
        const userElement = Array.from(this.onlineUsersList.children)
            .find(li => li.dataset.userId == userId);
            
        if (userElement) {
            userElement.remove();
        }
    }
    
    async createGroup() {
        const groupName = prompt('请输入群组名称:');
        if (groupName) {
            try {
                const response = await fetch('/api/groups', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${this.token}`
                    },
                    body: JSON.stringify({ name: groupName })
                });
                
                const data = await response.json();
                
                if (data.success) {
                    alert('群组创建成功!');
                    // 重新加载群组列表
                    this.loadUserGroups();
                } else {
                    alert('创建群组失败: ' + data.message);
                }
            } catch (error) {
                console.error('创建群组错误:', error);
                alert('创建群组过程中发生错误');
            }
        }
    }
    
    formatTime(timestamp) {
        if (!timestamp) return '';
        const date = new Date(timestamp);
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});