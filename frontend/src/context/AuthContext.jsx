import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from "react";

import {
  api,
  csrfConfig,
  setAccessToken,
  setRefreshHandler,
  setUnauthorizedHandler
} from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");
  const accessTokenRef = useRef(null);
  const mountedRef = useRef(true);

  const applySession = useCallback((session) => {
    accessTokenRef.current = session.accessToken;
    setAccessToken(session.accessToken);
    setUser(session.user);
    setStatus("authenticated");
  }, []);

  const clearSession = useCallback(() => {
    accessTokenRef.current = null;
    setAccessToken(null);
    setUser(null);
    setStatus("guest");
  }, []);

  const refresh = useCallback(async () => {
    const config = await csrfConfig();
    const { data } = await api.post("/auth/refresh", null, config);
    applySession(data);
    return data.accessToken;
  }, [applySession]);

  useEffect(() => {
    mountedRef.current = true;
    setRefreshHandler(refresh);
    setUnauthorizedHandler(clearSession);

    return () => {
      mountedRef.current = false;
      setRefreshHandler(null);
      setUnauthorizedHandler(null);
    };
  }, [refresh, clearSession]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        await refresh();
      } catch {
        if (!cancelled) {
          clearSession();
        }
      }
    }

    bootstrap();

    return () => {
      cancelled = true;
    };
    // Bootstrap runs once; refresh/clearSession are stable callbacks.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(
    async (email, password) => {
      const { data } = await api.post("/auth/login", { email, password });
      applySession(data);
      return data.user;
    },
    [applySession]
  );

  const register = useCallback(
    async (payload) => {
      const { data } = await api.post("/auth/register", payload);
      applySession(data);
      return data.user;
    },
    [applySession]
  );

  const logout = useCallback(async () => {
    try {
      const config = await csrfConfig();
      await api.post("/auth/logout", null, config);
    } catch {
      // Clearing the local session is still correct if the server call fails.
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const updateUser = useCallback((nextUser) => {
    setUser(nextUser);
  }, []);

  const value = useMemo(
    () => ({
      user,
      status,
      isAuthenticated: status === "authenticated",
      isAdmin: user?.role === "ADMIN",
      accessToken: accessTokenRef.current,
      login,
      register,
      logout,
      refresh,
      updateUser
    }),
    [user, status, login, register, logout, refresh, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
