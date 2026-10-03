import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import GoogleSignInButton from "../components/GoogleSignInButton";
import { useAuth } from "../context/AuthContext";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 30;

function validEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function AuthDivider() {
  return <div className="auth-divider"><span>or</span></div>;
}

export default function AuthPage({ mode = "login" }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, loginWithMobileOtp, register, sendMobileOtp, sendPasswordResetOtp, resetPassword } = useAuth();

  const initialMode = mode === "register" ? "register" : mode === "forgot" ? "forgot" : "login";
  const [view, setView] = useState(initialMode);
  const [loginMethod, setLoginMethod] = useState("password");
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "", otp: "", newPassword: "", confirmPassword: "" });
  const [otpSent, setOtpSent] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [phoneSignupDetailsRequired, setPhoneSignupDetailsRequired] = useState(false);

  const nextPath = useMemo(() => searchParams.get("next") || "/account", [searchParams]);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  function switchView(nextView) {
    setView(nextView);
    setError("");
    setMessage("");
    setOtpSent(false);
    setPhoneSignupDetailsRequired(false);
    setForm((current) => ({ ...current, otp: "", newPassword: "", confirmPassword: "" }));
  }

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function sendMobileOtpForLogin() {
    setError("");
    setMessage("");
    const phone = form.phone.trim();
    if (!/^\d{10}$/.test(phone)) {
      setError("Enter a valid 10-digit mobile number.");
      return;
    }
    setLoading(true);
    try {
      await sendMobileOtp(phone);
      setOtpSent(true);
      setPhoneSignupDetailsRequired(false);
      setCooldown(RESEND_SECONDS);
      setMessage("A verification code has been sent to your mobile number.");
    } catch (err) {
      setError(err.message || "Unable to send the mobile verification code.");
    } finally {
      setLoading(false);
    }
  }

  async function sendResetOtp() {
    setError("");
    setMessage("");
    const phone = form.phone.trim();
    if (!/^\d{10}$/.test(phone)) {
      setError("Enter the 10-digit mobile number linked to your Cerclay account.");
      return;
    }
    setLoading(true);
    try {
      await sendPasswordResetOtp(phone);
      setOtpSent(true);
      setCooldown(RESEND_SECONDS);
      setMessage("If the account is eligible, a password-reset code has been sent to your mobile number.");
    } catch (err) {
      setError(err.message || "Unable to send the reset code.");
    } finally {
      setLoading(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    setLoading(true);

    try {
      if (view === "register") {
        await register({
          name: form.name.trim(),
          email: form.email.trim().toLowerCase(),
          phone: form.phone.trim(),
          password: form.password,
        });
        setMessage("Account created. Please sign in to continue.");
        setView("login");
        return;
      }

      if (view === "forgot") {
        if (form.newPassword !== form.confirmPassword) {
          throw new Error("The new passwords do not match.");
        }
        await resetPassword(form.phone.trim(), form.otp.trim(), form.newPassword);
        setMessage("Your password has been reset. You can now sign in.");
        setView("login");
        setOtpSent(false);
        setForm((current) => ({ ...current, password: "", otp: "", newPassword: "", confirmPassword: "" }));
        return;
      }

      if (loginMethod === "mobileOtp") {
        const customer = await loginWithMobileOtp(
          form.phone.trim(),
          form.otp.trim(),
          form.name.trim(),
          form.email.trim()
        );
        if (customer) navigate(nextPath, { replace: true });
        return;
      }

      await login(form.email.trim().toLowerCase(), form.password);
      navigate(nextPath, { replace: true });
    } catch (err) {
      if (loginMethod === "mobileOtp" && err.code === "PHONE_ACCOUNT_DETAILS_REQUIRED") {
        setPhoneSignupDetailsRequired(true);
        setError("This mobile number is not registered yet. Enter your name and email to create your Cerclay account.");
      } else {
        setError(err.message || "Unable to continue. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  const isLogin = view === "login";
  const isRegister = view === "register";
  const isForgot = view === "forgot";

  return (
    <section className="auth-page page-container">
      <div className="auth-card auth-card-wide">
        <p className="eyebrow">CERCLAY ACCOUNT</p>
        <h1>{isRegister ? "Create your account" : isForgot ? "Reset your password" : "Welcome to Cerclay"}</h1>
        <p className="auth-intro">
          {isRegister
            ? "Create an account to keep your details, addresses and orders together."
            : isForgot
              ? "Verify your mobile number and choose a new password."
              : "Sign in to manage your account, orders and saved addresses."}
        </p>

        {error && <div className="form-error" role="alert">{error}</div>}
        {message && <div className="form-success" role="status">{message}</div>}

        {isLogin && (
          <div className="auth-method-tabs" role="tablist" aria-label="Sign-in method">
            <button type="button" className={loginMethod === "password" ? "active" : ""} onClick={() => { setLoginMethod("password"); setError(""); }}>Password</button>
            <button type="button" className={loginMethod === "mobileOtp" ? "active" : ""} onClick={() => { setLoginMethod("mobileOtp"); setError(""); setOtpSent(false); }}>Mobile OTP</button>
          </div>
        )}

        {!isForgot && (
          <form onSubmit={submit} className="auth-form">
            {isRegister && <label><span>Full name</span><input name="name" value={form.name} onChange={update} required autoComplete="name" maxLength={100} /></label>}
            {isRegister && <label><span>Phone number</span><input name="phone" value={form.phone} onChange={update} required inputMode="numeric" pattern="[0-9]{10}" maxLength={10} autoComplete="tel" /></label>}
            {(isRegister || (isLogin && loginMethod === "password")) && (
              <label><span>Email</span><input type="email" name="email" value={form.email} onChange={update} required autoComplete="email" /></label>
            )}

            {isRegister && <label><span>Password</span><input type="password" name="password" value={form.password} onChange={update} required minLength={8} autoComplete="new-password" /></label>}

            {isLogin && loginMethod === "mobileOtp" && (
              <>
                <label><span>Mobile number</span><div className="phone-field"><span>+91</span><input name="phone" value={form.phone} onChange={update} inputMode="numeric" pattern="[0-9]{10}" maxLength={10} placeholder="10-digit mobile number" required autoComplete="tel" /></div></label>
                {phoneSignupDetailsRequired && (
                  <>
                    <div className="form-success" role="status">This mobile number is not registered yet. Complete these details to create your account.</div>
                    <label><span>Full name</span><input name="name" value={form.name} onChange={update} required autoComplete="name" maxLength={100} /></label>
                    <label><span>Email</span><input type="email" name="email" value={form.email} onChange={update} required autoComplete="email" /></label>
                  </>
                )}
                <div className="otp-action-row">
                  <label className="grow"><span>Verification code</span><input name="otp" value={form.otp} onChange={update} inputMode="numeric" pattern="[0-9]{6}" maxLength={OTP_LENGTH} placeholder="6-digit code" required={otpSent} autoComplete="one-time-code" /></label>
                  <button type="button" className="secondary-button otp-send-button" onClick={sendMobileOtpForLogin} disabled={loading || cooldown > 0}>{cooldown > 0 ? `Resend ${cooldown}s` : otpSent ? "Resend code" : "Send code"}</button>
                </div>
                {otpSent && <p className="field-help">Enter the code sent to <strong>+91 {form.phone}</strong>.</p>}
              </>
            )}

            {isLogin && loginMethod === "password" && (
              <>
                <label><span>Password</span><input type="password" name="password" value={form.password} onChange={update} required autoComplete="current-password" /></label>
                <div className="auth-inline-row"><Link className="link-button" to={`/forgot-password?next=${encodeURIComponent(nextPath)}`}>Forgot password?</Link></div>
              </>
            )}



            <button className="primary-button auth-submit" disabled={loading} type="submit">
              {loading ? "Please wait…" : isRegister ? "Create account" : loginMethod === "mobileOtp" ? (phoneSignupDetailsRequired ? "Create account & sign in" : "Verify & sign in") : "Sign in"}
            </button>
          </form>
        )}

        {isForgot && (
          <form onSubmit={submit} className="auth-form">
            <label><span>Mobile number</span><div className="phone-field"><span>+91</span><input name="phone" value={form.phone} onChange={update} inputMode="numeric" pattern="[0-9]{10}" maxLength={10} placeholder="10-digit mobile number" required autoComplete="tel" /></div></label>
            <div className="otp-action-row">
              <label className="grow"><span>Reset code</span><input name="otp" value={form.otp} onChange={update} inputMode="numeric" pattern="[0-9]{6}" maxLength={OTP_LENGTH} placeholder="6-digit code" required={otpSent} autoComplete="one-time-code" /></label>
              <button type="button" className="secondary-button otp-send-button" onClick={sendResetOtp} disabled={loading || cooldown > 0}>{cooldown > 0 ? `Resend ${cooldown}s` : otpSent ? "Resend code" : "Send code"}</button>
            </div>
            <label><span>New password</span><input type="password" name="newPassword" value={form.newPassword} onChange={update} required={otpSent} minLength={8} autoComplete="new-password" /></label>
            <label><span>Confirm new password</span><input type="password" name="confirmPassword" value={form.confirmPassword} onChange={update} required={otpSent} minLength={8} autoComplete="new-password" /></label>
            <button className="primary-button auth-submit" disabled={loading || !otpSent} type="submit">{loading ? "Please wait…" : "Reset password"}</button>
          </form>
        )}

        {isLogin && (
          <>
            <AuthDivider />
            <GoogleSignInButton />
          </>
        )}

        <div className="auth-footer-links">
          {isLogin && <p>New to Cerclay? <button type="button" className="link-button" onClick={() => switchView("register")}>Create an account</button></p>}
          {isRegister && <p>Already have an account? <button type="button" className="link-button" onClick={() => switchView("login")}>Sign in</button></p>}
          {isForgot && <p>Remember your password? <button type="button" className="link-button" onClick={() => switchView("login")}>Back to sign in</button></p>}
        </div>
      </div>
    </section>
  );
}
