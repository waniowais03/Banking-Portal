import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../services/api";

function Register() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    countryCode: "IN",
    phoneNumber: "",
    address: "",
    password: "",
    confirmPassword: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));

    setError("");
    setMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setMessage("");

    if (
      !formData.name.trim() ||
      !formData.email.trim() ||
      !formData.countryCode.trim() ||
      !formData.phoneNumber.trim() ||
      !formData.address.trim() ||
      !formData.password ||
      !formData.confirmPassword
    ) {
      setError("Please fill in all fields.");
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    try {
      setLoading(true);

      const response = await registerUser({
        name: formData.name.trim(),
        email: formData.email.trim(),
        countryCode: formData.countryCode.trim(),
        phoneNumber: formData.phoneNumber.trim(),
        address: formData.address.trim(),
        password: formData.password,
      });

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message ||
            "User registered successfully.";

      setMessage(responseMessage);

      setFormData({
        name: "",
        email: "",
        countryCode: "IN",
        phoneNumber: "",
        address: "",
        password: "",
        confirmPassword: "",
      });

      setTimeout(() => {
        navigate("/login");
      }, 1500);
    } catch (err) {
      console.error("Registration error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Registration failed. Please try again.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Registration failed. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card register-card">
        <div className="auth-brand">
          <div className="brand-icon">₹</div>

          <h1>Banking Portal</h1>

          <p>Create your bank account</p>
        </div>

        <div className="auth-content">
          <h2>Create Account</h2>

          <p className="auth-subtitle">
            Enter your details to register
          </p>

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
              <label htmlFor="name">
                Full Name
              </label>

              <input
                id="name"
                name="name"
                type="text"
                placeholder="Enter your full name"
                value={formData.name}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">
                Email
              </label>

              <input
                id="email"
                name="email"
                type="email"
                placeholder="Enter your email"
                value={formData.email}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="countryCode">
                Country Code
              </label>

              <input
                id="countryCode"
                name="countryCode"
                type="text"
                placeholder="IN"
                value={formData.countryCode}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="phoneNumber">
                Phone Number
              </label>

              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                placeholder="Enter your phone number"
                value={formData.phoneNumber}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="address">
                Address
              </label>

              <input
                id="address"
                name="address"
                type="text"
                placeholder="Enter your address"
                value={formData.address}
                onChange={handleChange}
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
                placeholder="Create a password"
                value={formData.password}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword">
                Confirm Password
              </label>

              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChange={handleChange}
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="primary-button"
              disabled={loading}
            >
              {loading
                ? "Creating Account..."
                : "Create Account"}
            </button>
          </form>

          <div className="auth-footer">
            <span>Already have an account?</span>

            <button
              type="button"
              className="link-button"
              onClick={() => navigate("/login")}
              disabled={loading}
            >
              Login
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Register;