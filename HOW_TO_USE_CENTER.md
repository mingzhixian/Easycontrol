# 易控(Easycontrol)Center服务器搭建说明

## 前期准备
- 一台Linux服务器（没有可通过以下[链接](https://my.racknerd.com/aff.php?aff=5222)购买，支持支付宝付款，价格便宜）
- JAVA环境
```shell
# Debian系统示例安装命令
apt install openjdk-11-jre
# 其他系统请自行搜索“xx系统安装java11环境”，按照网上教程安装
```
- 下载Center-all.jar文件至服务器（项目本身不再提供发行版文件，需手动编译或进入[易控下载群](https://gitee.com/mingzhixianweb/easycontrol/raw/master/pic/other/qq_download.webp))付费下载）

## 运行
- 将Center-all.jar复制到/root/easycontrol_center/目录下
- 执行以下命令运行程序
```shell
java -jar /root/easycontrol_center/center-all.jar
```

## 持久运行（服务）
- 将Center-all.jar复制到/root/easycontrol_center/目录下
- 在目录：/etc/systemd/system/下新建easycontrol_center.service文件，并在文件中填写以下内容：
```
[Unit]
Description=easycontrol_center

[Service]
WorkingDirectory=/root/easycontrol_center
ExecStart=java -jar /root/easycontrol_center/center-all.jar
KillMode=mixed

[Install]
WantedBy=multi-user.target
```
- 使用"systemctl enable easycontrol_center"命令允许开机自启动
- 使用"systemctl start easycontrol_center"命令启动服务
- 使用"systemctl status easycontrol_center"命令查看运行信息包括日志

## 使用
- Center服务启动后监听8866端口，在易控端填入地址时需包括/api/路径，即若服务器地址为1.1.1.1，则易控端填写http://1.1.1.1:8866/api/
- 使用Caddy或Nginx反向代理，推荐使用Caddy，可自动维护证书，方便部署，具体使用方法可查看Caddy文档，以下给出示例Caddy配置文件：
```
{
        storage file_system {
                root /etc/ssl/caddy
        }
}
:443, easycontrol.ex.com {
        tls easycontrol@ex.com
        reverse_proxy  http://localhost:8866 {
                header_up  Host  {upstream_hostport}
        }
}
```