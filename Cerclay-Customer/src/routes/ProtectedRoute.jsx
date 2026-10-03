import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children }) {
  const { customer, status } = useAuth();
  const location = useLocation();

  if (status === "loading") {
    return (
      <section className="page-container state-page">
        <div className="state-box">
          <strong>Checking your account…</strong>
          <span>Please wait a moment.</span>
        </div>
      </section>
    );
  }

  if (!customer) {
    const next = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />;
  }

  return children;
}
