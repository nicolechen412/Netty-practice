package com.nicole.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class NioDiscardServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NioDiscardServer.class);

    public static void startServer() throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        Selector selector = Selector.open();

        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //只有非阻塞模式才可以注册到selector

        serverSocketChannel.bind(new InetSocketAddress(18899));


        System.out.println("服务器启动成功");
        LOGGER.info("服务器启动成功");

        while(selector.select() > 0){
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                //OP_ACCEPT
                if(selectionKey.isAcceptable()){
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);

                //OP_READ
                } else if(selectionKey.isReadable()){
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int length;
                    while((length = socketChannel.read(byteBuffer)) > 0){
                        byteBuffer.flip();
                        System.out.println("read: " + new String(byteBuffer.array(), 0, length));
                        LOGGER.info("read: {}", new String(byteBuffer.array(), 0, length));
                        byteBuffer.clear();
                    }

                }
                iterator.remove();
            }
        }
        serverSocketChannel.close();



    }

    public static void main(String[] args) throws IOException {
        startServer();
    }

}
