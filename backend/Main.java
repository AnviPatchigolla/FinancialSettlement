public class Main{
    public static void main(String[] args){
        BankManager manager = new BankManager();

        manager.addBank("Bank A", 10000);
        manager.addBank("Bank B", 8000);
        manager.addBank("Bank C", 12000);

        manager.addTransaction("Bank A", "Bank B", 5000);
        manager.addTransaction("Bank B", "Bank C", 3000);
        manager.addTransaction("Bank C", "Bank A", 2000);

        System.out.println("Transactions before settlement:");

        for(Transaction transaction : manager.getTransactions()){
            System.out.printf("%s pays %s: %.2f%n", transaction.getSender().getName(),
                    transaction.getReceiver().getName(), transaction.getAmount());
        }

        Bank bankA = manager.findBank("Bank A");
        Bank bankB = manager.findBank("Bank B");
        Bank bankC = manager.findBank("Bank C");

        Settlement settlement = new NetSettlement();

        System.out.println("\nFirst settlement:");
        settlement.settle(manager);

        System.out.println("Final balances:");
        System.out.printf("Bank A: %.2f%n", bankA.getBalance());
        System.out.printf("Bank B: %.2f%n", bankB.getBalance());
        System.out.printf("Bank C: %.2f%n", bankC.getBalance());

        System.out.println("\nSecond settlement:");
        settlement.settle(manager);
    }
}
