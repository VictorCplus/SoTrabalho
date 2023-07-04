package br.ufsm.poli.csi.so;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// Inicia o Runnable a cada nova requisição
public class Conexao implements Runnable {

    private Socket socket;


    public Conexao(Socket socket) {
        this.socket = socket;
    }


    @Override
    @SneakyThrows
    public void run() {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        // Scanner que lê o InputStream
        Scanner scanner = new Scanner(in);

        if (!scanner.hasNext()) {
            scanner.close();
            return;
        }

        String metodo = scanner.next();
        String caminho = scanner.next();
        // Exibe o caminho do método
        System.out.println(metodo + " " + caminho);

        String[] dirAndParams = caminho.split("\\?");
        String recurso = dirAndParams[0];
        // Queries interpretadas na URL
        Map<String, String> query = this.parseQuery(dirAndParams.length > 1
                ? dirAndParams[1].split("&") : null);
        byte[] contBytes = new byte[0];
        // Cabeçalho
        String header = """
                HTTP/1.1 200 OK
                Content-Type: text/html; charset=UTF-8
                """;

        if (recurso.equals("/")) {
            contBytes = this.getBytes("index.html");

            String html = new String(contBytes);
            String elements = "";
            int num = 1;

            for (Assento assento : Serv.assentos.values()) {

                String element = "";
                //Se o assento estiver ocupado = True, desabilitanto o assento.
                if (assento.isReservado()) {
                    element += "<a href=\"#\">";
                    element += "<abbr title=\"" +
                            "Nome: " + assento.getNome();
                    element += "  " +
                            "Data: " + assento.getData();
                    element += "  " +
                            "Hora: " + assento.getHora();
                    element += "\">";
                    element += "<button type=\"button\" class=\"btn btn-danger disabled\">";
                    element += assento.getId() + "</button></abbr></a>";
                } else {
                    element += "<a class\"assento\"";
                    element += " href=\"/reservar?id=" + assento.getId() + "\"";
                    element += "><button type=\"button\" class=\"btn btn-success\">" + assento.getId() + "</button></a>";
                }

                if (num % 15 == 0) {
                    // Organiza as linhas
                    element += "<br/>";
                }

                elements += element + "\n";
                num++;

            }
        // Subistituí os assentos gerados na tag assento no html
            html = html.replace("<assento />", elements);
            contBytes = html.getBytes();
        }
        if (recurso.equals("/reservar")) {
            contBytes = this.getBytes("reservar.html");

            String html = new String(contBytes);
            html = html.replace("{{id}}", query.get("id"));
            contBytes = html.getBytes();
        }

        if (recurso.equals("/confirmar")) {
            // Header de redirecionamento
            header = """
                    HTTP/1.1 302 Found
                    Content-Type: text/html; charset=UTF-8
                    Location: /
                    
                    """;
            // Fecha entrada ao acessar a região crítica
            Serv.mutex.acquire();
            int id = Integer.parseInt(query.get("id"));
            Assento assento = Serv.assentos.get(id);
            // Verefica se o assento está vago
            if (assento != null) {
                String nome = query.get("nome");
                String dataHora[] = query.get("data_hora").split("T");
                String data = dataHora[0];
                String hora = dataHora[1];

                assento.setNome(nome);
                assento.setData(data);
                assento.setHora(hora);
                assento.setReservado(true);

                Serv.logger.log(socket,assento);
                System.out.println("LOG informa: Nova reserva adicionada: "
                        + assento.getId() + " Nome: " + assento.getNome());
            }
            // Libera o mutex
            Serv.mutex.release();
        }
        // Escreve o cabeçalho
        out.write(header.toString().getBytes());
        out.write(contBytes);
        // Encerra as streams
        in.close();
        out.close();

        scanner.close();
        // Encerra a conexão
        this.socket.close();
    }

    @SneakyThrows
    private Map<String, String> parseQuery(String[] query) {

        if (query == null)
            return null;

        Map<String, String> queries = new HashMap<>();

        for (String s : query) {

            String[] vPair = s.split("=");

            if (vPair.length == 1) {
                queries.put(vPair[0], null);
            } else {
                queries.put(vPair[0], URLDecoder.decode(vPair[1], "UTF-8"));
            }
        }
        return queries;
    }

    @SneakyThrows
    private byte[] getBytes(String recurso) {
    // Remove o / do recurso recebido
        if (recurso.startsWith("/"))
            recurso = recurso.substring(1);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(recurso);

        if (is != null)
            return is.readAllBytes();

        return null;
    }
}

