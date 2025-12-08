-- 创建数据库
CREATE DATABASE IF NOT EXISTS chatroom;

-- 创建用户并设置密码
CREATE USER 'chatroom_user'@'localhost' IDENTIFIED BY 'chatroom_pass';

-- 授予权限
GRANT ALL PRIVILEGES ON chatroom.* TO 'chatroom_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
