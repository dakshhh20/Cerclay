import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { CartProvider } from "./context/CartContext";
import { AuthProvider } from "./context/AuthContext";
import { WishlistProvider } from "./context/WishlistContext";
import "./styles.css";

class AppErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, message: "" };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, message: error?.message || "Unexpected page error." };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 24, background: "#F8F3EA", color: "#302A27" }}>
          <div style={{ maxWidth: 520, textAlign: "center", padding: 32, border: "1px solid rgba(48,42,39,.14)", background: "#FBF8F2" }}>
            <p style={{ margin: "0 0 10px", fontSize: 11, letterSpacing: ".16em", textTransform: "uppercase" }}>Cerclay</p>
            <h1 style={{ margin: "0 0 12px", fontFamily: "Georgia, serif", fontWeight: 400 }}>This piece needs another look.</h1>
            <p style={{ margin: "0 0 22px", color: "rgba(48,42,39,.65)", lineHeight: 1.6 }}>The product page hit an unexpected error. Your catalogue and account data are still safe.</p>
            <button type="button" onClick={() => window.location.assign("/shop")} style={{ border: 0, background: "#302A27", color: "#F8F3EA", padding: "13px 20px", cursor: "pointer" }}>Back to collection</button>
            <details style={{ marginTop: 18, textAlign: "left", fontSize: 11, color: "rgba(48,42,39,.55)" }}>
              <summary>Technical detail</summary>
              <pre style={{ whiteSpace: "pre-wrap" }}>{this.state.message}</pre>
            </details>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AppErrorBoundary>
      <BrowserRouter>
        <CartProvider>
          <AuthProvider>
            <WishlistProvider>
              <App />
            </WishlistProvider>
          </AuthProvider>
        </CartProvider>
      </BrowserRouter>
    </AppErrorBoundary>
  </React.StrictMode>
);
