import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class SettlementPrecisionTest{
    public static void main(String[] args){
        testEncryption();
        testEncryptedTransactionStorage();
        testValidation();
        testBalanceAccess();
        testSettlement();
        testDecimalSettlement();
        testInsufficientBalance();
        testLargeBalancePrecision();
        testEmptyAndRepeatedSettlement();
        System.out.println("All settlement tests passed.");
    }

    private static void testEncryption(){
        EncryptionUtil encryption = new EncryptionUtil();
        byte[] first = encryption.encrypt("0,1,500000");
        byte[] second = encryption.encrypt("0,1,500000");

        if(!encryption.decrypt(first).equals("0,1,500000")){
            throw new AssertionError("Encrypted text did not decrypt correctly.");
        }

        if(Arrays.equals(first, second)){
            throw new AssertionError("Each encryption must use a fresh IV.");
        }

        first[first.length - 1] ^= 1;
        expectIllegalArgument(() -> encryption.decrypt(first), "modified encrypted data");
    }

    private static void testEncryptedTransactionStorage(){
        BankManager manager = new BankManager();
        manager.addBank("Bank A", 100);
        manager.addBank("Bank B", 100);
        manager.addTransaction("Bank A", "Bank B", 25.50);

        ArrayList<?> storedTransactions;

        try{
            Field field = BankManager.class.getDeclaredField("encryptedTransactions");
            field.setAccessible(true);
            storedTransactions = (ArrayList<?>) field.get(manager);
        }catch(ReflectiveOperationException e){
            throw new AssertionError("BankManager should store encrypted transactions.", e);
        }

        if(storedTransactions.size() != 1 || !(storedTransactions.get(0) instanceof byte[])){
            throw new AssertionError("BankManager should store transaction ciphertext.");
        }

        byte[] encryptedData = (byte[]) storedTransactions.get(0);
        byte[] plaintext = "0,1,2550".getBytes(StandardCharsets.UTF_8);

        if(Arrays.equals(encryptedData, plaintext)){
            throw new AssertionError("Stored transaction data should not contain plaintext details.");
        }

        Transaction transaction = manager.getTransactions().get(0);
        if(!transaction.getSender().getName().equals("Bank A")
                || !transaction.getReceiver().getName().equals("Bank B")
                || transaction.getAmount() != 25.50){
            throw new AssertionError("Encrypted transaction was not reconstructed correctly.");
        }

        encryptedData[encryptedData.length - 1] ^= 1;
        expectIllegalArgument(() -> manager.getTransactions(), "corrupted stored transaction");
        expectIllegalArgument(() -> new NetSettlement().settle(manager), "settlement with corrupted transaction");
        assertClose(100, manager.findBank("Bank A").getBalance(), "unchanged corrupted sender balance");
        assertClose(100, manager.findBank("Bank B").getBalance(), "unchanged corrupted receiver balance");

        if(storedTransactions.size() != 1){
            throw new AssertionError("Corrupted transactions should not be cleared.");
        }
    }

    private static void testBalanceAccess(){
        Bank bank = new Bank("Balance Bank", 123.45);
        assertClose(123.45, bank.getBalance(), "accessible bank balance");
    }

    private static void testValidation(){
        new Bank("Valid", 100.25);
        expectIllegalArgument(() -> new Bank("Negative", -1), "negative balance");
        expectIllegalArgument(() -> new Bank("NaN", Double.NaN), "NaN balance");
        expectIllegalArgument(() -> new Bank("Infinity", Double.POSITIVE_INFINITY), "infinite balance");
        expectIllegalArgument(() -> new Bank("Precise", 100.001), "balance with more than two decimal places");
        expectIllegalArgument(() -> new Bank("Huge", Double.MAX_VALUE), "extremely large balance");

        Bank sender = new Bank("Sender", 100);
        Bank receiver = new Bank("Receiver", 100);
        new Transaction(sender, receiver, 10.25);
        expectIllegalArgument(() -> new Transaction(sender, receiver, 0), "zero transaction");
        expectIllegalArgument(() -> new Transaction(sender, receiver, -1), "negative transaction");
        expectIllegalArgument(() -> new Transaction(sender, receiver, Double.NaN), "NaN transaction");
        expectIllegalArgument(() -> new Transaction(sender, receiver, Double.POSITIVE_INFINITY), "infinite transaction");
        expectIllegalArgument(() -> new Transaction(sender, receiver, Double.MAX_VALUE), "extremely large transaction");
        expectIllegalArgument(() -> new Transaction(sender, receiver, 10.001), "transaction with more than two decimal places");
        expectIllegalArgument(() -> new Transaction(sender, sender, 10), "self transaction");

        BankManager manager = new BankManager();
        manager.addBank("Bank A", 100);
        expectIllegalArgument(() -> manager.addBank("Bank A", 100), "duplicate bank");
        expectIllegalArgument(() -> manager.addTransaction("Bank A", "Missing", 10), "missing bank");
    }

    private static void testSettlement(){
        BankManager manager = new BankManager();
        manager.addBank("Bank A", 10000);
        manager.addBank("Bank B", 8000);
        manager.addBank("Bank C", 12000);
        manager.addTransaction("Bank A", "Bank B", 5000);
        manager.addTransaction("Bank B", "Bank C", 3000);
        manager.addTransaction("Bank C", "Bank A", 2000);

        NetSettlement settlement = new NetSettlement();
        settlement.settle(manager);

        assertBalance(manager.findBank("Bank A"), 7000, "Bank A balance");
        assertBalance(manager.findBank("Bank B"), 10000, "Bank B balance");
        assertBalance(manager.findBank("Bank C"), 13000, "Bank C balance");
        assertClose(10000, settlement.getTotalBefore(), "total before settlement");
        assertClose(3000, settlement.getTotalAfter(), "total after settlement");
        assertClose(70, settlement.getReductionPercentage(), "reduction percentage");

        if(settlement.getTransferAmounts().size() != 2
                || !settlement.getTransferSenders().get(0).equals("Bank A")
                || !settlement.getTransferReceivers().get(0).equals("Bank B")
                || settlement.getTransferAmounts().get(0) != 2000
                || !settlement.getTransferSenders().get(1).equals("Bank A")
                || !settlement.getTransferReceivers().get(1).equals("Bank C")
                || settlement.getTransferAmounts().get(1) != 1000){
            throw new AssertionError("Unexpected settlement transfer plan.");
        }

        if(!manager.getTransactions().isEmpty()){
            throw new AssertionError("Settled transactions should be cleared.");
        }
    }

    private static void testDecimalSettlement(){
        BankManager manager = new BankManager();
        manager.addBank("Bank A", 100.10);
        manager.addBank("Bank B", 50.20);
        manager.addBank("Bank C", 75.30);
        manager.addTransaction("Bank A", "Bank B", 10.15);
        manager.addTransaction("Bank B", "Bank C", 3.05);
        manager.addTransaction("Bank C", "Bank A", 1.10);

        NetSettlement settlement = new NetSettlement();
        settlement.settle(manager);

        assertClose(91.05, manager.findBank("Bank A").getBalance(), "decimal Bank A balance");
        assertClose(57.30, manager.findBank("Bank B").getBalance(), "decimal Bank B balance");
        assertClose(77.25, manager.findBank("Bank C").getBalance(), "decimal Bank C balance");
        assertClose(14.30, settlement.getTotalBefore(), "decimal total before settlement");
        assertClose(9.05, settlement.getTotalAfter(), "decimal total after settlement");
        assertClose(36.71, settlement.getReductionPercentage(), "decimal reduction percentage");
    }

    private static void testInsufficientBalance(){
        BankManager manager = new BankManager();
        manager.addBank("Bank A", 100);
        manager.addBank("Bank B", 100);
        manager.addTransaction("Bank A", "Bank B", 150);

        expectIllegalArgument(() -> new NetSettlement().settle(manager), "insufficient balance");
        assertBalance(manager.findBank("Bank A"), 100, "unchanged Bank A balance");
        assertBalance(manager.findBank("Bank B"), 100, "unchanged Bank B balance");

        if(manager.getTransactions().size() != 1){
            throw new AssertionError("Failed settlement should keep pending transactions.");
        }
    }

    private static void testLargeBalancePrecision(){
        double largeBalance = 140737488355328.0;
        BankManager manager = new BankManager();
        manager.addBank("Payer", 1);
        manager.addBank("Receiver", largeBalance);
        manager.addTransaction("Payer", "Receiver", 0.02);

        expectIllegalArgument(() -> new NetSettlement().settle(manager), "unrepresentable balance change");
        assertClose(1, manager.findBank("Payer").getBalance(), "unchanged large-settlement payer balance");
        assertClose(largeBalance, manager.findBank("Receiver").getBalance(), "unchanged large receiver balance");

        if(manager.getTransactions().size() != 1){
            throw new AssertionError("Failed large settlement should keep pending transactions.");
        }
    }

    private static void testEmptyAndRepeatedSettlement(){
        BankManager manager = new BankManager();
        manager.addBank("Bank A", 100);
        manager.addBank("Bank B", 100);
        manager.addTransaction("Bank A", "Bank B", 25);

        NetSettlement settlement = new NetSettlement();
        settlement.settle(manager);
        settlement.settle(manager);

        assertClose(0, settlement.getTotalBefore(), "empty total before settlement");
        assertClose(0, settlement.getTotalAfter(), "empty total after settlement");
        assertBalance(manager.findBank("Bank A"), 75, "repeated Bank A balance");
        assertBalance(manager.findBank("Bank B"), 125, "repeated Bank B balance");
    }

    private static void expectIllegalArgument(Runnable action, String label){
        try{
            action.run();
            throw new AssertionError("Expected rejection for " + label + ".");
        }catch(IllegalArgumentException expected){
        }
    }

    private static void assertClose(double expected, double actual, String label){
        if(Math.abs(expected - actual) > 0.000000001){
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertBalance(Bank bank, double expected, String label){
        assertClose(expected, bank.getBalance(), label);
    }
}
