import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login.css";

function Register() {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("SHIPPER");

  const handleRegister = async (e) => {
    e.preventDefault();

    // ✅ basic validation
    if (!name || !email || !password) {
      alert("All fields are required ❌");
      return;
    }

    try {
      const res = await fetch("http://localhost:8084/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username:name,
          email,
          password,
          
        }),
      });

      
      const data = await res.json();
      console.log("FULL RESPONSE:", data);

      if (res.ok) {
  console.log("SUCCESS RESPONSE:", data);

  localStorage.setItem("token", data.token);

  alert("Registration Successful ✅");

  navigate("/login");
} else {
  console.error("ERROR RESPONSE:", data);
  alert(data.message || "Registration Failed ❌");
}
    } catch (error) {
      console.error("Error:", error);
      alert("Backend not working / Server error ❌");
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Create Account</h2>
        <p className="subtitle">Register to get started</p>

        <form onSubmit={handleRegister}>
          <input
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <input
            type="email"
            placeholder="Enter Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <input
            type="password"
            placeholder="Enter Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="SHIPPER">Shipper</option>
            <option value="CARRIER">Carrier</option>
          </select>

          <button type="submit">Register</button>
        </form>

        <p className="link">
          Already have an account?{" "}
          <span onClick={() => navigate("/login")}>Login</span>
        </p>
      </div>
    </div>
  );
}

export default Register;