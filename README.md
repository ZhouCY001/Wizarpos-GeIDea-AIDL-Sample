## App Description

### 🔧 AIDL Service
- **bind / unBind** buttons are used to bind or unbind the AIDL service.  
- The new payment application's **packageName** and **Action** have changed, so they must be updated accordingly.

---

### ⛔ Cancel Transaction
- **CancelTrans** is used to cancel the currently running transaction.  
- **Note:** Transactions that have already gone online *cannot* be cancelled.

---

### 🔄 Callback Registration
- **setPayCallback** and **setPinpadCallback** are used to register callbacks:
  - `setPayCallback`: listens to the current action code and message.
  - `setPinpadCallback`: listens to the number of digits entered during PIN input.

#### Important Notes
- If **setPayCallback** is enabled:
  - The payment app runs entirely in the **background**.
  - It continuously reports the current **Action code** and **message**.
- If **setPayCallback** is NOT enabled:
  - The payment app runs transactions in the **foreground**.

---

### 💳 Transaction APIs
- **Sale**, **VoidSale**, and similar methods are all transaction-related.
- Please refer to the GitHub sample for required parameters.

---

### 🧾 POS Information
- **POSInfo** is used to get device parameters including:
  - SN
  - MID / TID
  - Merchant Name
  - Whether keys have been injected
  - And more

---

### 🧾 Last Transaction Information
- **PrintLast** retrieves information about the last completed transaction.

---

### ⚙️ Application Display Control
- **SetParams** is used to control whether the payment application's icon is displayed or hidden.
  - In most vending machine scenarios, the icon should be **hidden**.

---

