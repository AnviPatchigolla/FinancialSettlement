import java.util.ArrayList;

public class BankManager{
    private ArrayList<Bank> banks;
    private ArrayList<Transaction> transactions;

    public BankManager(){
        banks = new ArrayList<Bank>();
        transactions = new ArrayList<Transaction>();
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
        transactions.add(transaction);
    }

    public ArrayList<Bank> getBanks(){
        return new ArrayList<Bank>(banks);
    }

    public ArrayList<Transaction> getTransactions(){
        return new ArrayList<Transaction>(transactions);
    }

    public void clearTransactions(){
        transactions.clear();
    }
}
