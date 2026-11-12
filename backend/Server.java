import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Server implements HttpHandler{
    private final BankManager manager = new BankManager();
    private final Object managerLock = new Object();
    private final Path frontendDirectory;

    public Server(Path frontendDirectory){
        this.frontendDirectory = frontendDirectory.toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws IOException{
        Path frontend = Paths.get("frontend");

        if(!Files.isDirectory(frontend)){
            frontend = Paths.get("../frontend");
        }

        if(!Files.isDirectory(frontend)){
            throw new IOException("Frontend folder not found.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/", new Server(frontend));
        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        try{
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if(path.equals("/api/banks")){
                handleBanks(exchange, method);
            }else if(path.equals("/api/transactions")){
                handleTransactions(exchange, method);
            }else if(path.equals("/api/settle")){
                handleSettlement(exchange, method);
            }else{
                serveFile(exchange, path, method);
            }
        }catch(IllegalArgumentException e){
            sendJson(exchange, 400, "{\"error\":" + quote(e.getMessage()) + "}");
        }catch(Exception e){
            sendJson(exchange, 500, "{\"error\":\"Server error.\"}");
        }
    }

    private void handleBanks(HttpExchange exchange, String method) throws IOException{
        if(method.equals("GET")){
            StringBuilder json = new StringBuilder("{\"banks\":[");

            synchronized(managerLock){
                ArrayList<Bank> banks = manager.getBanks();

                for(int i = 0; i < banks.size(); i++){
                    if(i > 0){
                        json.append(',');
                    }
                    json.append(quote(banks.get(i).getName()));
                }
            }

            json.append("]}");
            sendJson(exchange, 200, json.toString());
        }else if(method.equals("POST")){
            HashMap<String, String> values = parseObject(readBody(exchange));
            String name = required(values, "name");
            double balance = number(values, "balance");

            synchronized(managerLock){
                manager.addBank(name, balance);
            }

            sendJson(exchange, 201, "{\"message\":\"Bank added.\"}");
        }else{
            methodNotAllowed(exchange, "GET, POST");
        }
    }

    private void handleTransactions(HttpExchange exchange, String method) throws IOException{
        if(method.equals("GET")){
            StringBuilder json = new StringBuilder("{\"transactions\":[");

            synchronized(managerLock){
                ArrayList<Transaction> transactions = manager.getTransactions();

                for(int i = 0; i < transactions.size(); i++){
                    if(i > 0){
                        json.append(',');
                    }

                    Transaction transaction = transactions.get(i);
                    json.append("{\"sender\":").append(quote(transaction.getSender().getName()));
                    json.append(",\"receiver\":").append(quote(transaction.getReceiver().getName()));
                    json.append(",\"amount\":").append(transaction.getAmount()).append('}');
                }
            }

            json.append("]}");
            sendJson(exchange, 200, json.toString());
        }else if(method.equals("POST")){
            HashMap<String, String> values = parseObject(readBody(exchange));
            String sender = required(values, "sender");
            String receiver = required(values, "receiver");
            double amount = number(values, "amount");

            synchronized(managerLock){
                manager.addTransaction(sender, receiver, amount);
            }

            sendJson(exchange, 201, "{\"message\":\"Transaction added.\"}");
        }else{
            methodNotAllowed(exchange, "GET, POST");
        }
    }

    private void handleSettlement(HttpExchange exchange, String method) throws IOException{
        if(!method.equals("POST")){
            methodNotAllowed(exchange, "POST");
            return;
        }

        NetSettlement settlement = new NetSettlement();

        synchronized(managerLock){
            settlement.settle(manager);
        }

        StringBuilder json = new StringBuilder("{\"netPositions\":[");
        ArrayList<String> names = settlement.getBankNames();
        ArrayList<Double> amounts = settlement.getNetAmounts();

        for(int i = 0; i < names.size(); i++){
            if(i > 0){
                json.append(',');
            }
            json.append("{\"bank\":").append(quote(names.get(i))).append(",\"amount\":").append(amounts.get(i)).append('}');
        }

        json.append("],\"transfers\":[");
        ArrayList<String> senders = settlement.getTransferSenders();
        ArrayList<String> receivers = settlement.getTransferReceivers();
        ArrayList<Double> transferAmounts = settlement.getTransferAmounts();

        for(int i = 0; i < senders.size(); i++){
            if(i > 0){
                json.append(',');
            }
            json.append("{\"sender\":").append(quote(senders.get(i)));
            json.append(",\"receiver\":").append(quote(receivers.get(i)));
            json.append(",\"amount\":").append(transferAmounts.get(i)).append('}');
        }

        json.append("],\"totalBefore\":").append(settlement.getTotalBefore());
        json.append(",\"totalAfter\":").append(settlement.getTotalAfter());
        json.append(",\"reductionPercentage\":").append(settlement.getReductionPercentage()).append('}');
        sendJson(exchange, 200, json.toString());
    }

    private void serveFile(HttpExchange exchange, String path, String method) throws IOException{
        if(!method.equals("GET")){
            methodNotAllowed(exchange, "GET");
            return;
        }

        String fileName;
        String contentType;

        if(path.equals("/") || path.equals("/index.html")){
            fileName = "index.html";
            contentType = "text/html; charset=utf-8";
        }else if(path.equals("/style.css")){
            fileName = "style.css";
            contentType = "text/css; charset=utf-8";
        }else if(path.equals("/script.js")){
            fileName = "script.js";
            contentType = "application/javascript; charset=utf-8";
        }else{
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not found.");
            return;
        }

        Path file = frontendDirectory.resolve(fileName).normalize();

        if(!file.startsWith(frontendDirectory) || !Files.isRegularFile(file)){
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not found.");
            return;
        }

        sendBytes(exchange, 200, contentType, Files.readAllBytes(file));
    }

    private String readBody(HttpExchange exchange) throws IOException{
        byte[] body = exchange.getRequestBody().readNBytes(8193);

        if(body.length > 8192){
            throw new IllegalArgumentException("Request body is too large.");
        }

        return new String(body, StandardCharsets.UTF_8);
    }

    private HashMap<String, String> parseObject(String json){
        HashMap<String, String> values = new HashMap<String, String>();
        int[] index = {0};
        skipSpaces(json, index);
        expect(json, index, '{');
        skipSpaces(json, index);

        if(index[0] < json.length() && json.charAt(index[0]) == '}'){
            index[0]++;
        }else{
            while(true){
                skipSpaces(json, index);
                String key = parseString(json, index);
                skipSpaces(json, index);
                expect(json, index, ':');
                skipSpaces(json, index);
                String value;

                if(index[0] < json.length() && json.charAt(index[0]) == '"'){
                    value = parseString(json, index);
                }else{
                    int start = index[0];
                    while(index[0] < json.length() && "-+0123456789.eE".indexOf(json.charAt(index[0])) >= 0){
                        index[0]++;
                    }
                    if(start == index[0]){
                        throw new IllegalArgumentException("Invalid JSON value.");
                    }
                    value = json.substring(start, index[0]);
                }

                if(values.put(key, value) != null){
                    throw new IllegalArgumentException("Duplicate JSON field.");
                }

                skipSpaces(json, index);
                if(index[0] < json.length() && json.charAt(index[0]) == ','){
                    index[0]++;
                }else{
                    expect(json, index, '}');
                    break;
                }
            }
        }

        skipSpaces(json, index);
        if(index[0] != json.length()){
            throw new IllegalArgumentException("Unexpected JSON content.");
        }
        return values;
    }

    private String parseString(String text, int[] index){
        expect(text, index, '"');
        StringBuilder value = new StringBuilder();

        while(index[0] < text.length()){
            char current = text.charAt(index[0]++);
            if(current == '"'){
                return value.toString();
            }
            if(current == '\\'){
                if(index[0] >= text.length()){
                    break;
                }
                char escaped = text.charAt(index[0]++);
                if(escaped == '"' || escaped == '\\' || escaped == '/'){
                    value.append(escaped);
                }else if(escaped == 'b'){
                    value.append('\b');
                }else if(escaped == 'f'){
                    value.append('\f');
                }else if(escaped == 'n'){
                    value.append('\n');
                }else if(escaped == 'r'){
                    value.append('\r');
                }else if(escaped == 't'){
                    value.append('\t');
                }else if(escaped == 'u' && index[0] + 4 <= text.length()){
                    try{
                        value.append((char) Integer.parseInt(text.substring(index[0], index[0] + 4), 16));
                    }catch(NumberFormatException e){
                        throw new IllegalArgumentException("Invalid JSON escape.");
                    }
                    index[0] += 4;
                }else{
                    throw new IllegalArgumentException("Invalid JSON escape.");
                }
            }else{
                if(current < 32){
                    throw new IllegalArgumentException("Invalid JSON string.");
                }
                value.append(current);
            }
        }
        throw new IllegalArgumentException("Unclosed JSON string.");
    }

    private void skipSpaces(String text, int[] index){
        while(index[0] < text.length() && Character.isWhitespace(text.charAt(index[0]))){
            index[0]++;
        }
    }

    private void expect(String text, int[] index, char expected){
        if(index[0] >= text.length() || text.charAt(index[0]) != expected){
            throw new IllegalArgumentException("Invalid JSON.");
        }
        index[0]++;
    }

    private String required(HashMap<String, String> values, String key){
        String value = values.get(key);
        if(value == null){
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value;
    }

    private double number(HashMap<String, String> values, String key){
        try{
            double value = Double.parseDouble(required(values, key));
            if(!Double.isFinite(value)){
                throw new IllegalArgumentException("Invalid number: " + key);
            }
            return value;
        }catch(NumberFormatException e){
            throw new IllegalArgumentException("Invalid number: " + key);
        }
    }

    private String quote(String value){
        StringBuilder json = new StringBuilder("\"");
        for(int i = 0; i < value.length(); i++){
            char character = value.charAt(i);
            if(character == '"') json.append("\\\"");
            else if(character == '\\') json.append("\\\\");
            else if(character == '\n') json.append("\\n");
            else if(character == '\r') json.append("\\r");
            else if(character == '\t') json.append("\\t");
            else if(character < 32) json.append(String.format("\\u%04x", (int) character));
            else json.append(character);
        }
        return json.append('"').toString();
    }

    private void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException{
        exchange.getResponseHeaders().set("Allow", allowed);
        sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException{
        sendText(exchange, status, "application/json; charset=utf-8", body);
    }

    private void sendText(HttpExchange exchange, int status, String type, String body) throws IOException{
        sendBytes(exchange, status, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private void sendBytes(HttpExchange exchange, int status, String type, byte[] body) throws IOException{
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
