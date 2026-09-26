import "./App.css";

import { Navigate, Route, Routes } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Deposit from "./pages/Deposit";
import Withdraw from "./pages/Withdraw";
import Transfer from "./pages/Transfer";
import Transactions from "./pages/Transactions";
import Account from "./pages/Account";
import PinManagement from "./pages/PinManagement";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <Routes>
      {/* =========================
          PUBLIC ROUTES
      ========================= */}

      <Route
        path="/login"
        element={<Login />}
      />

      <Route
        path="/register"
        element={<Register />}
      />

      {/* =========================
          PROTECTED ROUTES
      ========================= */}

      <Route element={<ProtectedRoute />}>

        <Route
          path="/dashboard"
          element={<Dashboard />}
        />

        <Route
          path="/deposit"
          element={<Deposit />}
        />

        <Route
          path="/withdraw"
          element={<Withdraw />}
        />

        <Route
          path="/transfer"
          element={<Transfer />}
        />

        <Route
          path="/transactions"
          element={<Transactions />}
        />

        <Route
          path="/account"
          element={<Account />}
        />

        <Route
          path="/pin-management"
          element={<PinManagement />}
        />

      </Route>

      {/* =========================
          DEFAULT ROUTE
      ========================= */}

      <Route
        path="/"
        element={
          <Navigate
            to={
              localStorage.getItem("token")
                ? "/dashboard"
                : "/login"
            }
            replace
          />
        }
      />

      {/* =========================
          UNKNOWN ROUTES
      ========================= */}

      <Route
        path="*"
        element={
          <Navigate
            to={
              localStorage.getItem("token")
                ? "/dashboard"
                : "/login"
            }
            replace
          />
        }
      />

    </Routes>
  );
}

export default App;