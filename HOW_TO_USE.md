# 易控(Easycontrol)使用说明

## 相关文件或链接
1. 视频教程：
	1. [视频1](https://www.bilibili.com/video/BV1Wu4y1u7vD/)
	2. [视频2](https://www.bilibili.com/video/BV11a4y1d7EU/)
	3. [视频3](https://www.bilibili.com/video/BV1Wa4y197tF/)
2. 易控软件：限于成本不再免费提供安装包，需捐赠35元及以上后前往下载群下载，或下载源码手动编译
	<img src="https://gitee.com/mingzhixianweb/easycontrol/raw/master/pic/other/wechat.webp" width="200px">
	<img src="https://gitee.com/mingzhixianweb/easycontrol/raw/master/pic/other/alipay.webp" width="200px">
	<img src="https://gitee.com/mingzhixianweb/easycontrol/raw/master/pic/other/qq_download.webp" width="200px">

## 初次使用
1. 被控端手机连续点击关于手机-版本号，直至提示打开开发者选项
2. 开发者选项打开USB调试，打开停用adb授权超时功能

## 软件使用
1. 简单使用-有线连接
	1. 主控端安装易控，打开软件进行悬浮窗授权
	2. 利用数据线将主控端与被控端连接，主控端易控界面允许易控访问设备
	3. 点击主控端易控列表中第一行出现的新设备
	4. 被控端授权允许主控端连接（请勾选一律允许）
	
2. 简单使用-无线连接
	1. 主控端安装易控，打开软件进行悬浮窗授权
	2. 确保主控端能够访问被控端（例如在一个wifi下面）
	3. 打开被控端无线调试（并非开发者选项中的无线调试），可使用上面有线连接，随后长按设备点击“打开无线”按钮实现
	4. 主控端易控界面点击右上角添加设备，在设备地址处输入被控端地址，地址格式：
		- IPv4：192.168.43.1:5555
		- IPv6：[2408:8462:2510:1e05:c39:3262:632d:1a3d]:5555
		- 域名：ex.com:5555
	5. 点击刚添加的新设备，被控端授权允许主控端连接（请勾选一律允许）

3. 界面使用
	1. 工具栏
		- 在小窗模式下点击上横条，可查看工具栏
		- 在全屏模式下点击导航栏更多按钮，可查看工具栏
	2. 小窗模式
		- 可通过拖动横条移动小窗
		- 拖动右下角可更改小窗大小
	3. 最小化模式
		- 可上下拖动
		- 单击返回小窗模式
	4. 全屏模式
		- 底部为导航栏，可向被控端发送多任务、桌面、返回按键
		- 导航栏左边旋转按钮，可以控制被控端页面旋转，仅请求旋转，实际是否旋转看被控端当前应用是否允许
		- 全屏页面方向跟随手机重力方向（可在工具栏中锁定）

3. 高级使用
	- 在添加设备时或长按设备点击“修改”按钮，设置高级选项，可自定义编解码参数、投屏控制参数等
	- 设置中可设置编码参数等默认选项，添加设备时默认使用这些参数，如对单个设备进行了修改，则以该设备参数为准
	- 特殊地址标识符，在添加设备时可用于代表特殊地址：
		- 网关地址：\*gateway\*，如网关为192.168.43.1，则“\*gateway\*:5555”表示“192.168.43.1:5555”
		- 子网地址：\*netAddress\*，如子网为192.168.43.0/24, 则“\*netAddress\*.1:5555”表示“192.168.43.1:5555”
	
4. 扩展使用
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
