package com.nicole.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioDiscardClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NioDiscardClient.class);


    public static void startClient() throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 18899);
        SocketChannel socketChannel = SocketChannel.open(socketAddress);
        socketChannel.configureBlocking(false);

        while(!socketChannel.finishConnect()){

        }
        System.out.println("连接已经建立");
        LOGGER.info("连接已经建立");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello world".getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        socketChannel.shutdownOutput();
        socketChannel.close();


    }

    public static void main(String[] args) throws IOException {
        startClient();
    }
}
