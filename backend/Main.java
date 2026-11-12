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
            System.out.println(transaction.getSender().getName() + " pays "
                    + transaction.getReceiver().getName() + ": " + transaction.getAmount());
        }

        Bank bankA = manager.findBank("Bank A");
        Bank bankB = manager.findBank("Bank B");
        Bank bankC = manager.findBank("Bank C");

        Settlement settlement = new NetSettlement();

        System.out.println("\nFirst settlement:");
        settlement.settle(manager);

        boolean bankABalanceCorrect = bankA.canPay(7000) && !bankA.canPay(7000.01);
        boolean bankBBalanceCorrect = bankB.canPay(10000) && !bankB.canPay(10000.01);
        boolean bankCBalanceCorrect = bankC.canPay(13000) && !bankC.canPay(13000.01);

        System.out.println("Final balance checks:");
        System.out.println("Bank A = 7000: " + bankABalanceCorrect);
        System.out.println("Bank B = 10000: " + bankBBalanceCorrect);
        System.out.println("Bank C = 13000: " + bankCBalanceCorrect);

        System.out.println("\nSecond settlement:");
        settlement.settle(manager);
    }
}
