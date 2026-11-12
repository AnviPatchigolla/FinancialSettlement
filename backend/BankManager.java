import java.util.ArrayList;

public class BankManager{
    private ArrayList<Bank> banks;
    private ArrayList<byte[]> encryptedTransactions;
    private EncryptionUtil encryption;

    public BankManager(){
        banks = new ArrayList<Bank>();
        encryptedTransactions = new ArrayList<byte[]>();
        encryption = new EncryptionUtil();
    }

    public void addBank(String name, double balance){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Bank name cannot be empty.");
        }

        if(findBank(name) != null){
            throw new IllegalArgumentException("Bank name already exists.");
        }

        Bank bank = new Bank(name, balance);
        banks.add(bank);
    }

    public Bank findBank(String name){
        for(Bank bank : banks){
            if(bank.getName().equals(name)){
                return bank;
            }
        }

        return null;
    }

    public void addTransaction(String senderName, String receiverName, double amount){
        Bank sender = findBank(senderName);
        Bank receiver = findBank(receiverName);

        if(sender == null || receiver == null){
            throw new IllegalArgumentException("Both banks must exist.");
        }

        Transaction transaction = new Transaction(sender, receiver, amount);
        double scaledAmount = transaction.getAmount() * 100.0;

        if(!Double.isFinite(scaledAmount) || scaledAmount >= Long.MAX_VALUE || scaledAmount <= Long.MIN_VALUE){
            throw new IllegalArgumentException("Transaction amount is too large.");
        }

        long cents = Math.round(scaledAmount);

        if(transaction.getAmount() != cents / 100.0){
            throw new IllegalArgumentException("Transaction amount cannot be converted to cents.");
        }

        int senderIndex = banks.indexOf(sender);
        int receiverIndex = banks.indexOf(receiver);
        String transactionData = senderIndex + "," + receiverIndex + "," + cents;
        encryptedTransactions.add(encryption.encrypt(transactionData));
    }

    public ArrayList<Bank> getBanks(){
        return new ArrayList<Bank>(banks);
    }

    public ArrayList<Transaction> getTransactions(){
        ArrayList<Transaction> transactions = new ArrayList<Transaction>();

        for(byte[] encryptedData : encryptedTransactions){
            String[] values = encryption.decrypt(encryptedData).split(",", -1);

            if(values.length != 3){
                throw new IllegalArgumentException("Encrypted transaction data is invalid.");
            }

            try{
                int senderIndex = Integer.parseInt(values[0]);
                int receiverIndex = Integer.parseInt(values[1]);
                long cents = Long.parseLong(values[2]);

                if(senderIndex < 0 || senderIndex >= banks.size()
                        || receiverIndex < 0 || receiverIndex >= banks.size()){
                    throw new IllegalArgumentException("Encrypted transaction data is invalid.");
                }

                double amount = cents / 100.0;
                transactions.add(new Transaction(banks.get(senderIndex), banks.get(receiverIndex), amount));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Encrypted transaction data is invalid.", e);
            }
        }

        return transactions;
    }

    public void clearTransactions(){
        encryptedTransactions.clear();
    }
}
