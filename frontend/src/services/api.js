import axios from "axios";

const API_BASE = import.meta.env.VITE_API_URL || "/api";

export const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  headers: { Accept: "application/json" }
});

let accessToken = null;
let refreshHandler = null;
let unauthorizedHandler = null;

export function setAccessToken(token) {
  accessToken = token || null;
}

export function setRefreshHandler(handler) {
  refreshHandler = handler;
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isAuthRequest = original?.url?.includes("/auth/");
    const canRetry =
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !isAuthRequest &&
      typeof refreshHandler === "function";

    if (!canRetry) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      const token = await refreshHandler();
      if (!token) {
        return Promise.reject(error);
      }
      original.headers = {
        ...original.headers,
        Authorization: `Bearer ${token}`
      };
      return api(original);
    } catch {
      if (typeof unauthorizedHandler === "function") {
        unauthorizedHandler();
      }
      return Promise.reject(error);
    }
  }
);

export function readCookie(name) {
  const prefix = `${name}=`;
  return document.cookie
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part.startsWith(prefix))
    .map((part) => decodeURIComponent(part.slice(prefix.length)))
    .at(0);
}

export async function getCsrfToken() {
  const cookieToken = readCookie("XSRF-TOKEN");
  if (cookieToken) {
    return cookieToken;
  }

  const { data } = await api.get("/auth/csrf");
  return data.token || readCookie("XSRF-TOKEN");
}

export async function csrfConfig(config = {}) {
  const token = await getCsrfToken();
  return {
    ...config,
    headers: {
      ...(config.headers || {}),
      "X-XSRF-TOKEN": token
    }
  };
}

export function apiErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.fieldErrors?._global ||
    error?.message ||
    "Something went wrong. Please try again."
  );
}

export function fieldErrors(error) {
  return error?.response?.data?.fieldErrors || {};
}
