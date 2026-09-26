import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { withdrawCash } from "../services/api";

function Withdraw() {
  const navigate = useNavigate();

  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const accountNumber = localStorage.getItem("accountNumber");

  const handleSubmit = async (event) => {
    event.preventDefault();

    setMessage("");
    setError("");

    const numericAmount = Number(amount);

    if (!numericAmount || numericAmount <= 0) {
      setError("Please enter a valid amount.");
      return;
    }

    if (!/^\d{4}$/.test(pin)) {
      setError("PIN must contain exactly 4 digits.");
      return;
    }

    if (!accountNumber) {
      setError("Account number not found.");
      return;
    }

    try {
      setLoading(true);

      const response = await withdrawCash(
        numericAmount,
        pin
      );

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "Cash withdrawn successfully.";

      setMessage(responseMessage);
      setAmount("");
      setPin("");
    } catch (err) {
      console.error("Withdraw error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to withdraw cash.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Unable to withdraw cash."
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

        <h1>Withdraw Cash</h1>

        <p>Withdraw money from your bank account.</p>

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
          <span>Account Number</span>
          <strong>{accountNumber || "—"}</strong>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="withdraw-amount">
              Amount
            </label>

            <input
              id="withdraw-amount"
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
            <label htmlFor="withdraw-pin">
              Transaction PIN
            </label>

            <input
              id="withdraw-pin"
              type="password"
              inputMode="numeric"
              maxLength="4"
              placeholder="Enter 4-digit PIN"
              value={pin}
              onChange={(event) =>
                setPin(
                  event.target.value.replace(/\D/g, "")
                )
              }
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="primary-button"
            disabled={loading}
          >
            {loading
              ? "Processing..."
              : "Withdraw Cash"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Withdraw;