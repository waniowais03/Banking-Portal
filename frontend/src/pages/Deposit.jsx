import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { depositCash } from "../services/api";

function Deposit() {
  const navigate = useNavigate();

  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

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

    try {
      setLoading(true);

      const response = await depositCash(
        numericAmount,
        pin
      );

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "Cash deposited successfully.";

      setMessage(responseMessage);
      setAmount("");
      setPin("");
    } catch (err) {
      console.error("Deposit error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to deposit cash.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Unable to deposit cash."
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

        <h1>Deposit Cash</h1>

        <p>Add money to your bank account.</p>

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

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="deposit-amount">
              Amount
            </label>

            <input
              id="deposit-amount"
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
            <label htmlFor="deposit-pin">
              Transaction PIN
            </label>

            <input
              id="deposit-pin"
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
              : "Deposit Cash"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Deposit;