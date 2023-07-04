package br.ufsm.poli.csi.so;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class Logger {

    private String logString = "";

    private Semaphore vazio = new Semaphore(1000);

    private Semaphore cheio = new Semaphore(0);

    private Semaphore mutex = new Semaphore(1);

    private File file = new File("Log.txt");

    private Socket socket;

    private Assento assento;

    @SneakyThrows
    public Logger(){
        if(file.createNewFile()){
            System.out.println("Arquivo log Criado: " + this.file.getName());
        }
    }

    public void log(Socket socket, Assento assento){
        this.socket = socket;
        this.assento = assento;

        Thread produz = new Thread(new ProduzLog());
        Thread consome = new Thread(new ConsomeLog());

        produz.start();
        consome.start();

    }
    // Produtores e Consumidores
    private class ProduzLog implements Runnable {

        @Override
        @SneakyThrows
        public void run(){
            mutex.acquire();
            logString = "\nNova reserva\n";
            logString += "Ip: " + socket.getInetAddress().toString() + "\n";
            logString += "Nome: " + assento.getNome() + "\n";
            logString += "Número do Assento: " + assento.getId() + "\n";
            logString += "Data e Hora: " + assento.getData() + " " + assento.getHora();
            logString += "\n";

            vazio.acquire(logString.length());
            cheio.release();
            mutex.release();
        }
    }

    private class ConsomeLog implements Runnable{

        @Override
        @SneakyThrows
        public void run(){
            mutex.acquire();
            cheio.acquire();

            vazio.release(logString.length());
            // Inicia o arquivo de log
            FileWriter writer = new FileWriter(file.getName(), true);
            // Escreve no arquivo de log
            writer.write(logString);
            // Fecha o arquivo de log
            writer.close();

            mutex.release();
        }
    }
}
