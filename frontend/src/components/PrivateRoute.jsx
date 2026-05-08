import { Navigate } from "react-router-dom";
import { isLoggedIn } from "../services/api";

const PrivateRoute = ({ children }) => {
  return isLoggedIn() ? children : <Navigate to="/login" />;
};

export default PrivateRoute;
