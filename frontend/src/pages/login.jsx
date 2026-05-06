import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { loginUser, isLoggedIn } from "../services/api";
import "./login.css";

const Login = () => {

  const navigate = useNavigate();

  // ✅ STATES
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  // ✅ Already logged in
  useEffect(() => {

    if (isLoggedIn()) {
      navigate("/dashboard");
    }

  }, [navigate]);

  // ✅ LOGIN
  const handleLogin = async (e) => {

    e.preventDefault();

    setLoading(true);

    try {

      // 🔥 API CALL
      const data = await loginUser(email, password);

      console.log("🔥 LOGIN RESPONSE:", data);

      // ✅ SAVE TOKEN
      localStorage.setItem("token", data.token || "");

      // ✅ SAVE USER DATA
      localStorage.setItem("email", data.email || "");
      localStorage.setItem("role", data.role || "");

      // ✅ IMPORTANT
      localStorage.setItem("userId", data.userId || "");

      console.log("✅ SAVED USER ID:", data.userId);

      alert("Login Successful ✅");

      // ✅ Redirect
      navigate("/dashboard");

    } catch (err) {

      console.error("❌ LOGIN ERROR:", err);

      alert(err.message || "Login failed ❌");

    } finally {

      setLoading(false);

    }
  };

  return (
    <div className="auth-container">

      <form className="auth-card" onSubmit={handleLogin}>

        <h2>Welcome Back</h2>

        <p>Login to your account</p>

        {/* EMAIL */}
        <input
          type="email"
          placeholder="Enter Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        {/* PASSWORD */}
        <input
          type="password"
          placeholder="Enter Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {/* BUTTON */}
        <button type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>

        {/* REGISTER */}
        <p>
          Don't have an account?{" "}
          <Link to="/register">Register</Link>
        </p>

      </form>

    </div>
  );
};

export default Login;