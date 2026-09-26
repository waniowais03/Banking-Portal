import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createPin, updatePin } from "../services/api";

function PinManagement() {
  const navigate = useNavigate();

  const [mode, setMode] = useState("create");

  const [pin, setPin] = useState("");
  const [oldPin, setOldPin] = useState("");
  const [newPin, setNewPin] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const accountNumber = localStorage.getItem("accountNumber");

  const clearMessages = () => {
    setMessage("");
    setError("");
  };

  const handleCreatePin = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!/^\d{4}$/.test(pin)) {
      setError("PIN must contain exactly 4 digits.");
      return;
    }

    if (!password) {
      setError("Please enter your password.");
      return;
    }

    try {
      setLoading(true);

      const response = await createPin({
        accountNumber,
        pin,
        password,
      });

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message || "PIN created successfully.";

      setMessage(responseMessage);
      setPin("");
      setPassword("");
    } catch (err) {
      console.error("Create PIN error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to create PIN.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Unable to create PIN."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePin = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!/^\d{4}$/.test(oldPin)) {
      setError("Old PIN must contain exactly 4 digits.");
      return;
    }

    if (!/^\d{4}$/.test(newPin)) {
      setError("New PIN must contain exactly 4 digits.");
      return;
    }

    if (!password) {
      setError("Please enter your password.");
      return;
    }

    try {
      setLoading(true);

      const response = await updatePin({
        accountNumber,
        oldPin,
        newPin,
        password,
      });

      const responseMessage =
        typeof response.data === "string"
          ? response.data
          : response.data?.message || "PIN updated successfully.";

      setMessage(responseMessage);

      setOldPin("");
      setNewPin("");
      setPassword("");
    } catch (err) {
      console.error("Update PIN error:", err);

      const responseError =
        err.response?.data?.message ||
        err.response?.data ||
        "Unable to update PIN.";

      setError(
        typeof responseError === "string"
          ? responseError
          : "Unable to update PIN."
      );
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (selectedMode) => {
    setMode(selectedMode);
    clearMessages();

    setPin("");
    setOldPin("");
    setNewPin("");
    setPassword("");
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

        <h1>PIN Management</h1>
        <p>Securely create or update your account PIN.</p>

        <div className="tab-buttons">
          <button
            type="button"
            className={mode === "create" ? "active-tab" : ""}
            onClick={() => switchMode("create")}
          >
            Create PIN
          </button>

          <button
            type="button"
            className={mode === "update" ? "active-tab" : ""}
            onClick={() => switchMode("update")}
          >
            Update PIN
          </button>
        </div>

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

        {mode === "create" ? (
          <form onSubmit={handleCreatePin}>
            <div className="form-group">
              <label htmlFor="pin">New PIN</label>

              <input
                id="pin"
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

            <div className="form-group">
              <label htmlFor="create-password">
                Account Password
              </label>

              <input
                id="create-password"
                type="password"
                placeholder="Enter account password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="primary-button"
              disabled={loading}
            >
              {loading ? "Creating..." : "Create PIN"}
            </button>
          </form>
        ) : (
          <form onSubmit={handleUpdatePin}>
            <div className="form-group">
              <label htmlFor="old-pin">Old PIN</label>

              <input
                id="old-pin"
                type="password"
                inputMode="numeric"
                maxLength="4"
                placeholder="Enter old PIN"
                value={oldPin}
                onChange={(event) =>
                  setOldPin(event.target.value.replace(/\D/g, ""))
                }
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="new-pin">New PIN</label>

              <input
                id="new-pin"
                type="password"
                inputMode="numeric"
                maxLength="4"
                placeholder="Enter new PIN"
                value={newPin}
                onChange={(event) =>
                  setNewPin(event.target.value.replace(/\D/g, ""))
                }
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="update-password">
                Account Password
              </label>

              <input
                id="update-password"
                type="password"
                placeholder="Enter account password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="primary-button"
              disabled={loading}
            >
              {loading ? "Updating..." : "Update PIN"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default PinManagement;