public class Bank{
    private String name;
    private double balance;

    public Bank(String name, double balance){
        if(!Double.isFinite(balance) || balance < 0 || !hasAtMostTwoDecimalPlaces(balance)){
            throw new IllegalArgumentException("Balance must be finite, non-negative, and have at most two decimal places.");
        }

        this.name = name;
        this.balance = balance;
    }

    public String getName(){
        return name;
    }

    public double getBalance(){
        return balance;
    }

    public boolean canPay(double amount){
        double newBalance = balance - amount;
        return Double.isFinite(amount) && amount > 0 && newBalance >= 0
                && Math.round((balance - newBalance) * 100.0) == Math.round(amount * 100.0);
    }

    public boolean canReceive(double amount){
        double newBalance = balance + amount;
        return Double.isFinite(amount) && amount > 0 && Double.isFinite(newBalance)
                && Math.round((newBalance - balance) * 100.0) == Math.round(amount * 100.0);
    }

    public void deposit(double amount){
        if(!Double.isFinite(amount) || amount <= 0 || !hasAtMostTwoDecimalPlaces(amount)){
            throw new IllegalArgumentException("Deposit amount must be finite, positive, and have at most two decimal places.");
        }

        if(!canReceive(amount)){
            throw new IllegalArgumentException("Deposit would result in a non-finite balance.");
        }

        balance += amount;
    }

    public void withdraw(double amount){
        if(!Double.isFinite(amount) || amount <= 0 || !hasAtMostTwoDecimalPlaces(amount)){
            throw new IllegalArgumentException("Withdrawal amount must be finite, positive, and have at most two decimal places.");
        }

        if(!canPay(amount)){
            throw new IllegalArgumentException("Insufficient balance.");
        }

        balance -= amount;
    }

    private boolean hasAtMostTwoDecimalPlaces(double value){
        double scaledValue = value * 100.0;

        if(!Double.isFinite(scaledValue) || scaledValue >= Long.MAX_VALUE || scaledValue <= Long.MIN_VALUE){
            return false;
        }

        long cents = Math.round(scaledValue);
        return value == cents / 100.0;
    }
}
