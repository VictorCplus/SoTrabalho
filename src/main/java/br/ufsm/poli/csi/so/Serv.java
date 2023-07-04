package br.ufsm.poli.csi.so;

import lombok.SneakyThrows;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Serv {

    public static Map<Integer, Assento> assentos = new HashMap<>();

    public static Semaphore mutex = new Semaphore(1);

    public static Logger logger = new Logger();

    @SneakyThrows
    public static void main(String[] args) {
        // Numeros de assentos
        for(int id = 1; id < 256; id++){
            assentos.put(id, new Assento(id));
        }

        try (ServerSocket serv = new ServerSocket(8080)){
            System.out.println("Aguardando conexão na porta 8080");
            while (true){
                // Esperando uma conexão "while True"
                Socket socket = serv.accept();
                // Criando a Thread e iniciando conexão do servidor
                Conexao conexao = new Conexao(socket);
                Thread thread = new Thread(conexao);
                thread.start();
            }
        }
    }
}
