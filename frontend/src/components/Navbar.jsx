import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();

  const [menuOpen, setMenuOpen] = useState(false);

  const handleNavigation = (path) => {
    navigate(path);
    setMenuOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("accountNumber");
    navigate("/login", { replace: true });
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="premium-navbar">
      {/* Animated background glow */}
      <div className="navbar-glow"></div>

      {/* BRAND */}
      <div
        className="navbar-brand"
        onClick={() => handleNavigation("/dashboard")}
      >
        <div className="navbar-logo-wrapper">
          <div className="navbar-logo">₹</div>
          <div className="logo-ring"></div>
        </div>

        <div className="navbar-title">
          <strong>Banking Portal</strong>
          <span>
            <span className="status-dot"></span>
            Secure Banking
          </span>
        </div>
      </div>

      {/* DESKTOP NAVIGATION */}
      <div className={`navbar-links ${menuOpen ? "open" : ""}`}>
        <button
          className={isActive("/dashboard") ? "active" : ""}
          onClick={() => handleNavigation("/dashboard")}
        >
          <span className="nav-icon">⌂</span>
          <span>Dashboard</span>
        </button>

        <button
          className={isActive("/transactions") ? "active" : ""}
          onClick={() => handleNavigation("/transactions")}
        >
          <span className="nav-icon">↕</span>
          <span>Transactions</span>
        </button>

        <button
          className={isActive("/account") ? "active" : ""}
          onClick={() => handleNavigation("/account")}
        >
          <span className="nav-icon">◉</span>
          <span>Account</span>
        </button>

        <button
          className="navbar-logout"
          onClick={handleLogout}
        >
          <span className="nav-icon">↪</span>
          <span>Logout</span>
        </button>
      </div>

      {/* MOBILE MENU BUTTON */}
      <button
        className={`navbar-menu ${menuOpen ? "menu-active" : ""}`}
        onClick={() => setMenuOpen(!menuOpen)}
        aria-label="Toggle navigation"
        aria-expanded={menuOpen}
      >
        <span></span>
        <span></span>
        <span></span>
      </button>
    </nav>
  );
}

export default Navbar;