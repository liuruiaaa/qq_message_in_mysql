# Go-Mirai-Client 0.2.8 和后端的源码，源码已经可以入库了

Go-Mirai-Client 0.2.8 是一个基于 Golang 编写的 Mirai 客户端。它支持使用 HTTP 和 WebSocket 协议与 Mirai API HTTP 插件进行通信，以便于开发 QQ 机器人等应用。

## 客户端

Go-Mirai-Client 0.2.8 的客户端程序为 `Go-Mirai-Client_0.2.8_windows_amd64.exe`，可以在 Windows 64 位操作系统上运行。该客户端提供了以下功能：

- 连接 Mirai API HTTP 插件，通过 HTTP 或 WebSocket 协议发送和接收消息。
- 配置 Mirai API HTTP 插件的连接参数，包括主机地址、端口号、AuthKey 等。
- 支持对消息进行加密和解密，保护消息的机密性。
- 支持对消息进行压缩和解压缩，减小消息的传输体积。
- 支持在控制台输出日志，方便调试和问题排查。

## 代码

Go-Mirai-Client 0.2.8 的源代码托管在 GitHub 上，您可以通过以下链接进行访问：

https://github.com/ProtobufBot/Go-Mirai-Client

该代码库包含了 Go-Mirai-Client 0.2.8 的全部源代码，以及一些示例程序和文档说明。您可以通过克隆代码库或者下载源代码压缩包来获取代码。

代码库中的主要目录结构如下：

- `client`：客户端程序的源代码。
- `examples`：示例程序的源代码和配置文件。
- `docs`：文档说明和使用指南。

在使用代码时，您需要先设置好 Golang 的开发环境，并且安装相关的依赖库。然后，您可以使用 Go 工具进行编译、打包和安装，或者直接使用已编译好的二进制文件进行运行。具体操作方法请参考代码库中的文档说明。
