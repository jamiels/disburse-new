import { Navigate } from "react-router-dom";
import { isAuthenticated } from "../util/AuthUtil";

const ProtectedRoute = ({ children }) => {
  return isAuthenticated() ? children : <Navigate to="/" />;
};

export default ProtectedRoute;
