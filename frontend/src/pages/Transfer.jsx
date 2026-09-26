import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { transferFunds } from "../services/api";

function Transfer() {
  const navigate = useNavigate();

  const [targetAccountNumber, setTargetAccountNumber] = useState("");
  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const sourceAccountNumber =
    localStorage.getItem("accountNumber");

  const handleSubmit = async (event) => {
    event.preventDefault();

    setMessage("");
    setError("");

    const numericAmount = Number(amount);

    if (!targetAccountNumber.trim()) {
      setError("Please enter the target account number.");
      return;
    }

    if (!numericAmount || numericAmount <= 0) {
      setError("Please enter a valid amount.");
      return;
    }

    if (!/^\d{4}$/.test(pin)) {
      setError("PIN must contain exactly 4 digits.");
      return;
    }

    if (!sourceAccountNumber) {
      setError("Source account number not found.");
      return;
    }

    if (sourceAccountNumber === targetAccountNumber.trim()) {
      setError("Source and target account numbers cannot be the same.");
      return;
    }

    try {
      setLoading(true);

      const response = await transferFunds(
        sourceAccountNumber,
        targetAccountNumber.trim(),
        numericAmount,
        pin
      );

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "Fund transferred successfully.";

      setMessage(responseMessage);

      setTargetAccountNumber("");
      setAmount("");
      setPin("");
    } catch (err) {
      console.error("Fund transfer error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to transfer funds.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Unable to transfer funds."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-card">
        <button
          className="back-button"
          onClick={() => navigate("/dashboard")}
        >
          ← Dashboard
        </button>

        <h1>Fund Transfer</h1>
        <p>Transfer money securely to another account.</p>

        {message && (
          <div className="success-message">
            {message}
          </div>
        )}

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <div className="transfer-summary">
          <span>From Account</span>
          <strong>{sourceAccountNumber || "—"}</strong>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="targetAccountNumber">
              Target Account Number
            </label>

            <input
              id="targetAccountNumber"
              type="text"
              placeholder="Enter target account number"
              value={targetAccountNumber}
              onChange={(event) =>
                setTargetAccountNumber(event.target.value)
              }
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="transfer-amount">
              Amount
            </label>

            <input
              id="transfer-amount"
              type="number"
              min="1"
              step="0.01"
              placeholder="Enter amount"
              value={amount}
              onChange={(event) =>
                setAmount(event.target.value)
              }
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="transfer-pin">
              Transaction PIN
            </label>

            <input
              id="transfer-pin"
              type="password"
              inputMode="numeric"
              maxLength="4"
              placeholder="Enter 4-digit PIN"
              value={pin}
              onChange={(event) =>
                setPin(event.target.value.replace(/\D/g, ""))
              }
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="primary-button"
            disabled={loading}
          >
            {loading ? "Processing..." : "Transfer Funds"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Transfer;