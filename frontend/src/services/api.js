import axios from "axios";

const API_BASE_URL = "http://localhost:8180";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
    Accept: "*/*",
  },
});

// =========================
// JWT REQUEST INTERCEPTOR
// =========================

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// =========================
// JWT RESPONSE INTERCEPTOR
// =========================

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || "";

    /*
     * 401 can mean two different things in this backend:
     *
     * 1. JWT/session is invalid
     * 2. Banking operation failed authentication
     *    e.g. wrong PIN/password
     *
     * Only logout for endpoints where 401 actually means
     * the user's login session is invalid.
     */

    const isAuthEndpoint =
      url.includes("/api/users/login") ||
      url.includes("/api/users/register");

    const isPinOperation =
      url.includes("/api/account/deposit") ||
      url.includes("/api/account/withdraw") ||
      url.includes("/api/account/fund-transfer") ||
      url.includes("/api/account/pin/create") ||
      url.includes("/api/account/pin/update");

    if (
      status === 401 &&
      !isAuthEndpoint &&
      !isPinOperation
    ) {
      localStorage.removeItem("token");
      localStorage.removeItem("accountNumber");

      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

// =========================
// AUTH APIs
// =========================

export const loginUser = (identifier, password) =>
  api.post("/api/users/login", {
    identifier,
    password,
  });

export const registerUser = (userData) =>
  api.post("/api/users/register", userData);

// =========================
// DASHBOARD APIs
// =========================

export const getUserDetails = () =>
  api.get("/api/dashboard/user");

export const getAccountDetails = () =>
  api.get("/api/dashboard/account");

export const getTransactions = () =>
  api.get("/api/account/transactions");

export const sendBankStatement = () =>
  api.get("/api/account/send-statement");

// =========================
// PIN APIs
// =========================

export const createPin = ({
  accountNumber,
  pin,
  password,
}) =>
  api.post("/api/account/pin/create", {
    accountNumber,
    pin,
    password,
  });

export const updatePin = ({
  accountNumber,
  oldPin,
  newPin,
  password,
}) =>
  api.post("/api/account/pin/update", {
    accountNumber,
    oldPin,
    newPin,
    password,
  });

// =========================
// BANKING APIs
// =========================

export const depositCash = (amount, pin) =>
  api.post("/api/account/deposit", {
    amount,
    pin,
  });

export const withdrawCash = (amount, pin) =>
  api.post("/api/account/withdraw", {
    amount,
    pin,
  });

export const transferFunds = (
  sourceAccountNumber,
  targetAccountNumber,
  amount,
  pin
) =>
  api.post("/api/account/fund-transfer", {
    sourceAccountNumber,
    targetAccountNumber,
    amount,
    pin,
  });

// =========================
// EXPORT AXIOS INSTANCE
// =========================

export default api;