import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getTransactions } from "../services/api";

function Transactions() {
  const navigate = useNavigate();

  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const accountNumber = localStorage.getItem("accountNumber");

  const loadTransactions = useCallback(
    async (showLoader = true) => {
      try {
        if (showLoader) {
          setLoading(true);
        } else {
          setRefreshing(true);
        }

        setError("");

        const response = await getTransactions();

        let data = response.data;

        if (typeof data === "string") {
          try {
            data = JSON.parse(data);
          } catch {
            data = [];
          }
        }

        setTransactions(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Transactions error:", err);

        const responseError =
          err.response?.data?.message ||
          err.response?.data ||
          "Unable to load transactions.";

        setError(
          typeof responseError === "string"
            ? responseError
            : "Unable to load transactions."
        );
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    []
  );

  useEffect(() => {
    loadTransactions(true);
  }, [loadTransactions]);

  const formatDate = (date) => {
    if (!date) {
      return "—";
    }

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) {
      return date;
    }

    return parsedDate.toLocaleString("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
    });
  };

  const formatAmount = (amount) => {
    const numericAmount = Number(amount);

    if (Number.isNaN(numericAmount)) {
      return "₹0.00";
    }

    return `₹${numericAmount.toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  const getTransactionInfo = (type) => {
    switch (type) {
      case "CASH_DEPOSIT":
        return {
          label: "Cash Deposit",
          category: "CREDIT",
          icon: "↓",
        };

      case "CASH_WITHDRAWAL":
        return {
          label: "Cash Withdrawal",
          category: "DEBIT",
          icon: "↑",
        };

      case "CASH_TRANSFER":
        return {
          label: "Fund Transfer",
          category: "TRANSFER",
          icon: "↔",
        };

      default:
        return {
          label: type || "Transaction",
          category: "TRANSACTION",
          icon: "₹",
        };
    }
  };

  return (
    <div className="page-container">
      <div className="page-card">

        {/* Header */}
        <div className="transaction-header">
          <div>
            <button
              className="back-button"
              onClick={() => navigate("/dashboard")}
            >
              ← Dashboard
            </button>

            <h1>Transactions</h1>

            <p>
              Transaction history for account{" "}
              <strong>{accountNumber || "—"}</strong>
            </p>
          </div>

          <button
            className="primary-button"
            onClick={() => loadTransactions(false)}
            disabled={loading || refreshing}
          >
            {refreshing ? "Refreshing..." : "↻ Refresh"}
          </button>
        </div>

        {/* Loading */}
        {loading && (
          <div className="empty-state">
            <div className="spinner"></div>

            <h3>Loading transactions...</h3>

            <p>
              Please wait while we fetch your transaction history.
            </p>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div>
            <div className="error-message">
              {error}
            </div>

            <button
              className="primary-button"
              onClick={() => loadTransactions(true)}
            >
              Try Again
            </button>
          </div>
        )}

        {/* Empty */}
        {!loading &&
          !error &&
          transactions.length === 0 && (
            <div className="empty-state">
              <div className="empty-icon">☷</div>

              <h3>No Transactions Available</h3>

              <p>
                Your transaction history will appear here
                after you perform a banking transaction.
              </p>

              <button
                className="primary-button"
                onClick={() => navigate("/deposit")}
              >
                Make a Deposit
              </button>
            </div>
          )}

        {/* Transactions */}
        {!loading &&
          !error &&
          transactions.length > 0 && (
            <div className="transaction-list">

              {transactions.map((transaction, index) => {
                const info = getTransactionInfo(
                  transaction.transactionType
                );

                return (
                  <div
                    className="transaction-card"
                    key={
                      transaction.id ||
                      `${transaction.transactionDate}-${index}`
                    }
                  >

                    {/* Icon */}
                    <div className="transaction-icon">
                      {info.icon}
                    </div>

                    {/* Main information */}
                    <div className="transaction-main">

                      <h3>{info.label}</h3>

                      <p>
                        {formatDate(
                          transaction.transactionDate
                        )}
                      </p>

                      <span className="transaction-category">
                        {info.category}
                      </span>

                    </div>

                    {/* Amount */}
                    <div
                      className={`transaction-amount ${info.category.toLowerCase()}`}
                    >
                      {info.category === "CREDIT"
                        ? "+"
                        : info.category === "DEBIT"
                        ? "-"
                        : ""}
                      {formatAmount(transaction.amount)}
                    </div>

                    {/* Details */}
                    <div className="transaction-details">

                      <p>
                        <strong>Source:</strong>{" "}
                        {transaction.sourceAccountNumber ||
                          "—"}
                      </p>

                      {transaction.targetAccountNumber && (
                        <p>
                          <strong>Target:</strong>{" "}
                          {transaction.targetAccountNumber}
                        </p>
                      )}

                    </div>

                  </div>
                );
              })}

            </div>
          )}

      </div>
    </div>
  );
}

export default Transactions;