package com.nicole.nio;

import com.nicole.NioDemoConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NioReceiveServer {

    //接受文件路径
    private static final String RECEIVE_PATH = NioDemoConfig.SOCKET_RECEIVE_PATH;

    private ByteBuffer buffer
            = ByteBuffer.allocate(NioDemoConfig.SERVER_BUFFER_SIZE);
    private Charset charset = Charset.forName("UTF-8");

    //使用Map保存每个客户端传输，当OP_READ通道可读时，根据channel找到对应的对象
    Map<SelectableChannel, Session> clientMap = new HashMap<SelectableChannel, Session>();

    public void startServer()  throws IOException{

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(18899));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while(selector.select() > 0){

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while(iterator.hasNext()){

                SelectionKey key = iterator.next();

                if(key.isAcceptable()){
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    Session session = new Session();
                    session.remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                    clientMap.put(socketChannel, session);
                    System.out.println("连接成功 ： " + session.remoteAddress );

                } else if(key.isReadable()){
                    processData(key);
                }
                iterator.remove();

            }

        }
    }

    private void processData(SelectionKey key) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        Session session = clientMap.get(socketChannel);

        int num = 0;
        buffer.clear();

        while((num = socketChannel.write(buffer)) > 0){

            buffer.flip();

            if(session.fileName==null){

                if(buffer.remaining() < 4) {
                    continue;
                } else {
                    //fileNameLength
                    int fileNameLength = buffer.getInt();
                    //fileName
                    byte[] fileNameBytes = new byte[fileNameLength];

                    if(buffer.remaining() < fileNameBytes.length){
                        continue;
                    } else {

                    }

                    buffer.get(fileNameBytes);
                    session.fileName = new String(fileNameBytes,charset);
                    //directory
                    File directory = new File(RECEIVE_PATH);
                    if(!directory.exists()){
                        directory.mkdir();
                    }
                    System.out.println("传输目标目录： " + directory);
                    //file
                    String fullName = directory.getAbsolutePath() + File.separator + session.fileName;
                    System.out.println("传输目标文件： " + fullName);
                    File file = new File(fullName.trim());
                    if(!file.exists()){
                        file.createNewFile();
                    }
                    FileChannel fileChannel = new FileOutputStream(file).getChannel();
                    session.fileChannel = fileChannel;

                    //fileLength
                    if(buffer.remaining() < 8){
                        continue;
                    }
                    long fileLength = buffer.getLong();
                    session.fileLength = fileLength;
                    System.out.println("传输开始：");
                    session.receiveLength += buffer.capacity();
                    //write file
                    if(buffer.remaining() > 0){
                        session.fileChannel.write(buffer);
                    }
                    if(session.isFinished()){
                        finished(key,session);

                    }
                    buffer.clear();
                }

            } else {
                session.receiveLength += buffer.capacity();
                if(buffer.remaining() > 0){
                    session.fileChannel.write(buffer);
                }
                if(session.isFinished()){
                    finished(key,session);

                }
                buffer.clear();
            }

        }
        key.cancel();
        if(num == -1){
            finished(key, session);
            buffer.clear();
        }


    }

    private void finished(SelectionKey key, Session session) throws IOException {

        key.cancel();
        session.fileChannel.close();
    }

    public static void main(String[] args) throws IOException {

        NioReceiveServer server = new NioReceiveServer();
        server.startServer();

    }

    /**
     * 服务器端保存的客户端对象，对应一个客户端文件
     */
    static class Session {
        int step = 1; //1 读取文件名称的长度，2 读取文件名称  ，3 ，读取文件内容的长度， 4 读取文件内容
        //文件名称
        String fileName = null;

        //长度
        long fileLength;
        int fileNameLength;

        //开始传输的时间
        long startTime;

        //客户端的地址
        InetSocketAddress remoteAddress;

        //输出的文件通道
        FileChannel fileChannel;

        //接收长度
        long receiveLength;

        public boolean isFinished() {
            return receiveLength >= fileLength;
        }
    }
}


