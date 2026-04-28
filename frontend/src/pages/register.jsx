import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser, saveAuth } from "../services/api";
import "./login.css";

function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("SHIPPER");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await registerUser(name, email, password, role);
      saveAuth(data);
      navigate("/dashboard");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Create Account</h2>
        <p className="subtitle">Register to get started</p>

        {error && <p className="error-msg">{error}</p>}

        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <input
            type="email"
            placeholder="Enter Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Enter Password (min 6 chars)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={6}
            required
          />

          <div className="role-selector">
            <label>I am a:</label>
            <div className="role-options">
              <button
                type="button"
                className={`role-btn ${role === "SHIPPER" ? "active" : ""}`}
                onClick={() => setRole("SHIPPER")}
              >
                📦 Shipper
              </button>
              <button
                type="button"
                className={`role-btn ${role === "CARRIER" ? "active" : ""}`}
                onClick={() => setRole("CARRIER")}
              >
                🚚 Carrier
              </button>
            </div>
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "Creating account..." : "Register"}
          </button>
        </form>

        <p className="link">
          Already have an account? <a href="/login">Login</a>
        </p>
      </div>
    </div>
  );
}

export default Register;