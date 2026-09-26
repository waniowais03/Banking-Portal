import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAccountDetails,
  getUserDetails,
  sendBankStatement,
} from "../services/api";

function Dashboard() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementMessage, setStatementMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = async () => {
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
        console.error("Dashboard error:", err);

        if (err.response?.status !== 401) {
          setError(
            err.response?.data?.message ||
              "Unable to load account information."
          );
        }
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("accountNumber");

    navigate("/login", {
      replace: true,
    });
  };

  const handleStatement = async () => {
    try {
      setStatementLoading(true);
      setStatementMessage("");

      const response = await sendBankStatement();

      const message =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "Bank statement sent successfully.";

      setStatementMessage(message);
    } catch (err) {
      console.error("Statement error:", err);

      setStatementMessage(
        err.response?.data?.message ||
          "Unable to send bank statement."
      );
    } finally {
      setStatementLoading(false);
    }
  };

  const formatBalance = (balance) => {
    return Number(balance || 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  if (loading) {
    return (
      <div className="premium-loading-screen">
        <div className="loading-orb"></div>

        <div className="premium-loader">
          <div className="loader-ring"></div>
          <div className="loader-logo">₹</div>
        </div>

        <h2>Loading your banking portal</h2>
        <p>Securing your account...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="premium-error-screen">
        <div className="premium-error-card">
          <div className="error-icon">!</div>

          <span className="eyebrow">BANKING PORTAL</span>

          <h2>Unable to load dashboard</h2>

          <p>{error}</p>

          <button
            className="premium-primary-button"
            onClick={() => window.location.reload()}
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="premium-dashboard">
      {/* =========================
          BACKGROUND EFFECTS
      ========================= */}

      <div className="dashboard-bg">
        <div className="bg-orb orb-one"></div>
        <div className="bg-orb orb-two"></div>
        <div className="bg-orb orb-three"></div>

        <div className="grid-overlay"></div>
      </div>

      {/* =========================
          NAVBAR
      ========================= */}

      <header className="premium-dashboard-header">
        <div
          className="dashboard-brand"
          onClick={() => navigate("/dashboard")}
        >
          <div className="dashboard-brand-icon">
            ₹
          </div>

          <div>
            <h1>Banking Portal</h1>
            <span>Secure Digital Banking</span>
          </div>
        </div>

        <div className="dashboard-header-actions">
          <button
            className="header-icon-button"
            onClick={() => navigate("/transactions")}
            title="Transactions"
          >
            ↗
          </button>

          <div className="profile-mini">
            <div className="profile-avatar">
              {(user?.name || "U")
                .charAt(0)
                .toUpperCase()}
            </div>

            <div className="profile-info">
              <strong>{user?.name || "User"}</strong>
              <span>Active Account</span>
            </div>
          </div>

          <button
            className="premium-logout-button"
            onClick={handleLogout}
          >
            <span>Logout</span>
            <span>↗</span>
          </button>
        </div>
      </header>

      {/* =========================
          MAIN CONTENT
      ========================= */}

      <main className="premium-dashboard-content">

        {/* =========================
            WELCOME
        ========================= */}

        <section className="premium-welcome">
          <div>
            <span className="eyebrow">
              PERSONAL BANKING
            </span>

            <h2>
              Welcome back,{" "}
              <span>{user?.name || "User"}</span>
            </h2>

            <p>
              Everything you need to manage your money,
              all in one secure place.
            </p>
          </div>

          <div className="secure-badge">
            <span className="secure-dot"></span>
            <span>Account Secured</span>
          </div>
        </section>

        {/* =========================
            BALANCE CARD
        ========================= */}

        <section className="premium-balance-card">

          <div className="balance-glow"></div>

          <div className="balance-card-content">

            <div className="balance-top">
              <div>
                <span className="balance-label">
                  AVAILABLE BALANCE
                </span>

                <p className="balance-caption">
                  Current account balance
                </p>
              </div>

              <div className="balance-chip">
                <span className="chip-dot"></span>
                ACTIVE
              </div>
            </div>

            <div className="balance-amount">
              <span className="currency">₹</span>

              <span>
                {formatBalance(account?.balance)}
              </span>
            </div>

            <div className="balance-bottom">

              <div>
                <span className="account-label">
                  ACCOUNT NUMBER
                </span>

                <strong>
                  {account?.accountNumber || "—"}
                </strong>
              </div>

              <div>
                <span className="account-label">
                  ACCOUNT TYPE
                </span>

                <strong>
                  {account?.accountType || "—"}
                </strong>
              </div>

              <div>
                <span className="account-label">
                  IFSC CODE
                </span>

                <strong>
                  {account?.ifscCode || "—"}
                </strong>
              </div>

            </div>
          </div>

          <div className="balance-watermark">
            ₹
          </div>

        </section>

        {/* =========================
            QUICK ACTIONS
        ========================= */}

        <section className="premium-section">

          <div className="section-heading">
            <div>
              <span className="eyebrow">
                BANKING SERVICES
              </span>

              <h3>Quick Actions</h3>
            </div>

            <span className="section-description">
              Manage your account instantly
            </span>
          </div>

          <div className="premium-action-grid">

            <button
              className="premium-action-card deposit-action"
              onClick={() => navigate("/deposit")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ↓
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Deposit Money</h4>
                <p>Add funds to your account</p>
              </div>
            </button>

            <button
              className="premium-action-card withdraw-action"
              onClick={() => navigate("/withdraw")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ↑
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Withdraw Money</h4>
                <p>Withdraw funds securely</p>
              </div>
            </button>

            <button
              className="premium-action-card transfer-action"
              onClick={() => navigate("/transfer")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ↔
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Fund Transfer</h4>
                <p>Send money instantly</p>
              </div>
            </button>

            <button
              className="premium-action-card transaction-action"
              onClick={() => navigate("/transactions")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ☷
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Transactions</h4>
                <p>View your transaction history</p>
              </div>
            </button>

            <button
              className="premium-action-card pin-action"
              onClick={() => navigate("/pin-management")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ●
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Manage PIN</h4>
                <p>Create or update your PIN</p>
              </div>
            </button>

            <button
              className="premium-action-card account-action"
              onClick={() => navigate("/account")}
            >
              <div className="action-card-top">
                <div className="premium-action-icon">
                  ◉
                </div>

                <span className="action-arrow">
                  ↗
                </span>
              </div>

              <div>
                <h4>Account Details</h4>
                <p>View your banking information</p>
              </div>
            </button>

          </div>
        </section>

        {/* =========================
            LOWER GRID
        ========================= */}

        <section className="premium-lower-grid">

          {/* ACCOUNT DETAILS */}

          <div className="premium-info-card">

            <div className="info-card-header">
              <div>
                <span className="eyebrow">
                  PROFILE
                </span>

                <h3>Account Details</h3>
              </div>

              <button
                className="small-outline-button"
                onClick={() => navigate("/account")}
              >
                View Full
              </button>
            </div>

            <div className="premium-details">

              <div className="premium-detail">
                <span>Full Name</span>
                <strong>{user?.name || "—"}</strong>
              </div>

              <div className="premium-detail">
                <span>Email Address</span>
                <strong>{user?.email || "—"}</strong>
              </div>

              <div className="premium-detail">
                <span>Phone Number</span>
                <strong>{user?.phoneNumber || "—"}</strong>
              </div>

              <div className="premium-detail">
                <span>Branch</span>
                <strong>{account?.branch || "—"}</strong>
              </div>

              <div className="premium-detail">
                <span>Account Type</span>
                <strong>{account?.accountType || "—"}</strong>
              </div>

              <div className="premium-detail">
                <span>IFSC Code</span>
                <strong>{account?.ifscCode || "—"}</strong>
              </div>

            </div>
          </div>

          {/* STATEMENT CARD */}

          <div className="premium-statement-card">

            <div className="statement-glow"></div>

            <div className="statement-icon">
              ↓
            </div>

            <span className="eyebrow">
              DOCUMENTS
            </span>

            <h3>
              Bank Statement
            </h3>

            <p>
              Get your latest transaction statement
              directly on your registered email address.
            </p>

            {statementMessage && (
              <div className="statement-message">
                {statementMessage}
              </div>
            )}

            <button
              className="statement-button"
              onClick={handleStatement}
              disabled={statementLoading}
            >
              {statementLoading
                ? "Sending..."
                : "Send Statement"}
              <span>↗</span>
            </button>

          </div>

        </section>

        {/* =========================
            SECURITY STRIP
        ========================= */}

        <section className="security-strip">

          <div className="security-icon">
            ✓
          </div>

          <div>
            <strong>
              Your account is protected
            </strong>

            <p>
              Your banking session is secured using
              encrypted authentication.
            </p>
          </div>

          <div className="security-status">
            <span></span>
            Secure
          </div>

        </section>

      </main>

      {/* =========================
          FOOTER
      ========================= */}

      <footer className="premium-footer">
        <span>
          © {new Date().getFullYear()} Banking Portal
        </span>

        <span>
          Secure • Private • Reliable
        </span>
      </footer>

    </div>
  );
}

export default Dashboard;