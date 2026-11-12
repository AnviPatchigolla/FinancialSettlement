import java.util.ArrayList;
import java.util.Scanner;

public class ConsoleApp{
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        BankManager manager = new BankManager();
        boolean running = true;

        while(running){
            System.out.println("\n===== BANK SETTLEMENT SYSTEM =====\n");
            System.out.println("1. Add Bank");
            System.out.println("2. View Banks");
            System.out.println("3. Add Transaction");
            System.out.println("4. View Transactions");
            System.out.println("5. Run Settlement");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            try{
                int choice = Integer.parseInt(scanner.nextLine());

                switch(choice){
                    case 1:
                        System.out.print("Enter bank name: ");
                        String bankName = scanner.nextLine();
                        System.out.print("Enter initial balance: ");
                        double balance = Double.parseDouble(scanner.nextLine());
                        manager.addBank(bankName, balance);
                        System.out.println("Bank added successfully.");
                        break;

                    case 2:
                        ArrayList<Bank> banks = manager.getBanks();

                        if(banks.isEmpty()){
                            System.out.println("No banks registered.");
                        }else{
                            System.out.println("Registered banks:");

                            for(Bank bank : banks){
                                System.out.println(bank.getName());
                            }
                        }
                        break;

                    case 3:
                        System.out.print("Enter sender bank: ");
                        String senderName = scanner.nextLine();
                        System.out.print("Enter receiver bank: ");
                        String receiverName = scanner.nextLine();
                        System.out.print("Enter amount: ");
                        double amount = Double.parseDouble(scanner.nextLine());
                        manager.addTransaction(senderName, receiverName, amount);
                        System.out.println("Transaction added successfully.");
                        break;

                    case 4:
                        ArrayList<Transaction> transactions = manager.getTransactions();

                        if(transactions.isEmpty()){
                            System.out.println("No pending transactions.");
                        }else{
                            System.out.println("Pending transactions:");

                            for(Transaction transaction : transactions){
                                System.out.println(transaction.getSender().getName() + " pays "
                                        + transaction.getReceiver().getName() + ": " + transaction.getAmount());
                            }
                        }
                        break;

                    case 5:
                        Settlement settlement = new NetSettlement();
                        settlement.settle(manager);
                        break;

                    case 6:
                        running = false;
                        System.out.println("Program closed.");
                        break;

                    default:
                        System.out.println("Error: Choose a number from 1 to 6.");
                }
            }catch(NumberFormatException e){
                System.out.println("Error: Enter a valid number.");
            }catch(IllegalArgumentException e){
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }
}
