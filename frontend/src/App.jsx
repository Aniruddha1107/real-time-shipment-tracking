import { Routes, Route } from "react-router-dom";
<<<<<<< HEAD
import Login from "./pages/login.jsx";
import Register from "./pages/register.jsx";
import Dashboard from "./pages/dashboard.jsx";
import PrivateRoute from "./components/PrivateRoute.jsx";
=======
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/dashboard";
>>>>>>> 15c8f9c25d719bc77bba075c0f892b8907f391b8

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
<<<<<<< HEAD

      {/* ✅ Protected Route */}
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        }
      />
=======
      <Route path="/dashboard" element={<Dashboard />} />
>>>>>>> 15c8f9c25d719bc77bba075c0f892b8907f391b8
    </Routes>
  );
}

export default App;