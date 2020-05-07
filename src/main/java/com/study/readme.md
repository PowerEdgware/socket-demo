####测试全连接队列满的情况
1.服务端程序使用serversocket启动端口，接受连接，但不调用serversocket.accept方法：  

查看全连接队列溢出：    

netstat -s | egrep -i listen

```
    30 times the listen queue of a socket overflowed  //发现这个overflowed 一直在增加，那么可以明确的是server上全连接队列一定溢出了
    30 SYNs to LISTEN sockets dropped
````



A.已经进入全连接队列的连接（已完成正常的三次握手），服务端不调用accept
此时客户端发送数据给服务端并等待读取服务的回应数据，服务端内核TCP模块会自动ACK客户端发来的数据，但是由于应用程序
没有回复客户端响应，导致客户端最终读取超时(客户端设置了timeout的情况下)。  

B.全连接队列已满，导致三次握手服务端接收到客户端ACK时，无法放入全连接队列，如果此时内核参数：`tcp_abort_on_overflow=1`
则 服务端直接RST掉该连接，否则，server过一段时间再次发送syn+ack给client（也就是重新走握手的第二步）。
如果Client端等待时间较短，就会出现异常，比如读超时。或者等待服务端重传syn+ack的次数(参数：`net.ipv4.tcp_synack_retries = 2`)耗尽
服务端会发生RST给客户端，此时client会受到：connection reset的错误。

