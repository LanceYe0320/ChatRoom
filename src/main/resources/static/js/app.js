class ChatApp {
    constructor() {
        this.token = localStorage.getItem('token');
        this.currentUser = null;
        this.currentChat = null; // {type: 'user'|'group', id: userId|groupId}
        this.currentGroup = null; // 当前聊天的群组信息
        this.ws = null;
        this.tokenCheckInterval = null; // 添加定时检查token的间隔
        this.offlineMessageCheckInterval = null; // 添加离线消息检查定时器
        
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
        
        // 群组成员相关元素
        this.groupMembersList = document.getElementById('group-members-list');
        this.groupMembersSection = document.querySelector('.group-members');
        this.inviteMemberBtn = document.getElementById('invite-member-btn');
        this.inviteUserBtn = document.getElementById('invite-user-btn');
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
        
        // 邀请成员
        this.inviteMemberBtn.addEventListener('click', () => this.inviteMember());
        this.inviteUserBtn.addEventListener('click', () => this.inviteUser());
        
        // 添加检查离线消息按钮事件
        document.addEventListener('keydown', (e) => {
            // 按Ctrl+O来手动检查离线消息
            if (e.ctrlKey && e.key === 'o') {
                e.preventDefault();
                this.checkOfflineMessages();
            }
        });
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
                this.startOfflineMessageCheck(); // 开始检查离线消息
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
                this.startOfflineMessageCheck(); // 开始检查离线消息
            } else {
                alert('注册失败: ' + data.message);
            }
        } catch (error) {
            console.error('注册错误:', error);
            alert('注册过程中发生错误');
        }
    }
    
    handleLogout() {
        // 清除token检查定时器
        if (this.tokenCheckInterval) {
            clearInterval(this.tokenCheckInterval);
            this.tokenCheckInterval = null;
        }
        
        this.token = null;
        this.currentUser = null;
        localStorage.removeItem('token');
        
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        
        this.showAuthInterface();
    }
    
    async checkAuth() {
        if (this.token) {
            // 验证token有效性
            try {
                const isValid = await this.validateToken(this.token);
                if (isValid) {
                    this.showChatInterface();
                    this.connectWebSocket();
                    this.loadOnlineUsers();
                    this.loadUserGroups();
                    // 设置定时检查token有效性
                    this.startTokenValidation();
                } else {
                    // Token无效，清除并显示登录界面
                    this.clearAuthAndShowLogin();
                }
            } catch (error) {
                console.error('Token验证错误:', error);
                this.clearAuthAndShowLogin();
            }
        } else {
            this.showAuthInterface();
        }
    }
    
    // 添加定期token验证
    startTokenValidation() {
        // 每5分钟检查一次token有效性
        this.tokenCheckInterval = setInterval(async () => {
            if (this.token) {
                try {
                    const isValid = await this.validateToken(this.token);
                    if (!isValid) {
                        this.clearAuthAndShowLogin();
                    }
                } catch (error) {
                    console.error('Token验证错误:', error);
                    this.clearAuthAndShowLogin();
                }
            }
        }, 5 * 60 * 1000); // 5分钟
    }
    
    // 清除认证信息并显示登录界面
    clearAuthAndShowLogin() {
        // 清除token检查定时器
        if (this.tokenCheckInterval) {
            clearInterval(this.tokenCheckInterval);
            this.tokenCheckInterval = null;
        }
        
        // 清除离线消息检查定时器
        if (this.offlineMessageCheckInterval) {
            clearInterval(this.offlineMessageCheckInterval);
            this.offlineMessageCheckInterval = null;
        }
        
        this.token = null;
        this.currentUser = null;
        localStorage.removeItem('token');
        
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        
        this.showAuthInterface();
    }
    
    async validateToken(token) {
        try {
            const response = await fetch('/api/users/me', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            return response.ok;
        } catch (error) {
            console.error('Token验证失败:', error);
            return false;
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
            // 检查是否因为认证失败导致连接关闭
            if (this.token) {
                this.validateToken(this.token).then(isValid => {
                    if (!isValid) {
                        this.clearAuthAndShowLogin();
                    }
                }).catch(error => {
                    console.error('Token验证错误:', error);
                    this.clearAuthAndShowLogin();
                });
            }
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket错误:', error);
        };
    }
    
    handleWebSocketMessage(message) {
        switch (message.type) {
            case 'USER_ONLINE':
                this.updateUserStatus(message.senderId, message.senderUsername, true);
                this.displaySystemMessage(`${message.senderUsername} 上线了`);
                break;
            case 'USER_OFFLINE':
                this.updateUserStatus(message.senderId, message.senderUsername, false);
                this.displaySystemMessage(`${message.senderUsername} 下线了`);
                break;
            case 'PRIVATE_MESSAGE':
                // 检查发送者是否在用户列表中，如果不在则添加
                this.addUserIfNotInList(message.senderId, message.senderUsername, false);
                this.displayPrivateMessage(message);
                break;
            case 'GROUP_MESSAGE':
                this.displayGroupMessage(message);
                break;
            case 'GROUP_NOTIFICATION':
                this.displaySystemMessage(message.content);
                break;
            case 'PRIVATE_MESSAGE_ACK':
                // 这是发送私聊消息的确认，消息已成功发送或已保存为离线消息
                console.log('私聊消息已发送或保存为离线消息:', message);
                break;
            case 'GROUP_MESSAGE_ACK':
                // 这是发送群聊消息的确认
                break;
        }
    }
    
    async loadOnlineUsers() {
        try {
            // 获取在线用户信息
            const onlineUsersResponse = await fetch('/api/users/online', {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const onlineUsersData = await onlineUsersResponse.json();
            
            if (onlineUsersData.success) {
                this.onlineUsersList.innerHTML = '';
                
                // 获取所有用户信息以显示完整列表
                const allUsersResponse = await fetch('/api/users', {
                    headers: {
                        'Authorization': `Bearer ${this.token}`
                    }
                });
                
                const allUsersData = await allUsersResponse.json();
                
                if (allUsersData.success) {
                    // 创建在线用户ID集合
                    const onlineUserIds = new Set(onlineUsersData.data.map(user => user.id));
                    
                    // 显示所有用户，标记在线/离线状态
                    allUsersData.data.forEach(user => {
                        if (user.id !== this.currentUser.id) {
                            const li = document.createElement('li');
                            
                            // 根据在线状态设置样式
                            if (onlineUserIds.has(user.id)) {
                                li.innerHTML = `<strong>${user.username}</strong> (在线)`;
                                li.style.fontWeight = 'bold';
                            } else {
                                li.innerHTML = `${user.username} (离线)`;
                                li.style.fontWeight = 'normal';
                            }
                            
                            li.dataset.userId = user.id;
                            li.dataset.username = user.username;
                            li.dataset.online = onlineUserIds.has(user.id);
                            li.addEventListener('click', () => this.startPrivateChat(user));
                            this.onlineUsersList.appendChild(li);
                        }
                    });
                }
            }
        } catch (error) {
            console.error('加载用户列表失败:', error);
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
        // 确保user对象包含所有必要属性
        const userObj = {
            id: user.id,
            username: user.username || user.name,
            online: user.online
        };
        
        this.currentChat = {
            type: 'user',
            id: userObj.id,
            name: userObj.username
        };
        this.currentGroup = null;
        this.chatTitle.textContent = `与 ${userObj.username} 聊天`;
        this.messagesContainer.innerHTML = '';
        this.groupMembersSection.classList.add('hidden');
        this.inviteUserBtn.classList.add('hidden'); // 默认隐藏邀请用户按钮
        this.inviteMemberBtn.classList.add('hidden');
        
        // 加载历史消息
        await this.loadPrivateChatHistory(userObj.id);
    }
    
    async startGroupChat(group) {
        this.currentChat = {
            type: 'group',
            id: group.id,
            name: group.name
        };
        this.currentGroup = group;
        this.chatTitle.textContent = `群组: ${group.name}`;
        this.messagesContainer.innerHTML = '';
        this.groupMembersSection.classList.remove('hidden');
        this.inviteUserBtn.classList.add('hidden');
        this.inviteMemberBtn.classList.remove('hidden');
        
        // 加载群组历史消息
        await this.loadGroupChatHistory(group.id);
        
        // 加载群组成员
        await this.loadGroupMembers(group.id);
    }
    
    async loadGroupMembers(groupId) {
        try {
            const response = await fetch(`/api/groups/${groupId}/members/list`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.groupMembersList.innerHTML = '';
                data.data.forEach(member => {
                    const li = document.createElement('li');
                    let roleText = '';
                    if (member.id === this.currentGroup.ownerId) {
                        roleText = ' (群主)';
                    }
                    li.textContent = member.username + roleText;
                    li.dataset.userId = member.id;
                    
                    // 如果当前用户是群主且目标不是自己，则添加踢人按钮
                    if (this.currentGroup.ownerId === this.currentUser.id && member.id !== this.currentUser.id) {
                        const kickBtn = document.createElement('button');
                        kickBtn.textContent = '踢出';
                        kickBtn.className = 'kick-btn';
                        kickBtn.onclick = (e) => {
                            e.stopPropagation();
                            this.kickMember(groupId, member.id, member.username);
                        };
                        li.appendChild(kickBtn);
                    }
                    
                    this.groupMembersList.appendChild(li);
                });
            }
        } catch (error) {
            console.error('加载群组成员失败:', error);
        }
    }

    // 踢出群成员
    async kickMember(groupId, userId, username) {
        if (!confirm(`确定要将用户 "${username}" 踢出群组吗？`)) {
            return;
        }
        
        try {
            const response = await fetch(`/api/groups/${groupId}/members/${userId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                alert(`用户 "${username}" 已被踢出群组`);
                // 重新加载群组成员列表
                await this.loadGroupMembers(groupId);
            } else {
                alert('踢出用户失败: ' + data.message);
            }
        } catch (error) {
            console.error('踢出用户失败:', error);
            alert('踢出用户过程中发生错误');
        }
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
            .find(li => li.dataset.userId === userId);
            
        if (!existingUser && userId !== this.currentUser.id) {
            const li = document.createElement('li');
            
            // 根据在线状态设置样式
            if (isOnline) {
                li.innerHTML = `<strong>${username}</strong> (在线)`;
                li.style.fontWeight = 'bold';
            } else {
                li.innerHTML = `${username} (离线)`;
                li.style.fontWeight = 'normal';
            }
            
            li.dataset.userId = userId;
            li.dataset.username = username;
            li.dataset.online = isOnline;
            li.addEventListener('click', () => {
                // 创建用户对象以兼容startPrivateChat方法
                const user = { id: userId, username: username, online: isOnline };
                this.startPrivateChat(user);
            });
            this.onlineUsersList.appendChild(li);
        }
    }
    
    removeUserFromList(userId) {
        // 不删除用户，只更新状态为离线
        this.updateUserStatus(userId, null, false);
    }
    
    updateUserStatus(userId, username, isOnline) {
        // 查找用户列表项
        const userElement = Array.from(this.onlineUsersList.children)
            .find(li => li.dataset.userId === userId);
            
        if (userElement) {
            // 更新现有用户的显示
            if (username) {
                userElement.dataset.username = username;
            }
            userElement.dataset.online = isOnline;
            
            // 根据在线状态更新显示
            if (isOnline) {
                userElement.innerHTML = `<strong>${userElement.dataset.username}</strong> (在线)`;
                userElement.style.fontWeight = 'bold';
            } else {
                userElement.innerHTML = `${userElement.dataset.username} (离线)`;
                userElement.style.fontWeight = 'normal';
            }
        }
        // 如果用户不在列表中，不自动添加，保持现有逻辑
    }
    
    addUserIfNotInList(userId, username, isOnline) {
        // 检查用户是否已经在列表中
        const existingUser = Array.from(this.onlineUsersList.children)
            .find(li => li.dataset.userId === userId);
            
        if (!existingUser && userId !== this.currentUser.id) {
            const li = document.createElement('li');
            
            // 根据在线状态设置样式
            if (isOnline) {
                li.innerHTML = `<strong>${username}</strong> (在线)`;
                li.style.fontWeight = 'bold';
            } else {
                li.innerHTML = `${username} (离线)`;
                li.style.fontWeight = 'normal';
            }
            
            li.dataset.userId = userId;
            li.dataset.username = username;
            li.dataset.online = isOnline;
            li.addEventListener('click', () => {
                // 创建用户对象以兼容startPrivateChat方法
                const user = { id: userId, username: username, online: isOnline };
                this.startPrivateChat(user);
            });
            this.onlineUsersList.appendChild(li);
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
    
    async inviteMember() {
        if (!this.currentGroup) {
            alert('请先选择一个群组');
            return;
        }
        
        // 创建一个包含所有在线用户的列表供选择
        try {
            const response = await fetch('/api/users/online', {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success) {
                // 过滤掉已经在群组中的用户
                const groupId = this.currentGroup.id;
                const groupMembersResponse = await fetch(`/api/groups/${groupId}/members/list`, {
                    headers: {
                        'Authorization': `Bearer ${this.token}`
                    }
                });
                
                const groupMembersData = await groupMembersResponse.json();
                const groupMemberIds = groupMembersData.data.map(member => member.id);
                
                const availableUsers = data.data.filter(user => 
                    user.id !== this.currentUser.id && 
                    !groupMemberIds.includes(user.id)
                );
                
                if (availableUsers.length === 0) {
                    alert('没有可邀请的在线用户');
                    return;
                }
                
                // 创建选择对话框
                let options = '';
                availableUsers.forEach(user => {
                    options += `<option value="${user.id}">${user.username}</option>`;
                });
                
                const selectedUserId = await promptWithSelect('请选择要邀请的用户:', options);
                if (selectedUserId) {
                    // 发送邀请请求
                    await this.sendGroupInvite(groupId, selectedUserId);
                }
            }
        } catch (error) {
            console.error('加载用户列表失败:', error);
            alert('加载用户列表失败');
        }
    }
    
    async inviteUser() {
        alert('私聊不需要邀请用户，您可以直接在在线用户列表中选择用户进行聊天');
    }
    
    async sendGroupInvite(groupId, userId) {
        try {
            const response = await fetch(`/api/groups/${groupId}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify({ groupId: groupId })
            });
            
            const data = await response.json();
            
            if (data.success) {
                alert('用户已邀请加入群组!');
                // 重新加载群组成员列表
                if (this.currentGroup && this.currentGroup.id === groupId) {
                    await this.loadGroupMembers(groupId);
                }
            } else {
                alert('邀请用户失败: ' + data.message);
            }
        } catch (error) {
            console.error('邀请用户失败:', error);
            alert('邀请用户过程中发生错误');
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
    
    // 开始检查离线消息
    startOfflineMessageCheck() {
        // 清除之前的定时器
        if (this.offlineMessageCheckInterval) {
            clearInterval(this.offlineMessageCheckInterval);
        }
        
        // 每30秒检查一次离线消息
        this.offlineMessageCheckInterval = setInterval(async () => {
            await this.checkOfflineMessages();
        }, 30000); // 30秒
        
        // 登录后立即检查一次离线消息
        this.checkOfflineMessages();
    }
    
    // 检查离线消息
    async checkOfflineMessages() {
        try {
            const response = await fetch('/api/messages/offline', {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            const data = await response.json();
            
            if (data.success && data.data) {
                if (data.data.length > 0) {
                    // 显示离线消息通知
                    this.displaySystemMessage(`您有 ${data.data.length} 条离线消息`);
                    
                    // 显示离线消息
                    data.data.forEach(message => {
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
            }
        } catch (error) {
            console.error('检查离线消息失败:', error);
        }
    }
}

// 带选择框的提示函数
function promptWithSelect(message, options) {
    // 创建一个自定义的模态框
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0,0,0,0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10000;
    `;
    
    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white;
        padding: 20px;
        border-radius: 5px;
        min-width: 300px;
        max-width: 500px;
    `;
    
    const messageEl = document.createElement('p');
    messageEl.textContent = message;
    messageEl.style.marginTop = '0';
    
    const select = document.createElement('select');
    select.style.cssText = `
        width: 100%;
        padding: 8px;
        margin: 10px 0;
        border: 1px solid #ddd;
        border-radius: 4px;
        box-sizing: border-box;
    `;
    
    // 解析选项并添加到下拉框
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = options;
    const tempOptions = tempDiv.querySelectorAll('option');
    tempOptions.forEach(option => {
        select.appendChild(option);
    });
    
    const buttonContainer = document.createElement('div');
    buttonContainer.style.cssText = `
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-top: 15px;
    `;
    
    const confirmBtn = document.createElement('button');
    confirmBtn.textContent = '确定';
    confirmBtn.style.cssText = `
        padding: 8px 16px;
        background-color: #007bff;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
    `;
    
    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = '取消';
    cancelBtn.style.cssText = `
        padding: 8px 16px;
        background-color: #6c757d;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
    `;
    
    buttonContainer.appendChild(cancelBtn);
    buttonContainer.appendChild(confirmBtn);
    
    modalContent.appendChild(messageEl);
    modalContent.appendChild(select);
    modalContent.appendChild(buttonContainer);
    modal.appendChild(modalContent);
    document.body.appendChild(modal);
    
    return new Promise((resolve) => {
        confirmBtn.addEventListener('click', () => {
            document.body.removeChild(modal);
            resolve(select.value);
        });
        
        cancelBtn.addEventListener('click', () => {
            document.body.removeChild(modal);
            resolve(null);
        });
        
        // 点击模态框外部关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                document.body.removeChild(modal);
                resolve(null);
            }
        });
    });
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});