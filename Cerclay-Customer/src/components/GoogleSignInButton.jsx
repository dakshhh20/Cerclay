import React, { useEffect, useRef, useState } from "react";
import { useAuth } from "../context/AuthContext";

const GOOGLE_SCRIPT = "https://accounts.google.com/gsi/client";

function loadGoogleScript() {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve();
      return;
    }

    const existing = document.querySelector(`script[src="${GOOGLE_SCRIPT}"]`);
    if (existing) {
      existing.addEventListener("load", resolve, { once: true });
      existing.addEventListener("error", reject, { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = GOOGLE_SCRIPT;
    script.async = true;
    script.defer = true;
    script.onload = resolve;
    script.onerror = () => reject(new Error("Google Sign-In could not be loaded."));
    document.head.appendChild(script);
  });
}

export default function GoogleSignInButton() {
  const containerRef = useRef(null);
  const { loginWithGoogle } = useAuth();
  const [error, setError] = useState("");
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    let cancelled = false;

    if (!clientId || !containerRef.current) return undefined;

    loadGoogleScript()
      .then(() => {
        if (cancelled || !containerRef.current || !window.google?.accounts?.id) return;

        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: async (response) => {
            setError("");
            try {
              await loginWithGoogle(response.credential);
            } catch (err) {
              setError(err.message || "Google sign-in failed.");
            }
          },
        });

        containerRef.current.innerHTML = "";
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: "outline",
          size: "large",
          width: 360,
          text: "continue_with",
          shape: "rectangular",
        });
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Google sign-in is unavailable.");
      });

    return () => {
      cancelled = true;
    };
  }, [clientId, loginWithGoogle]);

  if (!clientId) {
    return (
      <div className="auth-provider-note">
        Google sign-in will appear after <code>VITE_GOOGLE_CLIENT_ID</code> is configured.
      </div>
    );
  }

  return (
    <div className="google-signin-wrap">
      <div ref={containerRef} className="google-signin-button" />
      {error && (
        <p className="form-error compact-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
