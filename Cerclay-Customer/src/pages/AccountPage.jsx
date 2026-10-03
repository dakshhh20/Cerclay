import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowUpRight, Heart, MapPin, Package, UserRound, RotateCcw, Star, ShieldCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";

function ProfileForm({ customer, onSave, onCancel, loading, error }) {
  const [form, setForm] = useState({
    name: customer?.name || "",
    email: customer?.email || "",
    phone: customer?.phone || "",
  });

  useEffect(() => {
    setForm({
      name: customer?.name || "",
      email: customer?.email || "",
      phone: customer?.phone || "",
    });
  }, [customer]);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    await onSave({
      name: form.name.trim(),
      email: form.email.trim().toLowerCase(),
      phone: form.phone.trim(),
    });
  }

  return (
    <form className="profile-form" onSubmit={submit}>
      {error && <div className="form-error" role="alert">{error}</div>}
      <label><span>Full name</span><input name="name" value={form.name} onChange={update} required maxLength={100} autoComplete="name" /></label>
      <label><span>Email</span><input type="email" name="email" value={form.email} onChange={update} required autoComplete="email" /></label>
      <label><span>Phone number</span><input name="phone" value={form.phone} onChange={update} required inputMode="numeric" pattern="[6-9][0-9]{9}" maxLength={10} autoComplete="tel" /></label>
      <p className="profile-edit-note">Changing your email or phone may require verification again.</p>
      <div className="profile-form-actions">
        <button type="button" className="secondary-button" onClick={onCancel} disabled={loading}>Cancel</button>
        <button type="submit" className="primary-button" disabled={loading}>{loading ? "Saving…" : "Save changes"}</button>
      </div>
    </form>
  );
}

export default function AccountPage() {
  const { customer, logout, updateCustomer } = useAuth();
  const navigate = useNavigate();
  const [editingProfile, setEditingProfile] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [profileMessage, setProfileMessage] = useState("");

  async function handleLogout() {
    await logout();
    navigate("/", { replace: true });
  }

  async function handleProfileSave(payload) {
    setSavingProfile(true);
    setProfileError("");
    setProfileMessage("");
    try {
      await updateCustomer(payload);
      setEditingProfile(false);
      setProfileMessage("Your profile has been updated.");
    } catch (err) {
      setProfileError(err.message || "Unable to update your profile.");
    } finally {
      setSavingProfile(false);
    }
  }

  return (
    <section className="account-page page-container">
      <div className="account-header">
        <div className="account-welcome-copy">
          <p className="eyebrow">MY ACCOUNT</p>
          <h1>Hello, {customer?.name || "there"}</h1>
          <p>Everything you need to manage your Cerclay experience, in one place.</p>
        </div>
        <button className="secondary-button account-signout" onClick={handleLogout}>Sign out</button>
      </div>

      <div className="account-layout">
        <aside className="account-sidebar" aria-label="Account navigation">
          <div className="account-sidebar-label">YOUR SPACE</div>
          <Link className="account-sidebar-link active" to="/account"><UserRound size={17} /> <span>Overview</span></Link>
          <Link className="account-sidebar-link" to="/account/addresses"><MapPin size={17} /> <span>Addresses</span></Link>
          <Link className="account-sidebar-link" to="/account/wishlist"><Heart size={17} /> <span>Wishlist</span></Link>
          <Link className="account-sidebar-link" to="/account/orders"><Package size={17} /> <span>Orders</span></Link>
          <Link className="account-sidebar-link" to="/account/returns"><RotateCcw size={17} /> <span>Returns & refunds</span></Link>
          <Link className="account-sidebar-link" to="/account/reviews"><Star size={17} /> <span>My reviews</span></Link>
          <div className="account-sidebar-note"><ShieldCheck size={16} /><span>Your account details stay private and secure.</span></div>
        </aside>

        <div className="account-content">
          <section className="account-panel profile-panel">
            <div className="panel-heading-row profile-heading-row">
              <div>
                <p className="eyebrow">PROFILE</p>
                <h2>Your details</h2>
                <p className="panel-subtitle">Keep your contact details current for orders and delivery.</p>
              </div>
              {!editingProfile && <button type="button" className="secondary-button" onClick={() => { setProfileError(""); setProfileMessage(""); setEditingProfile(true); }}>Edit profile</button>}
            </div>

            {profileMessage && <div className="form-success" role="status">{profileMessage}</div>}

            {editingProfile ? (
              <ProfileForm
                customer={customer}
                onSave={handleProfileSave}
                onCancel={() => { setEditingProfile(false); setProfileError(""); }}
                loading={savingProfile}
                error={profileError}
              />
            ) : (
              <dl className="account-details">
                <div><dt>Name</dt><dd>{customer?.name || "—"}</dd></div>
                <div><dt>Email</dt><dd>{customer?.email || "—"}</dd></div>
                <div><dt>Phone</dt><dd>{customer?.phone || "Not added"}</dd></div>
              </dl>
            )}
          </section>

          <div className="account-section-intro">
            <div>
              <p className="eyebrow">QUICK ACCESS</p>
              <h2>Make yourself at home.</h2>
            </div>
            <span>Jump straight to what you need.</span>
          </div>

          <div className="account-grid">
            <Link className="account-panel account-action-panel" to="/account/addresses">
              <div className="account-action-top"><span className="account-action-icon"><MapPin size={19} /></span><ArrowUpRight size={18} className="account-action-arrow" /></div>
              <p className="eyebrow">DELIVERY</p>
              <h2>Saved addresses</h2>
              <p>Manage home, work and other delivery addresses.</p>
              <span className="text-link">Manage addresses <span aria-hidden="true">→</span></span>
            </Link>

            <Link className="account-panel account-action-panel" to="/account/orders">
              <div className="account-action-top"><span className="account-action-icon"><Package size={19} /></span><ArrowUpRight size={18} className="account-action-arrow" /></div>
              <p className="eyebrow">ORDERS</p>
              <h2>Your purchases</h2>
              <p>View orders, payment information and delivery tracking.</p>
              <span className="text-link">View orders <span aria-hidden="true">→</span></span>
            </Link>

            <Link className="account-panel account-action-panel" to="/account/reviews">
              <div className="account-action-top"><span className="account-action-icon"><Star size={19} /></span><ArrowUpRight size={18} className="account-action-arrow" /></div>
              <p className="eyebrow">REVIEWS</p>
              <h2>My reviews</h2>
              <p>See your submitted ratings and whether each review is published.</p>
              <span className="text-link">View reviews <span aria-hidden="true">→</span></span>
            </Link>

            <Link className="account-panel account-action-panel" to="/account/wishlist">
              <div className="account-action-top"><span className="account-action-icon"><Heart size={19} /></span><ArrowUpRight size={18} className="account-action-arrow" /></div>
              <p className="eyebrow">WISHLIST</p>
              <h2>Saved pieces</h2>
              <p>Keep your favourite Cerclay pieces ready for your next purchase.</p>
              <span className="text-link">View wishlist <span aria-hidden="true">→</span></span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}
