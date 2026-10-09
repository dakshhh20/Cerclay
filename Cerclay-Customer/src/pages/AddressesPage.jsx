import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createAddress, deleteAddress, getCustomerAddresses, updateAddress } from "../lib/api";
import { useAuth } from "../context/AuthContext";

const emptyForm = {
  name: "",
  phone: "",
  house: "",
  street: "",
  city: "",
  state: "",
  pincode: "",
  addressType: "HOME",
  defaultAddress: false,
};

export default function AddressesPage() {
  const { customer, status } = useAuth();
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);

  const loadAddresses = useCallback(async () => {
    if (!customer?.id) return;
    setLoading(true);
    setError("");
    try {
      const data = await getCustomerAddresses(customer.id);
      setAddresses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load your addresses.");
    } finally {
      setLoading(false);
    }
  }, [customer?.id]);

  useEffect(() => {
    if (customer?.id) loadAddresses();
  }, [customer?.id, loadAddresses]);

  const editingAddress = useMemo(
    () => addresses.find((address) => address.id === editingId),
    [addresses, editingId]
  );

  if (status === "loading") {
    return (
      <section className="page-container state-page">
        <p>Loading your account…</p>
      </section>
    );
  }

  function startEdit(address) {
    setEditingId(address.id);
    setForm({
      name: address.name || "",
      phone: address.phone || "",
      house: address.house || "",
      street: address.street || "",
      city: address.city || "",
      state: address.state || "",
      pincode: address.pincode || "",
      addressType: address.addressType || "HOME",
      defaultAddress: Boolean(address.defaultAddress),
    });
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
    setError("");
  }

  function handleChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (editingId) {
        await updateAddress(editingId, form);
      } else {
        await createAddress(customer.id, form);
      }
      resetForm();
      await loadAddresses();
    } catch (err) {
      setError(err.message || "Unable to save this address.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Delete this saved address?")) return;
    setError("");
    try {
      await deleteAddress(id);
      if (editingId === id) resetForm();
      await loadAddresses();
    } catch (err) {
      setError(err.message || "Unable to delete this address.");
    }
  }

  return (
    <section className="addresses-page page-container">
      <div className="account-header">
        <div>
          <p className="eyebrow">SAVED ADDRESSES</p>
          <h1>{editingAddress ? "Edit address" : "Your addresses"}</h1>
          <p>Save delivery addresses once and use them during checkout.</p>
        </div>
        <Link className="secondary-button" to="/account">
          Back to account
        </Link>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="addresses-layout">
        <form className="address-form account-panel" onSubmit={handleSubmit}>
          <div className="panel-heading-row">
            <div>
              <p className="eyebrow">{editingId ? "EDIT" : "ADD NEW"}</p>
              <h2>{editingId ? "Update address" : "New delivery address"}</h2>
            </div>
            {editingId && (
              <button type="button" className="text-button" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>

          <div className="address-form-grid">
            <label>
              <span>Full name</span>
              <input
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                maxLength={100}
              />
            </label>
            <label>
              <span>Phone</span>
              <input
                name="phone"
                value={form.phone}
                onChange={handleChange}
                required
                inputMode="numeric"
                pattern="[0-9]{10}"
                maxLength={10}
              />
            </label>
            <label className="address-form-wide">
              <span>House / flat / building</span>
              <input
                name="house"
                value={form.house}
                onChange={handleChange}
                required
                maxLength={200}
              />
            </label>
            <label className="address-form-wide">
              <span>Street / locality</span>
              <input
                name="street"
                value={form.street}
                onChange={handleChange}
                required
                maxLength={200}
              />
            </label>
            <label>
              <span>City</span>
              <input
                name="city"
                value={form.city}
                onChange={handleChange}
                required
                maxLength={100}
              />
            </label>
            <label>
              <span>State</span>
              <input
                name="state"
                value={form.state}
                onChange={handleChange}
                required
                maxLength={100}
              />
            </label>
            <label>
              <span>PIN code</span>
              <input
                name="pincode"
                value={form.pincode}
                onChange={handleChange}
                required
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
              />
            </label>
            <label>
              <span>Address type</span>
              <select name="addressType" value={form.addressType} onChange={handleChange}>
                <option value="HOME">Home</option>
                <option value="WORK">Work</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
          </div>

          <label className="checkbox-row">
            <input
              type="checkbox"
              name="defaultAddress"
              checked={form.defaultAddress}
              onChange={handleChange}
            />
            <span>Make this my default delivery address</span>
          </label>

          <button className="primary-button address-submit" type="submit" disabled={saving}>
            {saving ? "Saving…" : editingId ? "Save changes" : "Save address"}
          </button>
        </form>

        <div className="saved-addresses">
          <div className="panel-heading-row">
            <div>
              <p className="eyebrow">YOUR LIST</p>
              <h2>Saved delivery addresses</h2>
            </div>
            <span className="address-count">{addresses.length}</span>
          </div>

          {loading ? (
            <div className="address-state">Loading addresses…</div>
          ) : addresses.length === 0 ? (
            <div className="address-state">
              <strong>No saved addresses yet.</strong>
              <p>Add your first address using the form.</p>
            </div>
          ) : (
            <div className="address-list">
              {addresses.map((address) => (
                <article className="saved-address-card" key={address.id}>
                  <div className="saved-address-top">
                    <div>
                      <span className="address-type">{address.addressType || "ADDRESS"}</span>
                      {address.defaultAddress && <span className="default-badge">Default</span>}
                    </div>
                    <div className="saved-address-actions">
                      <button className="text-button" onClick={() => startEdit(address)}>
                        Edit
                      </button>
                      <button
                        className="text-button danger-text"
                        onClick={() => handleDelete(address.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  <h3>{address.name}</h3>
                  <p>
                    {address.house}, {address.street}
                  </p>
                  <p>
                    {address.city}, {address.state} — {address.pincode}
                  </p>
                  <p>Phone: {address.phone}</p>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
