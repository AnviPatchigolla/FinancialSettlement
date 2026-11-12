import java.util.ArrayList;

public class NetSettlement extends Settlement{
    private ArrayList<String> bankNames = new ArrayList<String>();
    private ArrayList<Double> netAmounts = new ArrayList<Double>();
    private ArrayList<String> transferSenders = new ArrayList<String>();
    private ArrayList<String> transferReceivers = new ArrayList<String>();
    private ArrayList<Double> transferAmounts = new ArrayList<Double>();
    private double totalBefore;
    private double totalAfter;
    private double reductionPercentage;

    public ArrayList<String> getBankNames(){
        return new ArrayList<String>(bankNames);
    }

    public ArrayList<Double> getNetAmounts(){
        return new ArrayList<Double>(netAmounts);
    }

    public ArrayList<String> getTransferSenders(){
        return new ArrayList<String>(transferSenders);
    }

    public ArrayList<String> getTransferReceivers(){
        return new ArrayList<String>(transferReceivers);
    }

    public ArrayList<Double> getTransferAmounts(){
        return new ArrayList<Double>(transferAmounts);
    }

    public double getTotalBefore(){
        return totalBefore;
    }

    public double getTotalAfter(){
        return totalAfter;
    }

    public double getReductionPercentage(){
        return reductionPercentage;
    }

    @Override
    public void settle(BankManager manager){
        ArrayList<Bank> banks = manager.getBanks();
        ArrayList<Transaction> transactions = manager.getTransactions();
        ArrayList<String> localBankNames = new ArrayList<String>();
        ArrayList<Double> localNetAmounts = new ArrayList<Double>();
        ArrayList<String> localTransferSenders = new ArrayList<String>();
        ArrayList<String> localTransferReceivers = new ArrayList<String>();
        ArrayList<Double> localTransferAmounts = new ArrayList<Double>();
        ArrayList<Double> netPositions = new ArrayList<Double>();
        ArrayList<Double> remainingPositions = new ArrayList<Double>();
        double localTotalBefore = 0.0;
        double localTotalAfter = 0.0;
        double localReductionPercentage = 0.0;

        for(Transaction transaction : transactions){
            localTotalBefore += transaction.getAmount();
        }
        localTotalBefore = roundToTwoDecimals(localTotalBefore);

        System.out.println("Net positions:");

        for(Bank bank : banks){
            double incoming = 0.0;
            double outgoing = 0.0;

            for(Transaction transaction : transactions){
                if(transaction.getReceiver() == bank){
                    incoming += transaction.getAmount();
                }

                if(transaction.getSender() == bank){
                    outgoing += transaction.getAmount();
                }
            }

            double netPosition = incoming - outgoing;
            netPosition = roundToTwoDecimals(netPosition);
            netPositions.add(netPosition);
            remainingPositions.add(netPosition);
            localBankNames.add(bank.getName());
            localNetAmounts.add(netPosition);
            System.out.printf("%s: %.2f%n", bank.getName(), netPosition);
        }

        double netPositionSum = 0.0;

        for(double netPosition : netPositions){
            netPositionSum += netPosition;
        }

        netPositionSum = roundToTwoDecimals(netPositionSum);

        if(netPositionSum != 0.0){
            throw new IllegalArgumentException("Rounded net positions must sum to zero.");
        }

        int bankIndex = 0;

        for(Bank bank : banks){
            double netPosition = netPositions.get(bankIndex);

            if(netPosition < 0 && !bank.canPay(-netPosition)){
                throw new IllegalArgumentException(bank.getName() + " has insufficient funds.");
            }

            if(netPosition > 0 && !bank.canReceive(netPosition)){
                throw new IllegalArgumentException(bank.getName() + " cannot receive funds.");
            }

            bankIndex++;
        }

        int payerIndex = 0;
        int receiverIndex = 0;

        while(payerIndex < banks.size() && receiverIndex < banks.size()){
            while(payerIndex < banks.size() && remainingPositions.get(payerIndex) >= 0){
                payerIndex++;
            }

            while(receiverIndex < banks.size() && remainingPositions.get(receiverIndex) <= 0){
                receiverIndex++;
            }

            if(payerIndex < banks.size() && receiverIndex < banks.size()){
                double payment = Math.min(-remainingPositions.get(payerIndex), remainingPositions.get(receiverIndex));
                payment = roundToTwoDecimals(payment);

                Bank payer = banks.get(payerIndex);
                Bank receiver = banks.get(receiverIndex);

                double payerPosition = remainingPositions.get(payerIndex) + payment;
                double receiverPosition = remainingPositions.get(receiverIndex) - payment;
                payerPosition = roundToTwoDecimals(payerPosition);
                receiverPosition = roundToTwoDecimals(receiverPosition);
                remainingPositions.set(payerIndex, payerPosition);
                remainingPositions.set(receiverIndex, receiverPosition);

                localTotalAfter += payment;
                localTotalAfter = roundToTwoDecimals(localTotalAfter);
                localTransferSenders.add(payer.getName());
                localTransferReceivers.add(receiver.getName());
                localTransferAmounts.add(payment);
            }
        }

        for(double remainingPosition : remainingPositions){
            if(remainingPosition != 0.0){
                throw new IllegalArgumentException("Unable to generate a complete transfer plan.");
            }
        }

        if(localTotalBefore != 0.0){
            localReductionPercentage = roundToTwoDecimals(((localTotalBefore - localTotalAfter) / localTotalBefore) * 100.0);
        }

        bankIndex = 0;

        for(Bank bank : banks){
            double netPosition = netPositions.get(bankIndex);

            if(netPosition < 0){
                bank.withdraw(-netPosition);
            }

            bankIndex++;
        }

        bankIndex = 0;

        for(Bank bank : banks){
            double netPosition = netPositions.get(bankIndex);

            if(netPosition > 0){
                bank.deposit(netPosition);
            }

            bankIndex++;
        }

        bankNames = localBankNames;
        netAmounts = localNetAmounts;
        transferSenders = localTransferSenders;
        transferReceivers = localTransferReceivers;
        transferAmounts = localTransferAmounts;
        totalBefore = localTotalBefore;
        totalAfter = localTotalAfter;
        reductionPercentage = localReductionPercentage;

        System.out.println("Final transfers:");

        for(int transferIndex = 0; transferIndex < transferAmounts.size(); transferIndex++){
            System.out.printf("%s pays %s: %.2f%n", transferSenders.get(transferIndex),
                    transferReceivers.get(transferIndex), transferAmounts.get(transferIndex));
        }

        System.out.printf("Total before netting: %.2f%n", totalBefore);
        System.out.printf("Total after netting: %.2f%n", totalAfter);
        System.out.printf("Reduction percentage: %.2f%%%n", reductionPercentage);

        manager.clearTransactions();
    }

    private double roundToTwoDecimals(double value){
        if(!Double.isFinite(value)){
            throw new IllegalArgumentException("Value must be finite.");
        }

        double scaledValue = value * 100.0;

        if(!Double.isFinite(scaledValue) || scaledValue >= Long.MAX_VALUE || scaledValue <= Long.MIN_VALUE){
            throw new IllegalArgumentException("Value is too large to round.");
        }

        return Math.round(scaledValue) / 100.0;
    }
}
