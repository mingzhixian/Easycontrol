# 易控(Easycontrol)使用说明

## 相关文件或链接
1. 视频教程：
	1. [视频1](https://www.bilibili.com/video/BV1Wu4y1u7vD/)
	2. [视频2](https://www.bilibili.com/video/BV11a4y1d7EU/)
	3. [视频3](https://www.bilibili.com/video/BV1Wa4y197tF/)
2. 电脑ADB文件：[蓝奏云](https://cloudstar.lanzoue.com/iAWKJ128mnif) 密码:scrcpy
3. 易控软件：新版本不再免费提供安装包，需前往下载群付费下载(10元)，或下载源码手动编译，[下载群](https://gitee.com/mingzhixianweb/easycontrol/raw/master/pic/other/qq_download.webp)

## 被控端准备
1. 被控端手机连续点击关于手机-版本号，直至提示打开开发者选项
2. 开发者选项打开USB调试
3. 电脑下载解压ADB文件
4. 被控端手机连接电脑
5. 电脑下载ADB文件并解压：[蓝奏云](https://cloudstar.lanzoue.com/iAWKJ128mnif) 密码:scrcpy
6. 在ADB所在文件夹按住Shift+右击打开命令行
6. 执行以下命令开启无线调试：
// root用户（永久）：
``` shell
adb shell
<回车>
su
<回车>
echo "sleep 10 && setprop service.adb.tcp.port 5555 && stop adbd && start adbd" > /data/adb/service.d/adb.sh && chmod +x /data/adb/service.d/adb.sh
<回车>
<重启>
```
// 无root（每次重启后需重复操作一次）：
``` shell
adb tcpip 5555
<回车>
```
7. 网络环境拥有防火墙或使用内网穿透的用户，请注意放通被控端ADB端口以及端口号+1端口，例如，上面开启无线调试时设置了ADB端口为5555，则需放行5555端口以及5556端口

## 软件使用
1. 简单使用
	- 主控端安装易控，打开软件进行悬浮窗授权，添加设备（地址为被控端地址加ADB端口号），点击添加后的设备，被控端同意永久调试，开始投屏
	- 设备地址格式：
		- IPv4：192.168.43.1:5555
		- IPv6：[2408:8462:2510:1e05:c39:3262:632d:1a3d]:5555
		- 域名：ex.com:5555
2. 高级使用
	- 在添加设备时设置高级选项，可自定义编解码参数、开启自动修改被控端分辨率等
	- 设置中可设置编码参数等默认选项，添加设备时默认使用这些参数，如对单个设备进行了修改，则以该设备参数为准
	- 特殊地址标识符：
		- 网关地址：\*gateway\*，如网关为192.168.43.1，则“\*gateway\*:5555”表示“192.168.43.1:5555”
		- 子网地址：\*netAddress\*，如子网为192.168.43.0/24, 则“\*netAddress\*.1:5555”表示“192.168.43.1:5555”
3. 工具栏
	- 在小窗模式下点击上横条，可查看工具栏
	- 在全屏模式下点击导航栏更多按钮，可查看工具栏
4. 小窗模式使用
	- 可通过拖动横条移动小窗
	- 拖动右下角可更改小窗大小
5. 最小化模式使用
	- 可上下拖动
	- 单击返回小窗模式
6. 全屏模式使用
	- 底部为导航栏，可向被控端发送多任务、桌面、返回按键
	- 导航栏左边旋转按钮，可以控制主控端页面旋转，当设置中开启主控端自动旋转时，会根据手机方向自动旋转主控端页面
	- 在设置中开启被控端方向跟随功能后，当主控端页面旋转时，会请求被控端同样旋转，以保持同步，旋转不强制，具体看被控端当前应用是否允许旋转
7. 扩展使用
	易控支持在外部使用广播控制，广播地址为："top.saymzx.easycontrol.app.CONTROL"，需要向意向也就是Intent填入想要做的动作：
	- 启动默认设备：
		action：startDefault
	- 启动目标设备：
		action：start
		uuid：设备ID
	- 目标设备变成小窗：
		action：changeToSmall
		uuid：设备ID
	- 目标设备最小化：
		action：changeToMini
		uuid：设备ID
	- 目标设备全屏：
		action：changeToFull
		uuid：设备ID
	- 目标设备关闭投屏：
		action：close
		uuid：设备ID
