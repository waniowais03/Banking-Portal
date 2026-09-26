import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAccountDetails,
  getUserDetails,
  sendBankStatement,
} from "../services/api";

function Account() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Bank statement states
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementMessage, setStatementMessage] = useState("");
  const [statementError, setStatementError] = useState("");

  useEffect(() => {
    const loadAccount = async () => {
      try {
        setLoading(true);
        setError("");

        const [userResponse, accountResponse] = await Promise.all([
          getUserDetails(),
          getAccountDetails(),
        ]);

        setUser(userResponse.data);
        setAccount(accountResponse.data);
      } catch (err) {
        console.error("Account details error:", err);

        if (err.response?.status !== 401) {
          setError(
            err.response?.data?.message ||
              "Unable to load account details."
          );
        }
      } finally {
        setLoading(false);
      }
    };

    loadAccount();
  }, []);

  // Send bank statement
  const handleSendStatement = async () => {
    try {
      setStatementLoading(true);
      setStatementMessage("");
      setStatementError("");

      const response = await sendBankStatement();

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "Bank statement sent successfully.";

      setStatementMessage(responseMessage);
    } catch (err) {
      console.error("Bank statement error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to send bank statement.";

      setStatementError(
        typeof responseError === "string"
          ? responseError
          : "Unable to send bank statement."
      );
    } finally {
      setStatementLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-card">
          <h2>Loading account details...</h2>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <div className="page-card">
          <h2>{error}</h2>

          <button
            className="primary-button"
            onClick={() => navigate("/dashboard")}
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-card">

        {/* Back Button */}
        <button
          className="back-button"
          onClick={() => navigate("/dashboard")}
        >
          ← Dashboard
        </button>

        <h1>Account Details</h1>

        <p>
          View your personal and banking information.
        </p>

        {/* Account Details */}
        <div className="details-list">

          <div className="detail-row">
            <span>Name</span>
            <strong>{user?.name || "—"}</strong>
          </div>

          <div className="detail-row">
            <span>Email</span>
            <strong>{user?.email || "—"}</strong>
          </div>

          <div className="detail-row">
            <span>Phone Number</span>
            <strong>{user?.phoneNumber || "—"}</strong>
          </div>

          <div className="detail-row">
            <span>Address</span>
            <strong>{user?.address || "—"}</strong>
          </div>

          <div className="detail-row">
            <span>Account Number</span>
            <strong>
              {account?.accountNumber || "—"}
            </strong>
          </div>

          <div className="detail-row">
            <span>Account Type</span>
            <strong>
              {account?.accountType || "—"}
            </strong>
          </div>

          <div className="detail-row">
            <span>Balance</span>
            <strong>
              ₹
              {Number(account?.balance || 0).toLocaleString(
                "en-IN",
                {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                }
              )}
            </strong>
          </div>

          <div className="detail-row">
            <span>Branch</span>
            <strong>
              {account?.branch || "—"}
            </strong>
          </div>

          <div className="detail-row">
            <span>IFSC Code</span>
            <strong>
              {account?.ifscCode || "—"}
            </strong>
          </div>

        </div>

        {/* Bank Statement Section */}
        <div style={{ marginTop: "30px" }}>

          {statementMessage && (
            <div className="success-message">
              {statementMessage}
            </div>
          )}

          {statementError && (
            <div className="error-message">
              {statementError}
            </div>
          )}

          <button
            className="primary-button"
            onClick={handleSendStatement}
            disabled={statementLoading}
          >
            {statementLoading
              ? "Sending Statement..."
              : "📄 Send Bank Statement"}
          </button>

        </div>

      </div>
    </div>
  );
}

export default Account;