import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../services/api";

function Login() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    identifier: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    if (!formData.identifier.trim() || !formData.password) {
      setError("Please enter your identifier and password.");
      return;
    }

    try {
      setLoading(true);

      const response = await loginUser(
        formData.identifier.trim(),
        formData.password
      );

      const token = response.data?.token;

      if (!token) {
        throw new Error("Login token was not received from the server.");
      }

      localStorage.setItem("token", token);

      // JWT subject in your backend contains the account number.
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        if (payload.sub) {
          localStorage.setItem("accountNumber", payload.sub);
        }
      } catch {
        // Account number can still be obtained from dashboard API.
      }

      navigate("/dashboard", { replace: true });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data ||
        err.message ||
        "Login failed. Please check your credentials.";

      setError(
        typeof message === "string" ? message : "Unable to login."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="brand-icon">₹</div>
          <h1>Banking Portal</h1>
          <p>Secure online banking</p>
        </div>

        <div className="auth-content">
          <h2>Welcome Back</h2>
          <p className="auth-subtitle">
            Login to access your account
          </p>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="identifier">
                Email / Account Number
              </label>

              <input
                id="identifier"
                name="identifier"
                type="text"
                placeholder="Enter email or account number"
                value={formData.identifier}
                onChange={handleChange}
                autoComplete="username"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">
                Password
              </label>

              <input
                id="password"
                name="password"
                type="password"
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                autoComplete="current-password"
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="primary-button"
              disabled={loading}
            >
              {loading ? "Logging in..." : "Login"}
            </button>
          </form>

          <div className="auth-footer">
            <span>Don't have an account?</span>

            <button
              type="button"
              className="link-button"
              onClick={() => navigate("/register")}
            >
              Create Account
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;