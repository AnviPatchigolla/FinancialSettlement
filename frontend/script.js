const bankForm = document.getElementById("bank-form");
const transactionForm = document.getElementById("transaction-form");
const settlementButton = document.getElementById("settlement-button");
const backendStatus = document.getElementById("backend-status");

function showMessage(id, message, isError){
    const element = document.getElementById(id);
    element.textContent = message;
    element.classList.toggle("error", isError);
}

async function request(path, options){
    const response = await fetch(path, options);
    const data = await response.json();
    if(!response.ok){
        throw new Error(data.error || "Request failed.");
    }
    return data;
}

function setBackendOnline(online){
    backendStatus.textContent = online ? "Backend online" : "Backend offline";
    backendStatus.classList.toggle("online", online);
}

function addEmptyItem(list, message){
    const item = document.createElement("li");
    item.className = "empty-state";
    item.textContent = message;
    list.appendChild(item);
}

function renderBanks(banks){
    const list = document.getElementById("bank-list");
    const sender = document.getElementById("sender-bank");
    const receiver = document.getElementById("receiver-bank");
    list.innerHTML = "";
    sender.innerHTML = "";
    receiver.innerHTML = "";

    if(banks.length === 0){
        addEmptyItem(list, "No banks registered.");
        sender.add(new Option("No banks available", ""));
        receiver.add(new Option("No banks available", ""));
        return;
    }

    sender.add(new Option("Select sender", ""));
    receiver.add(new Option("Select receiver", ""));
    for(const name of banks){
        const item = document.createElement("li");
        item.textContent = name;
        list.appendChild(item);
        sender.add(new Option(name, name));
        receiver.add(new Option(name, name));
    }
}

function renderTransactions(transactions){
    const body = document.getElementById("transaction-table-body");
    body.innerHTML = "";
    if(transactions.length === 0){
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 3;
        cell.className = "empty-state";
        cell.textContent = "No pending transactions.";
        row.appendChild(cell);
        body.appendChild(row);
        return;
    }

    for(const transaction of transactions){
        const row = document.createElement("tr");
        for(const value of [transaction.sender, transaction.receiver, transaction.amount.toFixed(2)]){
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        }
        body.appendChild(row);
    }
}

function renderSettlement(result){
    const positions = document.getElementById("net-position-list");
    const transfers = document.getElementById("transfer-list");
    positions.innerHTML = "";
    transfers.innerHTML = "";

    for(const position of result.netPositions){
        const sign = position.amount > 0 ? "+" : "";
        const item = document.createElement("li");
        item.textContent = `${position.bank}: ${sign}${position.amount.toFixed(2)}`;
        positions.appendChild(item);
    }
    if(result.transfers.length === 0){
        addEmptyItem(transfers, "No transfers required.");
    }else{
        for(const transfer of result.transfers){
            const item = document.createElement("li");
            item.textContent = `${transfer.sender} pays ${transfer.receiver}: ${transfer.amount.toFixed(2)}`;
            transfers.appendChild(item);
        }
    }
    document.getElementById("total-before").textContent = result.totalBefore.toFixed(2);
    document.getElementById("total-after").textContent = result.totalAfter.toFixed(2);
    document.getElementById("reduction-percentage").textContent = result.reductionPercentage.toFixed(2) + "%";
}

async function loadBanks(){
    const data = await request("/api/banks");
    renderBanks(data.banks);
}

async function loadTransactions(){
    const data = await request("/api/transactions");
    renderTransactions(data.transactions);
}

bankForm.addEventListener("submit", async function(event){
    event.preventDefault();
    const name = document.getElementById("bank-name").value.trim();
    const balance = Number(document.getElementById("bank-balance").value);
    try{
        await request("/api/banks", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({name: name, balance: balance})
        });
        bankForm.reset();
        showMessage("bank-message", "Bank added.", false);
        await loadBanks();
        setBackendOnline(true);
    }catch(error){
        showMessage("bank-message", error.message, true);
    }
});

transactionForm.addEventListener("submit", async function(event){
    event.preventDefault();
    const sender = document.getElementById("sender-bank").value;
    const receiver = document.getElementById("receiver-bank").value;
    const amount = Number(document.getElementById("transaction-amount").value);
    try{
        await request("/api/transactions", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({sender: sender, receiver: receiver, amount: amount})
        });
        transactionForm.reset();
        showMessage("transaction-message", "Transaction added.", false);
        await loadTransactions();
        setBackendOnline(true);
    }catch(error){
        showMessage("transaction-message", error.message, true);
    }
});

settlementButton.addEventListener("click", async function(){
    try{
        const result = await request("/api/settle", {method: "POST"});
        renderSettlement(result);
        await loadTransactions();
        showMessage("settlement-message", "Settlement completed.", false);
        setBackendOnline(true);
    }catch(error){
        showMessage("settlement-message", error.message, true);
    }
});

async function start(){
    try{
        await loadBanks();
        await loadTransactions();
        setBackendOnline(true);
    }catch(error){
        setBackendOnline(false);
        showMessage("bank-message", "Start the Java server to use this page.", true);
    }
}

start();
